package com.b5studios.usafavor

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b5studios.shared.*
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray

class AdminApplication:Application(){override fun onCreate(){super.onCreate();initializeFirebase(this,BuildConfig.DEBUG)}}
data class AdminState(val ready:Boolean=false,val cursor:String="",val busy:Boolean=false,val error:String="",val message:String="",val stats:Map<String,Any?> = emptyMap(),val licenses:List<Map<String,Any?>> = emptyList(),val settings:Map<String,Any?> = emptyMap(),val social:List<Map<String,Any?>> = emptyList(),val conflicts:List<Map<String,Any?>> = emptyList(),val sections:List<Map<String,Any?>> = emptyList(),val layout:Map<String,Any?> = emptyMap(),val profiles:List<Map<String,Any?>> = emptyList())
class AdminModel(app:Application):AndroidViewModel(app){
 private var conflictWatch:com.google.firebase.firestore.ListenerRegistration?=null
 val api=Backend(app);private val _state=MutableStateFlow(AdminState());val state=_state.asStateFlow()
 init{if(api.configured&&api.auth.currentUser!=null)run{api.auth.currentUser!!.getIdToken(true).addOnSuccessListener { if(it.claims["admin"]==true){_state.value=_state.value.copy(ready=true);refresh()} }}}
 fun work(block:suspend ()->Unit){if(_state.value.busy)return;viewModelScope.launch{_state.value=_state.value.copy(busy=true,error="",message="");try{block()}catch(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"Request failed")}finally{_state.value=_state.value.copy(busy=false)}}}
 fun login(email:String,password:String)=work{api.login(email,password);_state.value=_state.value.copy(ready=true);load()}
 fun reset(email:String)=work{api.reset(email);_state.value=_state.value.copy(message="If this account exists, password reset instructions have been sent")}
 suspend fun load(){val stats=api.call("dashboard");val page=api.call("listLicenses");val licenses=page["items"].rows();val config=api.call("adminConfiguration");val conflicts=api.call("listDeviceConflicts")["items"].rows();_state.value=_state.value.copy(stats=stats,licenses=licenses,cursor=page.str("next_cursor"),settings=config["settings"].obj(),social=config["social"].rows(),conflicts=conflicts,sections=config["sections"].rows(),layout=config["layout"].obj(),profiles=config["profiles"].rows());if(conflictWatch==null)conflictWatch=com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("device_conflicts").orderBy("attempted_at",com.google.firebase.firestore.Query.Direction.DESCENDING).limit(100).addSnapshotListener{snapshot,error->if(error==null&&snapshot!=null&&!snapshot.metadata.isFromCache){_state.value=_state.value.copy(conflicts=snapshot.documents.map{doc->doc.data.orEmpty()+mapOf("id" to doc.id,"attempted_at" to doc.getTimestamp("attempted_at")?.toDate()?.time)})}}}
 fun refresh()=work{load()}
 fun more()=work{val r=api.call("listLicenses",mapOf("cursor" to _state.value.cursor));_state.value=_state.value.copy(licenses=(_state.value.licenses+r["items"].rows()).distinctBy{it["id"]},cursor=r.str("next_cursor"))}
 fun searchAll(code:String)=work{val r=api.call("listLicenses",mapOf("search" to code));_state.value=_state.value.copy(licenses=r["items"].rows(),cursor="")}

 fun act(name:String,data:Map<String,Any?>,after:(Map<String,Any?>)->Unit={})=work{val result=api.call(name,data);after(result);load();_state.value=_state.value.copy(message="Saved successfully")}
 fun import(text:String)=work{val r=api.call("importOvpn",mapOf("text" to text));_state.value=_state.value.copy(message="Validated public summary: ${JSONObject(r)}. Register and verify an owned gateway before publishing.")}
 override fun onCleared(){conflictWatch?.remove();super.onCleared()}
 fun logout(){conflictWatch?.remove();conflictWatch=null;if(api.configured)api.auth.signOut();_state.value=AdminState()}
}
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{FavorTheme(admin=true){AdminScreen()}}}}
@Composable fun AdminScreen(model:AdminModel=viewModel()){
 val s by model.state.collectAsStateWithLifecycle();var page by remember{mutableIntStateOf(0)};var manageTab by remember{mutableIntStateOf(0)};var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
 val context=androidx.compose.ui.platform.LocalContext.current
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)model.work{val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBounded(65537)}?:error("Cannot read file");require(bytes.size<=65536){"Profile exceeds 64 KiB"};val r=model.api.call("importOvpn",mapOf("text" to bytes.toString(Charsets.UTF_8)));android.widget.Toast.makeText(context,"Safe summary: ${JSONObject(r)}. Gateway verification required.",android.widget.Toast.LENGTH_LONG).show()}}
 Backdrop{Scaffold(containerColor=androidx.compose.ui.graphics.Color.Transparent,bottomBar={if(s.ready)PremiumNav(page,admin=true){page=it}}){pad->Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal=20.dp)){
  if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Violet)
  if(s.error.isNotEmpty())Glass("Request failed"){Text(s.error,color=MaterialTheme.colorScheme.error)}
  if(s.message.isNotEmpty())Glass{Text(s.message)}
  if(!s.ready){Spacer(Modifier.height(30.dp));Brand(true,true);Glass("Administrator sign in"){Field("Email",email,{email=it});Field("Password",password,{password=it},true);Action("SIGN IN",!s.busy){model.login(email,password)};TextButton(onClick={model.reset(email)},enabled=!s.busy){Text("Reset password")}};Text("Private management • Spark license service • No Cloud Functions",color=Gold);Skyline()}
  else{Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){Brand()};TextButton(onClick={model.refresh()},enabled=!s.busy){Text("Refresh")};TextButton(onClick={model.logout()}){Text("Sign out")}}
   when(page){
    0->OrganizedDashboard(s,{page=it},{manageTab=2;page=3})
    1->LicensePage(s,model)
    2->ServerPage(s,model){picker.launch(arrayOf("application/x-openvpn-profile","application/octet-stream","text/plain","*/*"))}
    3->{PageTitle("Management studio");Row(Modifier.horizontalScroll(rememberScrollState())){listOf("App information","Client sections","Admin layout","VPN files","Free setup").forEachIndexed{i,label->FilterChip(selected=manageTab==i,onClick={manageTab=i},label={Text(label)},modifier=Modifier.padding(end=7.dp))}};when(manageTab){0->SettingsPage(s,model);1->SectionEditor(s,model);2->AdminLayoutEditor(s,model);3->ProfileLibrary(s,model);4->SparkSetupCard()}}
    4->SocialPage(s,model)
   }
  };Spacer(Modifier.height(24.dp))
 }}}
}
@Composable fun LicensePage(s:AdminState,m:AdminModel){
 var days by remember{mutableStateOf("30")};var policy by remember{mutableStateOf("first_activation")};var generated by remember{mutableStateOf("")};var search by remember{mutableStateOf("")};var filter by remember{mutableStateOf("all")};var confirm by remember{mutableStateOf<Map<String,Any?>?>(null)};var extend by remember{mutableStateOf("30")}
 var sessions by remember{mutableStateOf<List<Map<String,Any?>>?>(null)}
 val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current;val context=androidx.compose.ui.platform.LocalContext.current
 PageTitle("License studio");Text("Single-use codes • one installation per license • Firestore Spark",color=Violet)
 if(s.conflicts.isNotEmpty())Glass("Attempts from another installation") { s.conflicts.take(15).forEach { attempt->
 val license=s.licenses.firstOrNull{it.str("id")==attempt.str("license_id")}
 Text(license?.str("code")?:attempt.str("license_id").take(12),color=Gold,fontWeight=FontWeight.Bold)
 Fact("Reported phone",attempt.str("device_model"));Fact("Attempted",date(attempt["attempted_at"]))
 Text("Second activation was rejected. This is an attempt, not proof of two live VPN sessions.",color=Violet)
 Action("BAN THIS LICENSE",!s.busy){m.act("manageLicense",mapOf("id" to attempt["license_id"],"action" to "revoke"))}
 } }
Glass("Create a subscription"){
  Row(Modifier.horizontalScroll(rememberScrollState())){listOf(1,3,7,15,30,60,90,180,365).forEach{d->FilterChip(selected=days==d.toString(),onClick={days=d.toString()},label={Text("${d}d")},modifier=Modifier.padding(end=6.dp))}}
  Field("Duration days · custom supported",days,{days=it.filter(Char::isDigit)})
  Row{RadioButton(policy=="first_activation",{policy="first_activation"});Text("Start on activation",Modifier.padding(top=12.dp))};Row{RadioButton(policy=="generation",{policy="generation"});Text("Start now",Modifier.padding(top=12.dp))}
  Action("GENERATE LICENSE",!s.busy){m.act("generateLicense",mapOf("duration_days" to days.toIntOrNull(),"activation_policy" to policy)){generated=it.str("code")}}
  if(generated.isNotBlank()){Text(generated,color=Gold,fontWeight=FontWeight.Bold);Action("COPY CODE"){clipboard.setText(androidx.compose.ui.text.AnnotatedString(generated))};Action("SHARE VIA WHATSAPP"){val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,generated);setPackage("com.whatsapp")};runCatching{context.startActivity(intent)}.onFailure{context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,generated)},"Share license"))}}}
 }
 Field("Search loaded licenses",search,{search=it});Action("SEARCH ENTIRE INVENTORY BY FULL CODE",!s.busy&&search.length==13){m.searchAll(search)};Row(Modifier.horizontalScroll(rememberScrollState())){listOf("all","active","expired","revoked","unused").forEach{v->FilterChip(selected=filter==v,onClick={filter=v},label={Text(v)},modifier=Modifier.padding(end=6.dp))}}
 Text("${s.licenses.size} loaded licenses • refresh for current status",color=Violet);if(s.cursor.isNotEmpty()&&s.cursor!="null")Action("LOAD MORE LICENSES",!s.busy){m.more()}
 s.licenses.filter{(filter=="all"||it.str("status")==filter)&&it.str("code").contains(search,true)}.forEach{l->Glass(l.str("code")){Fact("Status",l.str("status"));Fact("Activated",date(l["activated_at"]));Fact("Expires",date(l["expires_at"]));Fact("Device bound",if(l["device_binding_id"]!=null)"Yes" else "No");Action("COPY"){clipboard.setText(androidx.compose.ui.text.AnnotatedString(l.str("code")))};Row{IconAction("Revoke license",!s.busy){m.act("manageLicense",mapOf("id" to l["id"],"action" to "revoke"))};IconAction("Reactivate license",!s.busy){m.act("manageLicense",mapOf("id" to l["id"],"action" to "reactivate"))}};Field("Extend by days",extend,{extend=it.filter(Char::isDigit)});Action("EXTEND",!s.busy){m.act("manageLicense",mapOf("id" to l["id"],"action" to "extend","days" to extend.toIntOrNull()))};Action("VIEW DEVICE / ATTEMPTS",!s.busy){m.work{sessions=m.api.call("listSessions",mapOf("license_id" to l["id"]))["items"].rows()}};TextButton(onClick={confirm=l+mapOf("action" to "reset")}){Text("Reset device binding")};IconAction("Delete license"){confirm=l+mapOf("action" to "delete")}}}
 sessions?.let{list->AlertDialog(onDismissRequest={sessions=null},title={Text("Authorized sessions")},text={Column(Modifier.verticalScroll(rememberScrollState())){if(list.isEmpty())Text("No sessions");list.forEach{Text("${if(it.str("kind")=="other_installation")"REJECTED ACTIVATION" else "BOUND DEVICE"} • ${it.str("device_model")}\n${it.str("uid")}\n${date(it["attempted_at"]?:it["last_seen"])}",color=if(it.str("kind")=="other_installation")Violet else Gold)}}},confirmButton={TextButton(onClick={sessions=null}){Text("Close")}})}
 confirm?.let{c->AlertDialog(onDismissRequest={confirm=null},title={Text("Confirm ${c.str("action")}")},text={Text("${c.str("code")} • Existing sessions will lose authorization.")},confirmButton={TextButton(onClick={m.act("manageLicense",mapOf("id" to c["id"],"action" to c["action"]));confirm=null}){Text("Confirm")}},dismissButton={TextButton(onClick={confirm=null}){Text("Cancel")}})}
}
private fun defaults()=mapOf<String,Any?>("name" to "","country" to "United States","city" to "New York","flag" to "🇺🇸","host" to "","port" to 1194,"transport" to "udp","dns" to listOf("1.1.1.1","9.9.9.9"),"max_sessions" to 100,"priority" to 10,"role" to "standard","enabled" to false,"maintenance" to false,"maintenance_message" to "","ca_reference" to "gateway-managed","tls_profile_reference" to "tls-crypt-v2","ipv6_policy" to "block","description" to "","provider" to "","private_notes" to "")
@Composable fun ServerPage(s:AdminState,m:AdminModel,import:()->Unit){
 var selected by remember{mutableStateOf("")};var form by remember{mutableStateOf(defaults().mapValues{if(it.value is List<*>) (it.value as List<*>).joinToString(",")else it.value.toString()})};var confirm by remember{mutableStateOf("")}
 val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
 PageTitle("VPN Control Center");Text("Registered gateways must be provisioned and verified before publishing.",color=Violet)
 Action("IMPORT OVPN CONFIG",!s.busy,import)
 s.stats["server_items"].rows().sortedBy{(it["priority"] as? Number)?.toInt()?:0}.forEach{v->Glass(v.str("name")){Fact("Location","${v.str("country")} · ${v.str("city")}");Fact("Endpoint","${v.str("host")}:${v.str("port")} / ${v.str("transport")}");Fact("Role",v.str("role"));Fact("Published",v.str("published"));Fact("Version",v.str("config_version"));Fact("Last health",date(v["health"].obj()["checked_at"]));Fact("Last successful check",date(v["last_success_at"]));Fact("Gateway sessions",v["health"].obj().str("active_sessions"));Action("EDIT"){selected=v.str("id");form=defaults().plus(v).filterKeys{it in defaults()}.mapValues{if(it.value is List<*>)(it.value as List<*>).joinToString(",")else it.value.toString()}};Row(Modifier.horizontalScroll(rememberScrollState())){listOf("test","publish","configure","disable","rotate").forEach{a->TextButton(onClick={m.act("manageServer",mapOf("id" to v["id"],"action" to a))},enabled=!s.busy){Text(a)}}};TextButton(onClick={confirm=v.str("id")}){Text("Remove registration",color=MaterialTheme.colorScheme.error)}}}
 Glass(if(selected.isEmpty())"Add new VPN server" else "Edit gateway registration"){
  if(selected.isNotEmpty()){Fact("Gateway ID",selected);Action("COPY GATEWAY ID"){clipboard.setText(androidx.compose.ui.text.AnnotatedString(selected))}}
  listOf("name","country","city","flag","host","port","transport","dns","max_sessions","priority","role","ca_reference","tls_profile_reference","ipv6_policy","maintenance_message","description","provider","private_notes").forEach{k->Field(k.replace('_',' '),form[k]?:"",{form=form+(k to it)})}
  listOf("enabled","maintenance").forEach{k->Row(verticalAlignment=Alignment.CenterVertically){Switch(form[k]=="true",{form=form+(k to it.toString())});Text(k,Modifier.padding(start=8.dp))}}
  Text("Transport: udp/tcp • role: primary/backup/standard • IPv6: block. Port changes require matching gateway deployment.",color=Violet)
  Action("SAVE REGISTRATION",!s.busy){val v=form.mapValues{(k,v)->when(k){"port","max_sessions","priority"->v.toIntOrNull();"enabled","maintenance"->v=="true";"dns"->v.split(',').map{it.trim()};else->v}};m.act("saveServer",mapOf("id" to selected.ifEmpty{null},"server" to v)){selected=it.str("id")}}
  TextButton(onClick={selected="";form=defaults().mapValues{if(it.value is List<*>)(it.value as List<*>).joinToString(",")else it.value.toString()}}){Text("New registration")}
 }
 if(confirm.isNotEmpty())AlertDialog(onDismissRequest={confirm=""},title={Text("Remove gateway?")},text={Text("The registration will be removed. This does not destroy the VPS.")},confirmButton={TextButton(onClick={m.act("manageServer",mapOf("id" to confirm,"action" to "remove"));confirm=""}){Text("Remove")}},dismissButton={TextButton(onClick={confirm=""}){Text("Cancel")}})
}
@Composable fun SettingsPage(s:AdminState,m:AdminModel){var fields by remember(s.settings){mutableStateOf(s.settings.mapValues{it.value.toString()})};PageTitle("App management");Glass("Remote client settings"){
 listOf("welcome","announcement","maintenance_message","default_server","server_order","support","privacy_url","terms_url","help","activation_popup","minimum_version","update_notice").forEach{k->Field(k.replace('_',' '),fields[k]?:if(k=="minimum_version")"1" else "",{fields=fields+(k to it)})};listOf("maintenance","show_social","show_tips").forEach{k->Row(verticalAlignment=Alignment.CenterVertically){Switch(fields[k]=="true",{fields=fields+(k to it.toString())});Text(k)}}
 Action("PUBLISH SETTINGS",!s.busy){m.act("saveConfiguration",mapOf("settings" to fields.mapValues{(k,v)->if(k=="minimum_version")v.toIntOrNull()else if(k in listOf("maintenance","show_social","show_tips"))v=="true" else v}))}
}}
@Composable fun SocialPage(s:AdminState,m:AdminModel){var items by remember(s.social){mutableStateOf(s.social)};PageTitle("Social links");Text("HTTPS or support email • order controls client display",color=Violet)
 items.forEachIndexed{i,x->Glass(x.str("name").ifEmpty{"New link"}){listOf("name","icon","url").forEach{k->Field(k,x.str(k),{v->items=items.toMutableList().also{it[i]=x+(k to v)}})};Row(verticalAlignment=Alignment.CenterVertically){Switch(x["enabled"]==true,{v->items=items.toMutableList().also{it[i]=x+("enabled" to v)}});TextButton(onClick={if(i>0)items=items.toMutableList().also{val a=it[i-1];it[i-1]=it[i];it[i]=a}},enabled=i>0){Text("Move up")};TextButton(onClick={items=items.filterIndexed{n,_->n!=i}}){Text("Remove")}}}}
 Action("ADD LINK"){items=items+mapOf("name" to "","icon" to "whatsapp","url" to "","enabled" to true)};Text("Icons: whatsapp, telegram, instagram, tiktok, facebook, youtube, website, email, custom",color=Violet);Action("PUBLISH SOCIAL LINKS",!s.busy){m.act("saveSocial",mapOf("items" to items))}
}

