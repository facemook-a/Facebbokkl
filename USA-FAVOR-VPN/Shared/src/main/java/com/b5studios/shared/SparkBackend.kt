package com.b5studios.shared

import android.content.Context
import android.os.Build
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date
import javax.net.ssl.HttpsURLConnection

/** Spark licensing: server-enforced Firestore transactions, no Cloud Functions or billing binding. */
class SparkBackend(private val context:Context,private val auth:FirebaseAuth) {
    private val db get()=FirebaseFirestore.getInstance()
    private val uid get()=auth.currentUser?.uid?:error("Sign in first")
    private val random=SecureRandom()
    private val alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val deviceModel get()="${Build.MANUFACTURER} ${Build.MODEL}".take(120)
    private fun time(v:Any?):Long?=when(v){is Timestamp->v.toDate().time;is Number->v.toLong();else->null}
    private fun expires(l:Map<String,Any?>):Long? = time(l["expires_at"]) ?: time(l[if(l["activation_policy"]=="generation")"created_at" else "activated_at"])?.let{it+((l["duration_days"] as? Number)?.toLong()?:0)*86400000L}
    private fun usable(l:Map<String,Any?>)=l["is_active"]==true&&l["status"]!="revoked"&&(expires(l)?.let{it>System.currentTimeMillis()}?:true)
    private fun status(l:Map<String,Any?>)=if(l["is_active"]!=true||l["status"]=="revoked")"revoked" else if(expires(l)?.let{it<=System.currentTimeMillis()}==true)"expired" else if(l["activated_at"]!=null)"active" else "unused"
    private fun normalized(v:Any?):Any?=when(v){is Timestamp->v.toDate().time;is Map<*,*>->v.entries.associate{it.key.toString() to normalized(it.value)};is List<*>->v.map{normalized(it)};else->v}
    @Suppress("UNCHECKED_CAST") private fun row(s:DocumentSnapshot):Map<String,Any?> {val l=s.data?:emptyMap();return (normalized(l) as Map<String,Any?>)+mapOf("id" to s.id,"expires_at" to expires(l),"status" to status(l))}
    private fun hash(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){"%02x".format(it.toInt() and 255)}
    private fun identifier(v:Any?):String {val s=v?.toString().orEmpty();require(s.matches(Regex("[A-Za-z0-9_-]{1,128}"))){"Invalid identifier"};return s}
    private fun days(v:Any?):Int {val n=(v as? Number)?.toInt()?:error("Choose a duration");require(n in 1..3650){"Duration must be 1–3650 days"};return n}
    private suspend fun read(path:String)=db.document(path).get(Source.SERVER).await()
    private suspend fun bound():DocumentSnapshot {
        val b=read("bindings/$uid");check(b.exists()){ "Activate your license first." }
        val l=read("licenses/${b.getString("license_id")}");check(l.exists()&&l.getString("device_binding_id")==uid){"Device binding was reset. Contact the administrator."}
        check(usable(l.data!!)){if(l.getBoolean("is_active")!=true)"This license has been banned by the administrator." else "Your subscription has expired."};return l
    }
    private suspend fun info():Map<String,Any?> {val s=bound();val l=row(s);return mapOf("masked" to "USA-••••-${s.getString("code").orEmpty().takeLast(4)}","activated_at" to l["activated_at"],"expires_at" to l["expires_at"],"status" to l["status"],"license_id" to s.id)}
    private suspend fun audit(action:String,id:String){db.collection("audit").add(mapOf("actor" to uid,"action" to action,"target" to id,"at" to FieldValue.serverTimestamp())).await()}
    private suspend fun touch(s:DocumentSnapshot){
        val ref=db.document("device_sessions/$uid");val old=ref.get(Source.SERVER).await()
        val data=mapOf("uid" to uid,"license_id" to s.id,"device_model" to deviceModel,"last_seen" to FieldValue.serverTimestamp(),"created_at" to (old.getTimestamp("created_at")?:FieldValue.serverTimestamp()))
        ref.set(data).await()
    }
    suspend fun call(name:String,d:Map<String,Any?>):Map<String,Any?> = when(name){
        "generateLicense"->generate(d)
        "activateLicense"->activate(d["code"].toString())
        "licenseInfo"->info().also { touch(bound()) }
        "listLicenses"->listLicenses(d)
        "manageLicense"->manageLicense(d)
        "dashboard"->dashboard()
        "listDeviceConflicts"->mapOf("items" to db.collection("device_conflicts").orderBy("attempted_at",Query.Direction.DESCENDING).limit(100).get(Source.SERVER).await().documents.map{normalized(it.data).obj()+mapOf("id" to it.id)})
        "listSessions"->listDevices(identifier(d["license_id"]))
        "adminConfiguration"->mapOf("settings" to read("configuration/client").data.orEmpty(),"social" to (read("configuration/social").get("items")?:emptyList<Any>()),"sections" to (read("configuration/sections").get("items")?:emptyList<Any>()),"layout" to read("configuration/admin_layout").data.orEmpty(),"profiles" to db.collection("profile_documents").orderBy("created_at",Query.Direction.DESCENDING).limit(100).get(Source.SERVER).await().documents.map{normalized(it.data).obj()+mapOf("id" to it.id)})
        "saveProfileDocument"->{val text=d["text"].toString();val summary=importOvpn(text);val name=d["name"].toString().take(100);require(name.lowercase().endsWith(".ovpn")){"Choose an .ovpn file"};val ref=db.collection("profile_documents").document();ref.set(mapOf("name" to name,"public_profile" to text,"summary" to summary,"sha256" to hash(text),"created_at" to FieldValue.serverTimestamp(),"created_by" to uid)).await();audit("profile.import",ref.id);mapOf("id" to ref.id,"summary" to summary)}
        "deleteProfileDocument"->{val id=identifier(d["id"]);db.document("profile_documents/$id").delete().await();audit("profile.delete",id);mapOf("ok" to true)}
        "createServerFromProfile"->{val id=identifier(d["id"]);val p=read("profile_documents/$id");check(p.exists());val summary=p.get("summary").obj();saveServer(mapOf("server" to mapOf("name" to p.getString("name").orEmpty().removeSuffix(".ovpn"),"country" to "Not verified","city" to "Not verified","flag" to "🌐","host" to summary["host"],"port" to summary["port"],"transport" to summary["transport"],"dns" to listOf("1.1.1.1","9.9.9.9"),"max_sessions" to 100,"priority" to 10,"role" to "standard","enabled" to false,"maintenance" to false,"maintenance_message" to "","ca_reference" to id,"tls_profile_reference" to "tls-crypt-v2","ipv6_policy" to "block","description" to "Imported public profile; gateway ownership and location need verification","private_notes" to "")))}
        "saveSections"->{val items=d["items"].rows();validateSections(items);db.document("configuration/sections").set(mapOf("items" to items)).await();audit("sections.save","sections");mapOf("ok" to true)}
        "saveAdminLayout"->{val panels=d["enabled_panels"] as? List<*>?:emptyList<Any>();require(panels.size<=8&&panels.all{it in listOf("overview","license_health","security_alerts","infrastructure","quick_actions")});val cards=d["custom_cards"].rows();validateSections(cards.map{it+mapOf("target" to "home")});db.document("configuration/admin_layout").set(mapOf("enabled_panels" to panels,"custom_cards" to cards)).await();mapOf("ok" to true)}
        "saveConfiguration"->{val s=d["settings"].obj();validateSettings(s);db.document("configuration/client").set(s).await();audit("configuration.save","client");mapOf("ok" to true)}
        "saveSocial"->{val items=d["items"].rows();require(items.size<=30);items.forEach{require(safeUrl(it.str("url"))&&it.str("name").length in 1..80&&it["enabled"] is Boolean){"Invalid link"}};db.document("configuration/social").set(mapOf("items" to items.mapIndexed{i,x->x+mapOf("order" to i)})).await();audit("social.save","social");mapOf("ok" to true)}
        "getConfiguration"->{bound();mapOf("settings" to read("configuration/client").data.orEmpty(),"social" to (read("configuration/social").get("items")?:emptyList<Any>()),"sections" to (read("configuration/sections").get("items")?:emptyList<Any>()),"servers" to db.collection("public_servers").get(Source.SERVER).await().documents.map{normalized(it.data).obj()+mapOf("id" to it.id)})}
        "saveServer"->saveServer(d)
        "manageServer"->manageServer(d)
        "importOvpn"->importOvpn(d["text"].toString())
        "startSession"->{bound();val id=identifier(d["server_id"]);val server=read("public_servers/$id");check(server.exists()){ "No verified gateway has been published" };remote(server.data!!,"/client/session",mapOf("version" to d["version"]))}
        "endSession"->{val id=identifier(d["session_id"]);val session=read("sessions/$id");check(session.exists()&&session.getString("uid")==uid);val server=read("public_servers/${session.getString("server_id")}");if(server.exists())remote(server.data!!,"/client/end",mapOf("session_id" to id))else mapOf("ok" to true)}
        else->error("Unsupported operation")
    }
    private suspend fun generate(d:Map<String,Any?>):Map<String,Any?> {
        val duration=days(d["duration_days"]);val policy=d["activation_policy"];require(policy in listOf("first_activation","generation"))
        repeat(5){
            val code="USA-"+List(8){alphabet[random.nextInt(alphabet.length)]}.joinToString("").chunked(4).joinToString("-");val id=hash(code);val ref=db.document("licenses/$id")
            val made=db.runTransaction{t->if(t.get(ref).exists())false else {t.set(ref,mapOf("schema_version" to 3,"code" to code,"is_active" to true,"status" to "unused","created_at" to FieldValue.serverTimestamp(),"created_by" to uid,"activated_at" to null,"expires_at" to null,"duration_days" to duration,"activation_policy" to policy,"device_binding_id" to null,"revoked_at" to null,"updated_at" to FieldValue.serverTimestamp()));true}}.await()
            if(made){audit("license.create",id);return mapOf("code" to code,"id" to id)}
        };error("Code collision. Try again.")
    }
    private suspend fun activate(raw:String):Map<String,Any?> {
        val code=raw.trim().uppercase();require(code.matches(Regex("USA-[A-Z2-9]{4}-[A-Z2-9]{4}"))){"Enter USA-XXXX-XXXX"}
        val id=hash(code);val ref=db.document("licenses/$id");val binding=db.document("bindings/$uid")
        try {
            db.runTransaction{t->
                val l=t.get(ref);val b=t.get(binding);check(l.exists()){ "License code not found." };check(l.getLong("schema_version")==3L){"Ask the administrator for a new code"};check(usable(l.data!!)){"This code has been revoked or has expired."}
                val owner=l.getString("device_binding_id");check(owner==null||owner==uid){"Code already activated on another installation"}
                if(b.exists()&&b.getString("license_id")!=id){val old=t.get(db.document("licenses/${b.getString("license_id")}"));check(!old.exists()||!usable(old.data!!)){"This installation already has an active license"}}
                if(owner==null)t.update(ref,mapOf("device_binding_id" to uid,"activated_at" to (l.getTimestamp("activated_at")?:FieldValue.serverTimestamp()),"status" to "active","updated_at" to FieldValue.serverTimestamp()))
                t.set(binding,mapOf("license_id" to id,"updated_at" to FieldValue.serverTimestamp()))
                null
            }.await()
        }catch(e:Exception){
            if(e is FirebaseFirestoreException&&e.code==FirebaseFirestoreException.Code.PERMISSION_DENIED){
                val logged=runCatching{val attempt=db.document("device_conflicts/${uid}_$id");db.runTransaction{t->if(!t.get(attempt).exists())t.set(attempt,mapOf("license_id" to id,"uid" to uid,"device_model" to deviceModel,"kind" to "other_installation","attempted_at" to FieldValue.serverTimestamp()));null}.await()}.isSuccess
                if(logged)error("This code is already activated on another installation. The administrator has been notified.")
                error("Activation could not be authorized. Ask the administrator to complete the free Firestore setup.")
            };throw e
        }
        val l=bound();touch(l);return info()
    }
    private suspend fun listLicenses(d:Map<String,Any?>):Map<String,Any?> {
        val search=d["search"]?.toString()?.trim()?.uppercase()
        if(!search.isNullOrBlank()){require(search.matches(Regex("USA-[A-Z2-9]{4}-[A-Z2-9]{4}")));val s=read("licenses/${hash(search)}");return mapOf("items" to if(s.exists())listOf(row(s))else emptyList<Any>(),"next_cursor" to null)}
        var q:Query=db.collection("licenses").orderBy("created_at",Query.Direction.DESCENDING).limit(200)
        d["cursor"]?.toString()?.takeIf{it.isNotBlank()}?.let{q=q.startAfter(read("licenses/${identifier(it)}"))}
        val docs=q.get(Source.SERVER).await().documents;return mapOf("items" to docs.map(::row),"next_cursor" to if(docs.size==200)docs.last().id else null)
    }
    private suspend fun manageLicense(d:Map<String,Any?>):Map<String,Any?> {
        val id=identifier(d["id"]);val action=d["action"].toString();val ref=db.document("licenses/$id")
        db.runTransaction{t->
            val snap=t.get(ref);check(snap.exists());val l=snap.data!!;val owner=l["device_binding_id"] as? String;val b=owner?.let{db.document("bindings/$it")};val binding=b?.let{t.get(it)}
            val patch=mutableMapOf<String,Any?>("updated_at" to FieldValue.serverTimestamp())
            when(action){
                "delete"->{t.delete(ref);if(binding?.getString("license_id")==id)t.delete(b!!)}
                "revoke"->{patch+=mapOf("is_active" to false,"status" to "revoked","revoked_at" to FieldValue.serverTimestamp());t.update(ref,patch)}
                "reactivate"->{check(expires(l)?.let{it>System.currentTimeMillis()}?:true){"Extend the expired subscription first"};patch+=mapOf("is_active" to true,"status" to if(l["activated_at"]==null)"unused" else "active","revoked_at" to null);t.update(ref,patch)}
                "extend"->{patch["expires_at"]=Timestamp(Date(maxOf(System.currentTimeMillis(),expires(l)?:System.currentTimeMillis())+days(d["days"])*86400000L));t.update(ref,patch)}
                "reset"->{patch+=mapOf("device_binding_id" to null,"status" to if(l["is_active"]==true)"unused" else "revoked");t.update(ref,patch);if(binding?.getString("license_id")==id)t.delete(b!!)}
                else->error("Unsupported license action")
            };null
        }.await();audit("license.$action",id);return mapOf("ok" to true)
    }
    private suspend fun dashboard():Map<String,Any?> {
        val licenses=db.collection("licenses").get(Source.SERVER).await().documents;val servers=db.collection("servers").get(Source.SERVER).await().documents
        val counts=mutableMapOf<String,Any?>("total" to licenses.size,"recent" to licenses.count{time(it["activated_at"])?.let{v->v>System.currentTimeMillis()-7*86400000L}==true})
        for(s in listOf("active","expired","revoked","unused"))counts[s]=licenses.count{status(it.data!!)==s}
        val fresh=servers.filter{it["health"].obj()["online"]==true&&time(it["health"].obj()["checked_at"])?.let{v->v>System.currentTimeMillis()-120000}==true}
        counts+=mapOf("servers" to servers.size,"online" to fresh.size,"offline" to servers.size-fresh.size,"active_sessions" to fresh.sumOf{(it["health"].obj()["active_sessions"] as? Number)?.toLong()?:0},"sessions" to db.collection("sessions").whereEqualTo("revoked",false).get(Source.SERVER).await().documents.count{time(it["expires_at"])?.let{v->v>System.currentTimeMillis()}==true},"server_items" to servers.map{normalized(it.data).obj()+mapOf("id" to it.id)})
        return counts
    }
    private suspend fun listDevices(id:String):Map<String,Any?> {
        val devices=db.collection("device_sessions").whereEqualTo("license_id",id).limit(100).get(Source.SERVER).await().documents.map{normalized(it.data).obj()+mapOf("kind" to "bound_device","id" to it.id)}
        val conflicts=db.collection("device_conflicts").whereEqualTo("license_id",id).limit(100).get(Source.SERVER).await().documents.map{normalized(it.data).obj()+mapOf("kind" to "other_installation","id" to it.id)}
        return mapOf("items" to devices+conflicts)
    }
    fun watchLicense(onInvalid:(String)->Unit):ListenerRegistration {
        var licenseListener:ListenerRegistration?=null
        val bindingListener=db.document("bindings/$uid").addSnapshotListener{b,error->
            if(error!=null){onInvalid("Unable to verify your license. Check your internet connection.");return@addSnapshotListener}
            if(b==null||b.metadata.isFromCache)return@addSnapshotListener
            licenseListener?.remove()
            if(!b.exists()){onInvalid("Your device binding was reset. Contact support.");return@addSnapshotListener}
            licenseListener=db.document("licenses/${b.getString("license_id")}").addSnapshotListener{l,e->
                if(e!=null){onInvalid("Unable to verify your license.");return@addSnapshotListener}
                if(l==null||l.metadata.isFromCache)return@addSnapshotListener
                if(!l.exists()||l.getString("device_binding_id")!=uid||!usable(l.data!!))onInvalid("Your license was revoked or has expired.")
            }
        }
        return ListenerRegistration{bindingListener.remove();licenseListener?.remove()}
    }
    private fun validateSections(items:List<Map<String,Any?>>){
        require(items.size<=20){"Maximum 20 sections"}
        val ids=items.map{it.str("id")};require(ids.distinct().size==ids.size)
        items.forEach { x->require(x.str("id").matches(Regex("[A-Za-z0-9_-]{1,80}"))&&x.str("title").length in 1..80&&x.str("body").length<=3000&&x["enabled"] is Boolean&&x["target"] in listOf("home","settings")){"Invalid section"};if(x.str("url").isNotBlank())require(safeUrl(x.str("url"))){"HTTPS or support email required"} }
    }
    private fun validateSettings(s:Map<String,Any?>){
        val keys=setOf("welcome","announcement","maintenance_message","default_server","server_order","support","privacy_url","terms_url","help","activation_popup","minimum_version","update_notice","maintenance","show_social","show_tips")
        require(s.keys.all{it in keys})
        s.forEach{(k,v)->when(k){"maintenance","show_social","show_tips"->require(v is Boolean);"minimum_version"->require(v is Number&&v.toInt()>=1);else->require(v is String&&v.length<=4000)}}
        for(k in listOf("privacy_url","terms_url"))s[k]?.toString()?.takeIf{it.isNotBlank()}?.let{require(safeUrl(it))}
    }
    private fun publicHost(h:String):Boolean {if(!h.matches(Regex("[A-Za-z0-9.-]{1,253}"))||!h.contains('.')||h.endsWith('.')||h.contains(".."))return false;return !h.lowercase().matches(Regex("(localhost|.*\\.(local|localhost|internal|invalid)|0\\..*|10\\..*|127\\..*|169\\.254\\..*|192\\.168\\..*|172\\.(1[6-9]|2[0-9]|3[01])\\..*)"))}
    private fun safeUrl(raw:String):Boolean=runCatching{val u=URI(raw);if(u.scheme=="mailto")u.rawQuery==null&&u.schemeSpecificPart.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))else u.scheme=="https"&&u.userInfo==null&&u.port==-1&&publicHost(u.host.orEmpty())}.getOrDefault(false)
    private suspend fun saveServer(d:Map<String,Any?>):Map<String,Any?> {
        val s=d["server"].obj();val id=d["id"]?.let(::identifier)?:db.collection("servers").document().id
        require(publicHost(s.str("host"))&&s["transport"] in listOf("udp","tcp")&&(s["port"] as? Number)?.toInt() in 1..65535){"Valid public gateway hostname, port and transport required"}
        require((s["max_sessions"] as? Number)?.toInt() in 1..200&&s["ipv6_policy"]=="block")
        val old=read("servers/$id");val value=s+mapOf("verified" to false,"published" to false,"config_version" to ((old.getLong("config_version")?:0)+1),"updated_at" to FieldValue.serverTimestamp())
        db.runBatch{it.set(db.document("servers/$id"),value);it.delete(db.document("public_servers/$id"))}.await();audit("server.save",id);return mapOf("id" to id)
    }
    private suspend fun manageServer(d:Map<String,Any?>):Map<String,Any?> {
        val id=identifier(d["id"]);val action=d["action"];val ref=db.document("servers/$id");val s=read("servers/$id").data?:error("Unknown server")
        when(action){
            "remove"->db.runBatch{it.delete(ref);it.delete(db.document("public_servers/$id"))}.await()
            "disable"->db.runBatch{it.update(ref,mapOf("enabled" to false,"published" to false));it.delete(db.document("public_servers/$id"))}.await()
            "configure","rotate"->{remote(s,"/control/$action",if(action=="configure")s.filterKeys{it in listOf("port","transport","dns","max_sessions")}else emptyMap())}
            "test","publish"->{val h=remote(s,"/control/health",emptyMap());check(h["online"]==true&&h["protocol"]=="openvpn"&&h["gateway_id"]==id){"Gateway identity/service verification failed"};check(h["host"]==s["host"]&&h["port"].toString()==s["port"].toString()&&h["transport"]==s["transport"]&&h["dns"]==s["dns"]){"Gateway deployment differs from registration"};val health=h+mapOf("checked_at" to Timestamp.now());val pub=action=="publish"||s["published"]==true;if(action=="publish")check(s["enabled"]==true&&s["maintenance"]!=true){"Enable gateway and exit maintenance first"};db.runBatch{b->b.update(ref,mapOf("health" to health,"verified" to true,"published" to pub,"last_success_at" to FieldValue.serverTimestamp()));if(pub)b.set(db.document("public_servers/$id"),s.filterKeys{it in setOf("name","country","city","flag","host","transport","port","dns","max_sessions","priority","role","enabled","maintenance","maintenance_message","ca_reference","tls_profile_reference","ipv6_policy","config_version","description","provider")}+mapOf("id" to id,"health" to health,"published" to true,"verified" to true))}.await()}
            else->error("Unsupported action")
        };audit("server.$action",id);return mapOf("ok" to true)
    }
    private fun importOvpn(text:String):Map<String,Any?> {
        require(text.toByteArray().size<=65536){"Maximum 64 KiB"}
        require(!Regex("(?im)<\\s*(key|pkcs12|secret|tls-auth|tls-crypt|tls-crypt-v2|auth-user-pass)\\b|PRIVATE KEY|^\\s*(up|down|plugin|script-security|management|auth-user-pass|key|pkcs12|secret|tls-auth|tls-crypt|tls-crypt-v2|config|include|http-proxy|socks-proxy)\\b").containsMatchIn(text)){"Profile contains private credentials or unsupported hooks; rejected"}
        val clean=text.replace(Regex("(?is)<(ca|cert)>.*?</\\1>"),"");require(!clean.contains('<')&&!clean.contains('>'))
        val lines=clean.lines().map{it.trim()}.filter{it.isNotBlank()&&!it.startsWith('#')&&!it.startsWith(';')}
        val allowed=setOf("client","dev","proto","remote","remote-cert-tls","verify-x509-name","cipher","data-ciphers","auth","tls-version-min","nobind","resolv-retry","persist-key","persist-tun","verb","redirect-gateway","dhcp-option","auth-nocache","reneg-sec","connect-timeout","float","explicit-exit-notify")
        require(lines.all{it.substringBefore(' ') in allowed}){"Unsupported profile directive"};val remote=lines.filter{it.startsWith("remote ")};require(remote.size==1);val fields=remote.single().split(Regex("\\s+"));require(fields.size==3&&publicHost(fields[1])&&fields[2].toIntOrNull() in 1..65535);require(lines.any{it.startsWith("data-ciphers ")&&it.contains("-GCM")}&&!lines.any{it=="auth SHA1"||it=="auth MD5"}){"Modern AEAD crypto required"}
        val proto=lines.singleOrNull{it.startsWith("proto ")}?.substringAfter("proto ")?.trim();require(proto in listOf("udp","tcp","tcp-client")){"OpenVPN UDP/TCP transport required"};return mapOf("host" to fields[1],"port" to fields[2].toInt(),"transport" to if(proto=="udp")"udp" else "tcp","requires_gateway_verification" to true)
    }
    private suspend fun remote(s:Map<String,Any?>,path:String,data:Map<String,Any?>):Map<String,Any?> {
        val host=s.str("host");require(publicHost(host)){"Public HTTPS gateway hostname required"}
        val token=auth.currentUser?.getIdToken(false)?.await()?.token?:error("Sign in first")
        val attestation=withTimeoutOrNull(2500){runCatching{com.google.firebase.appcheck.FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token}.getOrNull()}
        return withContext(Dispatchers.IO){
            val c=URL("https://$host$path").openConnection() as HttpsURLConnection;c.instanceFollowRedirects=false;c.connectTimeout=8000;c.readTimeout=45000;c.requestMethod="POST";c.doOutput=true;c.setRequestProperty("Authorization","Firebase $token");c.setRequestProperty("Content-Type","application/json");if(attestation!=null)c.setRequestProperty("X-Firebase-AppCheck",attestation)
            try{c.outputStream.use{it.write(JSONObject(data).toString().toByteArray())};check(c.responseCode==200){"Gateway unavailable or authorization denied (${c.responseCode}). A real deployed gateway is required."};val bytes=c.inputStream.use{it.readBounded(131073)};check(bytes.size<=131072);JSONObject(String(bytes)).let{j->j.keys().asSequence().associateWith{k->jsonValue(j.get(k))}}}finally{c.disconnect()}
        }
    }
    private fun jsonValue(v:Any?):Any?=when(v){JSONObject.NULL->null;is JSONObject->v.keys().asSequence().associateWith{jsonValue(v.get(it))};is org.json.JSONArray->(0 until v.length()).map{jsonValue(v.get(it))};else->v}
}
