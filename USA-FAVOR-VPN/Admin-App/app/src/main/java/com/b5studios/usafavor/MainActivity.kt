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
data class AdminState(val ready:Boolean=false,val cursor:String="",val busy:Boolean=false,val error:String="",val message:String="",val stats:Map<String,Any?> = emptyMap(),val licenses:List<Map<String,Any?>> = emptyList(),val settings:Map<String,Any?> = emptyMap(),val social:List<Map<String,Any?>> = emptyList())
class AdminModel(app:Application):AndroidViewModel(app){
 val api=Backend(app);private val _state=MutableStateFlow(AdminState());val state=_state.asStateFlow()
 init{if(api.configured&&api.auth.currentUser!=null)run{api.auth.currentUser!!.getIdToken(true).addOnSuccessListener { if(it.claims["admin"]==true){_state.value=_state.value.copy(ready=true);refresh()} }}}
 fun work(block:suspend ()->Unit){if(_state.value.busy)return;viewModelScope.launch{_state.value=_state.value.copy(busy=true,error="",message="");try{block()}catch(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"Request failed")}finally{_state.value=_state.value.copy(busy=false)}}}
 fun login(email:String,password:String)=work{api.login(email,password);_state.value=_state.value.copy(ready=true);load()}
 fun reset(email:String)=work{api.reset(email);_state.value=_state.value.copy(message="If this account exists, password reset instructions have been sent")}
 private suspend fun load(){val stats=api.call("dashboard");val page=api.call("listLicenses");val licenses=page["items"].rows();val config=api.call("adminConfiguration");_state.value=_state.value.copy(stats=stats,licenses=licenses,cursor=page.str("next_cursor"),settings=config["settings"].obj(),social=config["social"].rows())}
 fun refresh()=work{load()}
 fun more()=work{val r=api.call("listLicenses",mapOf("cursor" to _state.value.cursor));_state.value=_state.value.copy(licenses=(_state.value.licenses+r["items"].rows()).distinctBy{it["id"]},cursor=r.str("next_cursor"))}
 fun searchAll(code:String)=work{val r=api.call("listLicenses",mapOf("search" to code));_state.value=_state.value.copy(licenses=r["items"].rows(),cursor="")}

 fun act(name:String,data:Map<String,Any?>,after:(Map<String,Any?>)->Unit={})=work{val result=api.call(name,data);after(result);load();_state.value=_state.value.copy(message="Saved successfully")}
 fun import(text:String)=work{val r=api.call("importOvpn",mapOf("text" to text));_state.value=_state.value.copy(message="Validated public summary: ${JSONObject(r)}. Register and verify an owned gateway before publishing.")}
 fun logout(){if(api.configured)api.auth.signOut();_state.value=AdminState()}
}
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{FavorTheme{AdminScreen()}}}}
@Composable fun AdminScreen(model:AdminModel=viewModel()){
 val s by model.state.collectAsStateWithLifecycle();var page by remember{mutableIntStateOf(0)};var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
 val context=androidx.compose.ui.platform.LocalContext.current
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)model.work{val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBounded(65537)}?:error("Cannot read file");require(bytes.size<=65536){"Profile exceeds 64 KiB"};val r=model.api.call("importOvpn",mapOf("text" to bytes.toString(Charsets.UTF_8)));android.widget.Toast.makeText(context,"Safe summary: ${JSONObject(r)}. Gateway verification required.",android.widget.Toast.LENGTH_LONG).show()}}
 Backdrop{Scaffold(containerColor=androidx.compose.ui.graphics.Color.Transparent,bottomBar={if(s.ready)NavigationBar(containerColor=Navy){listOf("Home","Licenses","VPN","App","Social").forEachIndexed{i,label->NavigationBarItem(selected=page==i,onClick={page=i},icon={Text(listOf("⌂","⚿","◈","▣","↗")[i],color=if(page==i)Gold else Violet)},label={Text(label)})}}}){pad->Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal=20.dp)){
  if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Violet)
  if(s.error.isNotEmpty())Glass("Request failed"){Text(s.error,color=MaterialTheme.colorScheme.error)}
  if(s.message.isNotEmpty())Glass{Text(s.message)}
  if(!s.ready){Spacer(Modifier.height(30.dp));Brand(true,true);Glass("Administrator sign in"){Field("Email",email,{email=it});Field("Password",password,{password=it},true);Action("SIGN IN",!s.busy){model.login(email,password)};TextButton(onClick={model.reset(email)},enabled=!s.busy){Text("Reset password")}};Text("Private management • Explicit administrator authorization required",color=Gold);Skyline()}
  else{Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){Brand()};TextButton(onClick={model.refresh()},enabled=!s.busy){Text("Refresh")};TextButton(onClick={model.logout()}){Text("Sign out")}}
   when(page){
    0->{PageTitle("Control dashboard");Glass("Licenses • live backend counts"){listOf("total","active","expired","revoked","unused","recent").forEach{Fact(it.replaceFirstChar{c->c.uppercase()},s.stats[it]?.toString()?:"Unavailable")}};Glass("VPN infrastructure"){Fact("Registered servers",s.stats.str("servers"));Fact("Online",s.stats.str("online"));Fact("Offline / unknown",s.stats.str("offline"));Fact("Measured active sessions",s.stats.str("active_sessions"));Fact("Authorized sessions",s.stats.str("sessions"));s.stats["server_items"].rows().forEach{Fact(it.str("name"),"${it.str("role")} • ${if(it["health"].obj()["online"]==true)"Online" else "Unknown / offline"}");Text("Last check: ${date(it["health"].obj()["checked_at"])}")}}}
    1->LicensePage(s,model)
    2->ServerPage(s,model){picker.launch(arrayOf("application/x-openvpn-profile","application/octet-stream","text/plain","*/*"))}
    3->SettingsPage(s,model)
    4->SocialPage(s,model)
   }
  };Spacer(Modifier.height(24.dp))
 }}}
}
@Composable fun LicensePage(s:AdminState,m:AdminModel){
 var days by remember{mutableStateOf("30")};var policy by remember{mutableStateOf("first_activation")};var generated by remember{mutableStateOf("")};var search by remember{mutableStateOf("")};var filter by remember{mutableStateOf("all")};var confirm by remember{mutableStateOf<Map<String,Any?>?>(null)};var extend by remember{mutableStateOf("30")}
 var sessions by remember{mutableStateOf<List<Map<String,Any?>>?>(null)}
 val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current;val context=androidx.compose.ui.platform.LocalContext.current
 PageTitle("License studio");Glass("Create a subscription"){
  Row(Modifier.horizontalScroll(rememberScrollState())){listOf(1,3,7,15,30,60,90,180,365).forEach{d->FilterChip(selected=days==d.toString(),onClick={days=d.toString()},label={Text("${d}d")},modifier=Modifier.padding(end=6.dp))}}
  Field("Duration days · custom supported",days,{days=it.filter(Char::isDigit)})
  Row{RadioButton(policy=="first_activation",{policy="first_activation"});Text("Start on activation",Modifier.padding(top=12.dp))};Row{RadioButton(policy=="generation",{policy="generation"});Text("Start now",Modifier.padding(top=12.dp))}
  Action("GENERATE LICENSE",!s.busy){m.act("generateLicense",mapOf("duration_days" to days.toIntOrNull(),"activation_policy" to policy)){generated=it.str("code")}}
  if(generated.isNotBlank()){Text(generated,color=Gold,fontWeight=FontWeight.Bold);Action("COPY CODE"){clipboard.setText(androidx.compose.ui.text.AnnotatedString(generated))};Action("SHARE VIA WHATSAPP"){val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,generated);setPackage("com.whatsapp")};runCatching{context.startActivity(intent)}.onFailure{context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,generated)},"Share license"))}}}
 }
 Field("Search loaded licenses",search,{search=it});Action("SEARCH ENTIRE INVENTORY BY FULL CODE",!s.busy&&search.length==13){m.searchAll(search)};Row(Modifier.horizontalScroll(rememberScrollState())){listOf("all","active","expired","revoked","unused").forEach{v->FilterChip(selected=filter==v,onClick={filter=v},label={Text(v)},modifier=Modifier.padding(end=6.dp))}}
 Text("${s.licenses.size} loaded licenses • refresh for current status",color=Violet);if(s.cursor.isNotEmpty()&&s.cursor!="null")Action("LOAD MORE LICENSES",!s.busy){m.more()}
 s.licenses.filter{(filter=="all"||it.str("status")==filter)&&it.str("code").contains(search,true)}.forEach{l->Glass(l.str("code")){Fact("Status",l.str("status"));Fact("Activated",date(l["activated_at"]));Fact("Expires",date(l["expires_at"]));Fact("Device bound",if(l["device_binding_id"]!=null)"Yes" else "No");Action("COPY"){clipboard.setText(androidx.compose.ui.text.AnnotatedString(l.str("code")))};Row{TextButton(onClick={m.act("manageLicense",mapOf("id" to l["id"],"action" to "revoke"))},enabled=!s.busy){Text("Revoke")};TextButton(onClick={m.act("manageLicense",mapOf("id" to l["id"],"action" to "reactivate"))},enabled=!s.busy){Text("Reactivate")}};Field("Extend by days",extend,{extend=it.filter(Char::isDigit)});Action("EXTEND",!s.busy){m.act("manageLicense",mapOf("id" to l["id"],"action" to "extend","days" to extend.toIntOrNull()))};Action("VIEW PERMITTED SESSIONS",!s.busy){m.work{sessions=m.api.call("listSessions",mapOf("license_id" to l["id"]))["items"].rows()}};TextButton(onClick={confirm=l+mapOf("action" to "reset")}){Text("Reset device binding")};TextButton(onClick={confirm=l+mapOf("action" to "delete")}){Text("Delete license",color=MaterialTheme.colorScheme.error)}}}
 sessions?.let{list->AlertDialog(onDismissRequest={sessions=null},title={Text("Authorized sessions")},text={Column(Modifier.verticalScroll(rememberScrollState())){if(list.isEmpty())Text("No sessions");list.forEach{Text("${it.str("server_id")} • Expires ${date(it["expires_at"])} • Revoked: ${it.str("revoked")}")}}},confirmButton={TextButton(onClick={sessions=null}){Text("Close")}})}
 confirm?.let{c->AlertDialog(onDismissRequest={confirm=null},title={Text("Confirm ${c.str("action")}")},text={Text("${c.str("code")} • Existing sessions will lose authorization.")},confirmButton={TextButton(onClick={m.act("manageLicense",mapOf("id" to c["id"],"action" to c["action"]));confirm=null}){Text("Confirm")}},dismissButton={TextButton(onClick={confirm=null}){Text("Cancel")}})}
}
private fun defaults()=mapOf<String,Any?>("name" to "","country" to "United States","city" to "New York","flag" to "🇺🇸","host" to "","port" to 1194,"transport" to "udp","dns" to listOf("1.1.1.1","9.9.9.9"),"max_sessions" to 100,"priority" to 10,"role" to "standard","enabled" to false,"maintenance" to false,"maintenance_message" to "","ca_reference" to "gateway-managed","tls_profile_reference" to "tls-crypt-v2","ipv6_policy" to "block")
@Composable fun ServerPage(s:AdminState,m:AdminModel,import:()->Unit){
 var selected by remember{mutableStateOf("")};var form by remember{mutableStateOf(defaults().mapValues{if(it.value is List<*>) (it.value as List<*>).joinToString(",")else it.value.toString()})};var confirm by remember{mutableStateOf("")}
 val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
 PageTitle("VPN Control Center");Text("Registered gateways must be provisioned and verified before publishing.",color=Violet)
 Action("IMPORT OVPN CONFIG",!s.busy,import)
 s.stats["server_items"].rows().sortedBy{(it["priority"] as? Number)?.toInt()?:0}.forEach{v->Glass(v.str("name")){Fact("Location","${v.str("country")} · ${v.str("city")}");Fact("Endpoint","${v.str("host")}:${v.str("port")} / ${v.str("transport")}");Fact("Role",v.str("role"));Fact("Published",v.str("published"));Fact("Version",v.str("config_version"));Fact("Last health",date(v["health"].obj()["checked_at"]));Fact("Last successful check",date(v["last_success_at"]));Fact("Gateway sessions",v["health"].obj().str("active_sessions"));Action("EDIT"){selected=v.str("id");form=defaults().plus(v).filterKeys{it in defaults()}.mapValues{if(it.value is List<*>)(it.value as List<*>).joinToString(",")else it.value.toString()}};Row(Modifier.horizontalScroll(rememberScrollState())){listOf("test","publish","configure","disable","rotate").forEach{a->TextButton(onClick={m.act("manageServer",mapOf("id" to v["id"],"action" to a))},enabled=!s.busy){Text(a)}}};TextButton(onClick={confirm=v.str("id")}){Text("Remove registration",color=MaterialTheme.colorScheme.error)}}}
 Glass(if(selected.isEmpty())"Add new VPN server" else "Edit gateway registration"){
  if(selected.isNotEmpty()){Fact("Gateway ID",selected);Action("COPY GATEWAY ID"){clipboard.setText(androidx.compose.ui.text.AnnotatedString(selected))}}
  listOf("name","country","city","flag","host","port","transport","dns","max_sessions","priority","role","ca_reference","tls_profile_reference","ipv6_policy","maintenance_message").forEach{k->Field(k.replace('_',' '),form[k]?:"",{form=form+(k to it)})}
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