@Composable private fun OrganizedDashboard(s:AdminState,navigate:(Int)->Unit,customize:()->Unit){
 val enabled=(s.layout["enabled_panels"] as? List<*>)?.map{it.toString()}?:listOf("overview","license_health","security_alerts","infrastructure","quick_actions")
 PageTitle("Control center","B5 STUDIOS • PRIVATE MANAGEMENT")
 if(s.stats.isEmpty())SparkSetupCard()
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Firestore Spark · online license management",color=Violet,fontSize=10.sp);TextButton(onClick=customize){Text("Customize")}}
 if("overview" in enabled){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){MetricTile("Total licenses",s.stats["total"]?.toString()?:"—",Gold,Modifier.weight(1f)){navigate(1)};MetricTile("Active",s.stats["active"]?.toString()?:"—",androidx.compose.ui.graphics.Color(0xFF65EBA4),Modifier.weight(1f)){navigate(1)}}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){MetricTile("Expired",s.stats["expired"]?.toString()?:"—",Violet,Modifier.weight(1f)){navigate(1)};MetricTile("Banned",s.stats["revoked"]?.toString()?:"—",androidx.compose.ui.graphics.Color(0xFFFF8192),Modifier.weight(1f)){navigate(1)}}
 }
 if("license_health" in enabled)Glass("Subscription overview"){Fact("Unused codes",s.stats.str("unused").ifBlank{"—"});Fact("Activated in last 7 days",s.stats.str("recent").ifBlank{"—"});Action("OPEN LICENSE STUDIO"){navigate(1)}}
 if("security_alerts" in enabled)Glass("Device protection"){Fact("Recorded conflicts (latest 100)",s.conflicts.size.toString());Text("One code is bound to one installation. Another installation is refused; you choose whether to ban it.",color=androidx.compose.ui.graphics.Color(0xFFBDB9D3));Action("REVIEW DEVICE ATTEMPTS"){navigate(1)}}
 if("infrastructure" in enabled)Glass("VPN control center"){Fact("Registered gateways",s.stats.str("servers").ifBlank{"—"});Fact("Verified online",s.stats.str("online").ifBlank{"—"});Fact("Measured active tunnels",s.stats.str("active_sessions").ifBlank{"—"});Action("MANAGE SERVERS"){navigate(2)}}
 if("quick_actions" in enabled)Glass("Management workspace"){Action("CLIENT INFORMATION & SECTIONS"){navigate(3)};Action("SOCIAL LINKS & SUPPORT"){navigate(4)}}
 val context=androidx.compose.ui.platform.LocalContext.current
 s.layout["custom_cards"].rows().filter{it["enabled"]==true}.forEach{x->Glass(x.str("title")){Text(x.str("body"));if(x.str("url").isNotBlank())Action("OPEN LINK"){runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(x.str("url"))))}}}}
}
@Composable private fun MetricTile(label:String,value:String,color:androidx.compose.ui.graphics.Color,modifier:Modifier,click:()->Unit){
 Box(modifier.clickable(onClick=click)){Glass{Text(label,color=androidx.compose.ui.graphics.Color(0xFFB9BDD1),fontSize=12.sp);Spacer(Modifier.height(10.dp));Text(value,color=color,fontSize=30.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp));Box(Modifier.height(2.dp).fillMaxWidth().background(color.copy(alpha=.5f)))}}
}
@Composable private fun SectionEditor(s:AdminState,m:AdminModel){
 var items by remember(s.sections){mutableStateOf(s.sections)}
 Glass("Client sections"){Text("Create information cards for Home or Settings. Hide/show and reorder them without rebuilding the client.",color=Violet)}
 items.forEachIndexed{i,x->SectionFields(x,i,items.size,{changed->items=items.toMutableList().also{it[i]=changed}},{items=items.filterIndexed{n,_->n!=i}},{if(i>0)items=items.toMutableList().also{val previous=it[i-1];it[i-1]=it[i];it[i]=previous}})}
 Action("ADD CLIENT SECTION",items.size<20){items=items+newSection()};Action("PUBLISH SECTIONS",!s.busy){m.act("saveSections",mapOf("items" to items))}
}
private fun newSection()=mapOf<String,Any?>("id" to java.util.UUID.randomUUID().toString(),"title" to "","body" to "","url" to "","icon" to "info","target" to "home","enabled" to true)
@Composable private fun SectionFields(x:Map<String,Any?>,index:Int,total:Int,change:(Map<String,Any?>)->Unit,remove:()->Unit,up:()->Unit,showTargets:Boolean=true){
 Glass(x.str("title").ifBlank{"New section ${index+1}"}){
  Field("Title",x.str("title"),{change(x+("title" to it))});Field("Body text",x.str("body"),{change(x+("body" to it))});Field("Optional HTTPS / email URL",x.str("url"),{change(x+("url" to it))})
  Row(Modifier.horizontalScroll(rememberScrollState())){listOf("info","support","shield","globe","message").forEach{icon->FilterChip(x.str("icon")==icon,{change(x+("icon" to icon))},label={Text(icon)},modifier=Modifier.padding(end=6.dp))}}
  if(showTargets)Row{listOf("home","settings").forEach{target->FilterChip(x.str("target")==target,{change(x+("target" to target))},label={Text(target)},modifier=Modifier.padding(end=8.dp))}}
  Row(verticalAlignment=Alignment.CenterVertically){Switch(x["enabled"]==true,{change(x+("enabled" to it))});Text(if(x["enabled"]==true)"Visible" else "Hidden");Spacer(Modifier.weight(1f));TextButton(onClick=up,enabled=index>0){Text("↑")};TextButton(onClick=remove){Text("Remove",color=MaterialTheme.colorScheme.error)}}
 }
}
@Composable private fun AdminLayoutEditor(s:AdminState,m:AdminModel){
 val panels=listOf("overview","license_health","security_alerts","infrastructure","quick_actions")
 var enabled by remember(s.layout){mutableStateOf((s.layout["enabled_panels"] as? List<*>)?.map{it.toString()}?:panels)}
 var cards by remember(s.layout){mutableStateOf(s.layout["custom_cards"].rows())}
 Glass("Organize the Admin dashboard"){
  panels.forEach{panel->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Switch(panel in enabled,{checked->enabled=if(checked)enabled+panel else enabled-panel});Text(panel.replace('_',' '),Modifier.padding(start=10.dp))}}
  Text("Navigation and license authorization remain available even when a dashboard panel is hidden.",color=Violet)
 }
 cards.forEachIndexed{i,x->SectionFields(x+mapOf("target" to "home"),i,cards.size,{value->cards=cards.toMutableList().also{it[i]=value}},{cards=cards.filterIndexed{n,_->n!=i}},{if(i>0)cards=cards.toMutableList().also{val a=it[i-1];it[i-1]=it[i];it[i]=a}},showTargets=false)}
 Action("ADD PRIVATE ADMIN CARD",cards.size<20){cards=cards+newSection()};Action("SAVE DASHBOARD LAYOUT",!s.busy){m.act("saveAdminLayout",mapOf("enabled_panels" to enabled,"custom_cards" to cards))}
}

@Composable private fun ProfileLibrary(s:AdminState,m:AdminModel){
 val context=androidx.compose.ui.platform.LocalContext.current;var confirm by remember{mutableStateOf("")};var viewing by remember{mutableStateOf<Map<String,Any?>?>(null)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)m.work{
  val pair=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){
   val name=context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else null}?:"Imported.ovpn"
   val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBounded(65537)}?:error("Cannot read file")
   require(bytes.size<=65536){"Maximum profile size is 64 KiB"};name to String(bytes,Charsets.UTF_8)
  }
  m.api.call("saveProfileDocument",mapOf("name" to pair.first,"text" to pair.second));m.load()
 }}
 Glass("VPN configuration library"){
  Text("Import compatible public OpenVPN templates, inspect the endpoint, and create server drafts. Shared private keys, credentials and scripts are rejected.",color=Violet)
  Action("ADD OVPN FILE",!s.busy){picker.launch(arrayOf("application/x-openvpn-profile","application/octet-stream","text/plain","*/*"))}
  Text("Profiles are stored as private Firestore documents; no paid Storage bucket or Cloud Function is needed. A file does not prove control of its server.",fontSize=12.sp,color=androidx.compose.ui.graphics.Color(0xFFB9BDD1))
 }
 s.profiles.forEach{p->Glass(p.str("name")){
  val summary=p["summary"].obj();Fact("Endpoint","${summary.str("host")}:${summary.str("port")}");Fact("Transport",summary.str("transport").uppercase());Fact("Imported",date(p["created_at"]));Text("Gateway verification required",color=Gold,fontSize=12.sp)
  Action("VIEW PUBLIC PROFILE"){viewing=p};Action("CREATE SERVER DRAFT",!s.busy){m.act("createServerFromProfile",mapOf("id" to p["id"]))};TextButton(onClick={confirm=p.str("id")}){Text("Remove file",color=MaterialTheme.colorScheme.error)}
 }}
 viewing?.let{p->AlertDialog(onDismissRequest={viewing=null},title={Text(p.str("name"))},text={Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState())){Text(p.str("public_profile"),fontSize=10.sp,fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace)}},confirmButton={TextButton(onClick={viewing=null}){Text("Close")}})}
 if(confirm.isNotEmpty())AlertDialog(onDismissRequest={confirm=""},title={Text("Delete profile file?")},text={Text("This removes the stored template, not the VPS or a registered server.")},confirmButton={TextButton(onClick={m.act("deleteProfileDocument",mapOf("id" to confirm));confirm=""}){Text("Delete")}},dismissButton={TextButton(onClick={confirm=""}){Text("Cancel")}})
}

@Composable private fun SparkSetupCard(){
 val context=androidx.compose.ui.platform.LocalContext.current;val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
 Glass("Free licensing setup · no card"){
  Text("1. Create a Standard / Native Firestore database named (default).\n2. Publish the supplied rules.\n3. Enable Anonymous authentication for the Client. Your administrator role stays unchanged.",color=androidx.compose.ui.graphics.Color(0xFFD9D7E9),fontSize=13.sp)
  Action("COPY FIRESTORE RULES"){
   val text=context.resources.openRawResource(com.b5studios.shared.R.raw.firestore_spark_rules).bufferedReader().use{it.readText()}
   clipboard.setText(androidx.compose.ui.text.AnnotatedString(text));android.widget.Toast.makeText(context,"Rules copied. Paste into Firestore → Rules, then Publish.",android.widget.Toast.LENGTH_LONG).show()
  }
  Action("OPEN FIRESTORE CONSOLE"){context.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://console.firebase.google.com/project/usa-favor/firestore")))}
  TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://console.firebase.google.com/project/usa-favor/authentication/providers")))}){Text("Open authentication providers")}
  Text("Spark has usage quotas. These steps enable licensing; they do not create or verify a VPN server.",color=Violet,fontSize=11.sp)
 }
}
