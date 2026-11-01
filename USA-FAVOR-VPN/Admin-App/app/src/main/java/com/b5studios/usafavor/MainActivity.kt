package com.b5studios.usafavor

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import kotlinx.coroutines.tasks.await
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
data class AdminState(val ready:Boolean=false,val savedSession:Boolean=false,val cursor:String="",val busy:Boolean=false,val error:String="",val message:String="",val stats:Map<String,Any?> = emptyMap(),val licenses:List<Map<String,Any?>> = emptyList(),val settings:Map<String,Any?> = emptyMap(),val social:List<Map<String,Any?>> = emptyList(),val conflicts:List<Map<String,Any?>> = emptyList(),val sections:List<Map<String,Any?>> = emptyList(),val layout:Map<String,Any?> = emptyMap(),val profiles:List<Map<String,Any?>> = emptyList(),val provider:Map<String,Any?> = emptyMap(),val providerRequest:Map<String,Any?> = emptyMap(),val appUpdate:Map<String,Any?> = emptyMap())
class AdminModel(app:Application):AndroidViewModel(app){
 private var conflictWatch:com.google.firebase.firestore.ListenerRegistration?=null
 val api=Backend(app);private val _state=MutableStateFlow(AdminState());val state=_state.asStateFlow()
 init{if(api.configured&&api.auth.currentUser!=null)_state.value=_state.value.copy(savedSession=true)}
 fun unlockSavedSession()=work{
  val user=api.auth.currentUser?:error("Sign in once with your authorized owner account")
  val token=user.getIdToken(true).await()
  check(token.claims["admin"]==true){"This account is not an authorized administrator"}
  _state.value=_state.value.copy(ready=true,savedSession=true);load()
 }
 fun lock(){conflictWatch?.remove();conflictWatch=null;_state.value=AdminState(savedSession=api.configured&&api.auth.currentUser!=null)}
 fun work(block:suspend ()->Unit){if(_state.value.busy)return;viewModelScope.launch{_state.value=_state.value.copy(busy=true,error="",message="");try{block()}catch(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"Request failed")}finally{_state.value=_state.value.copy(busy=false)}}}
 fun login(email:String,password:String)=work{api.login(email,password);_state.value=_state.value.copy(ready=true,savedSession=true);load()}
 fun reset(email:String)=work{api.reset(email);_state.value=_state.value.copy(message="If this account exists, password reset instructions have been sent")}
 suspend fun load(){val stats=api.call("dashboard");val page=api.call("listLicenses");val licenses=page["items"].rows();val config=api.call("adminConfiguration");val conflicts=api.call("listDeviceConflicts")["items"].rows();_state.value=_state.value.copy(stats=stats,licenses=licenses,cursor=page.str("next_cursor"),settings=config["settings"].obj(),social=config["social"].rows(),conflicts=conflicts,sections=config["sections"].rows(),layout=config["layout"].obj(),provider=config["provider"].obj(),providerRequest=config["provider_request"].obj(),appUpdate=config["app_update"].obj());if(conflictWatch==null)conflictWatch=com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("device_conflicts").orderBy("attempted_at",com.google.firebase.firestore.Query.Direction.DESCENDING).limit(100).addSnapshotListener{snapshot,error->if(error==null&&snapshot!=null&&!snapshot.metadata.isFromCache){_state.value=_state.value.copy(conflicts=snapshot.documents.map{doc->doc.data.orEmpty()+mapOf("id" to doc.id,"attempted_at" to doc.getTimestamp("attempted_at")?.toDate()?.time)})}}}
 fun refresh()=work{load()}
 fun more()=work{val r=api.call("listLicenses",mapOf("cursor" to _state.value.cursor));_state.value=_state.value.copy(licenses=(_state.value.licenses+r["items"].rows()).distinctBy{it["id"]},cursor=r.str("next_cursor"))}
 fun searchAll(code:String)=work{val r=api.call("listLicenses",mapOf("search" to code));_state.value=_state.value.copy(licenses=r["items"].rows(),cursor="")}

 fun act(name:String,data:Map<String,Any?>,after:(Map<String,Any?>)->Unit={})=work{val result=api.call(name,data);after(result);load();_state.value=_state.value.copy(message="Saved successfully")}
 fun import(text:String)=work{val r=api.call("importOvpn",mapOf("text" to text));_state.value=_state.value.copy(message="Validated public summary: ${JSONObject(r)}. Register and verify an owned gateway before publishing.")}
 override fun onCleared(){conflictWatch?.remove();super.onCleared()}
 fun logout(){conflictWatch?.remove();conflictWatch=null;if(api.configured)api.auth.signOut();_state.value=AdminState()}
}
class MainActivity:FragmentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{FavorTheme(admin=true){AdminScreen()}}}}
@Composable fun AdminScreen(model:AdminModel=viewModel()){
 val context=androidx.compose.ui.platform.LocalContext.current
 val prefs=remember{context.getSharedPreferences("admin_interface",android.content.Context.MODE_PRIVATE)}
 var language by remember{mutableStateOf(prefs.getString("language",if(java.util.Locale.getDefault().language=="ar")"ar"else "en")?:"en")}
 CompositionLocalProvider(LocalAdminLanguage provides language,androidx.compose.ui.platform.LocalLayoutDirection provides if(language=="ar")androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr){
  AdminContent(model){selected->language=selected;prefs.edit().putString("language",selected).apply()}
 }
}
@Composable private fun AdminContent(model:AdminModel,onLanguage:(String)->Unit){
 val s by model.state.collectAsStateWithLifecycle();var page by remember{mutableIntStateOf(0)};var manageTab by remember{mutableIntStateOf(0)};var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
 val context=androidx.compose.ui.platform.LocalContext.current
 val LocalAdminLanguageValue=LocalAdminLanguage.current
 val drawer=rememberDrawerState(DrawerValue.Closed);val scope=rememberCoroutineScope()
 val credential=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){if(it.resultCode==android.app.Activity.RESULT_OK)model.unlockSavedSession()}
 fun unlock(){
  val activity=context as? FragmentActivity
  val authenticators=BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
  if(activity!=null&&BiometricManager.from(context).canAuthenticate(authenticators)==BiometricManager.BIOMETRIC_SUCCESS){
   BiometricPrompt(activity,ContextCompat.getMainExecutor(context),object:BiometricPrompt.AuthenticationCallback(){override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){model.unlockSavedSession()}}).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(AdminLanguage.translate("Unlock on this phone",LocalAdminLanguageValue)).setAllowedAuthenticators(authenticators).build())
  }else{
   val keyguard=context.getSystemService(android.app.KeyguardManager::class.java)
   val intent=keyguard.createConfirmDeviceCredentialIntent("B5 STUDIOS ADMIN","Unlock your owner session")
   if(intent!=null)credential.launch(intent)else android.widget.Toast.makeText(context,"Set a screen lock in Android settings, or sign in with your owner account.",android.widget.Toast.LENGTH_LONG).show()
  }
 }
 fun navigate(next:Int,tab:Int=-1){page=next;if(tab>=0)manageTab=tab;scope.launch{drawer.close()}}
 ModalNavigationDrawer(drawerState=drawer,gesturesEnabled=s.ready,drawerContent={if(s.ready)ModalDrawerSheet(drawerContainerColor=androidx.compose.ui.graphics.Color(0xFF091B18)){
  Brand(admin=true)
  listOf(Triple("Home",0,-1),Triple("Licenses",1,-1),Triple("VPN Connection Settings",2,-1),Triple("Client sections",3,1),Triple("Code editor",3,1),Triple("App updates",3,3),Triple("Package library",3,4),Triple("App information",3,0),Triple("Social links",4,-1)).forEach{(title,destination,tab)->NavigationDrawerItem(label={AdminText(title)},selected=page==destination&&(tab<0||manageTab==tab),onClick={navigate(destination,tab)})}
  Row(Modifier.padding(12.dp)){TextButton(onClick={onLanguage("ar")}){AdminText("العربية")};TextButton(onClick={onLanguage("en")}){AdminText("English")}}
  TextButton(onClick={model.lock();scope.launch{drawer.close()}}){AdminText("Lock")}
 }}){
 Backdrop{Scaffold(containerColor=androidx.compose.ui.graphics.Color.Transparent,bottomBar={if(s.ready)PremiumNav(page,admin=true){page=it}}){pad->Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal=20.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={onLanguage("ar")}){AdminText("العربية")};TextButton(onClick={onLanguage("en")}){AdminText("English")}}
  if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Violet)
  if(s.error.isNotEmpty())Glass("Request failed"){AdminText(s.error,color=MaterialTheme.colorScheme.error)}
  if(s.message.isNotEmpty())Glass{AdminText(s.message)}
  if(!s.ready){
   Spacer(Modifier.height(24.dp));Brand(true,true)
   if(s.savedSession)Glass("Unlock on this phone"){
    AdminText("Sign in once with your authorized owner account. Later, use your phone unlock without typing a password.")
    Action("UNLOCK WITH PHONE",!s.busy){unlock()}
    TextButton(onClick={model.logout()}){AdminText("Use another account")}
   }else Glass("Administrator sign in"){
    AdminText("Sign in once with your authorized owner account. Later, use your phone unlock without typing a password.",color=Violet)
    Field("Email",email,{email=it});Field("Password",password,{password=it},true)
    Action("SIGN IN",!s.busy){model.login(email,password)}
    TextButton(onClick={model.reset(email)},enabled=!s.busy){AdminText("Reset password")}
   }
   AdminText("Private management • Secure licenses • B5 STUDIOS",color=Gold);Skyline()
  }
  else{Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={scope.launch{drawer.open()}}){Icon(Icons.Outlined.Menu,contentDescription="Menu",tint=Gold)};Box(Modifier.weight(1f)){Brand()};TextButton(onClick={model.refresh()},enabled=!s.busy){AdminText("Refresh")};TextButton(onClick={model.logout()}){AdminText("Sign out")}}
   when(page){
    0->OrganizedDashboard(s,{page=it},{manageTab=2;page=3})
    1->LicensePage(s,model)
    2->VpnSettingsPage(s,model)
    3->{PageTitle("Management studio");Row(Modifier.horizontalScroll(rememberScrollState())){listOf("App information","Client sections","Admin layout","App updates","Package library","Service help").forEachIndexed{i,label->FilterChip(selected=manageTab==i,onClick={manageTab=i},label={AdminText(label)},modifier=Modifier.padding(end=7.dp))}};when(manageTab){0->SettingsPage(s,model);1->SectionEditor(s,model);2->AdminLayoutEditor(s,model);3->UpdateEditor(s,model);4->PackageLibrary(s,model);5->SparkSetupCard()}}
    4->SocialPage(s,model)
   }
  };Spacer(Modifier.height(24.dp))
 }}}
 }
}
@Composable fun LicensePage(s:AdminState,m:AdminModel){
 var days by remember{mutableStateOf("30")};var policy by remember{mutableStateOf("first_activation")};var generated by remember{mutableStateOf("")};var search by remember{mutableStateOf("")};var filter by remember{mutableStateOf("all")};var confirm by remember{mutableStateOf<Map<String,Any?>?>(null)};var extend by remember{mutableStateOf("30")}
 var sessions by remember{mutableStateOf<List<Map<String,Any?>>?>(null)}
 val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current;val context=androidx.compose.ui.platform.LocalContext.current
 PageTitle("License studio");AdminText("Single-use codes • one installation per license",color=Violet)
 if(s.conflicts.isNotEmpty())Glass("Attempts from another installation") { s.conflicts.take(15).forEach { attempt->
 val license=s.licenses.firstOrNull{it.str("id")==attempt.str("license_id")}
 AdminText(license?.str("code")?:attempt.str("license_id").take(12),color=Gold,fontWeight=FontWeight.Bold)
 Fact("Reported phone",attempt.str("device_model"));Fact("Attempted",date(attempt["attempted_at"]))
 AdminText("Second activation was rejected. This is an attempt, not proof of two live VPN sessions.",color=Violet)
 Action("BAN THIS LICENSE",!s.busy){m.act("manageLicense",mapOf("id" to attempt["license_id"],"action" to "revoke"))}
 } }
Glass("Create a subscription"){
  Row(Modifier.horizontalScroll(rememberScrollState())){listOf(1,3,7,15,30,60,90,180,365).forEach{d->FilterChip(selected=days==d.toString(),onClick={days=d.toString()},label={AdminText("${d}d")},modifier=Modifier.padding(end=6.dp))}}
  Field("Duration days · custom supported",days,{days=it.filter(Char::isDigit)})
  Row{RadioButton(policy=="first_activation",{policy="first_activation"});AdminText("Start on activation",Modifier.padding(top=12.dp))};Row{RadioButton(policy=="generation",{policy="generation"});AdminText("Start now",Modifier.padding(top=12.dp))}
  Action("GENERATE LICENSE",!s.busy){m.act("generateLicense",mapOf("duration_days" to days.toIntOrNull(),"activation_policy" to policy)){generated=it.str("code")}}
  if(generated.isNotBlank()){AdminText(generated,color=Gold,fontWeight=FontWeight.Bold);Action("COPY CODE"){clipboard.setText(androidx.compose.ui.text.AnnotatedString(generated))};Action("SHARE VIA WHATSAPP"){val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,generated);setPackage("com.whatsapp")};runCatching{context.startActivity(intent)}.onFailure{context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,generated)},"Share license"))}}}
 }
 Field("Search loaded licenses",search,{search=it});Action("SEARCH ENTIRE INVENTORY BY FULL CODE",!s.busy&&search.length==13){m.searchAll(search)};Row(Modifier.horizontalScroll(rememberScrollState())){listOf("all","active","expired","revoked","unused").forEach{v->FilterChip(selected=filter==v,onClick={filter=v},label={AdminText(v)},modifier=Modifier.padding(end=6.dp))}}
 AdminText("${s.licenses.size} loaded licenses • refresh for current status",color=Violet);if(s.cursor.isNotEmpty()&&s.cursor!="null")Action("LOAD MORE LICENSES",!s.busy){m.more()}
 s.licenses.filter{(filter=="all"||it.str("status")==filter)&&it.str("code").contains(search,true)}.forEach{l->Glass(l.str("code")){Fact("Status",l.str("status"));Fact("Activated",date(l["activated_at"]));Fact("Expires",date(l["expires_at"]));Fact("Device bound",if(l["device_binding_id"]!=null)"Yes" else "No");Action("COPY"){clipboard.setText(androidx.compose.ui.text.AnnotatedString(l.str("code")))};Row{IconAction("Revoke license",!s.busy){m.act("manageLicense",mapOf("id" to l["id"],"action" to "revoke"))};IconAction("Reactivate license",!s.busy){m.act("manageLicense",mapOf("id" to l["id"],"action" to "reactivate"))}};Field("Extend by days",extend,{extend=it.filter(Char::isDigit)});Action("EXTEND",!s.busy){m.act("manageLicense",mapOf("id" to l["id"],"action" to "extend","days" to extend.toIntOrNull()))};Action("VIEW DEVICE / ATTEMPTS",!s.busy){m.work{sessions=m.api.call("listSessions",mapOf("license_id" to l["id"]))["items"].rows()}};TextButton(onClick={confirm=l+mapOf("action" to "reset")}){AdminText("Reset device binding")};IconAction("Delete license"){confirm=l+mapOf("action" to "delete")}}}
 sessions?.let{list->AlertDialog(onDismissRequest={sessions=null},title={AdminText("Authorized sessions")},text={Column(Modifier.verticalScroll(rememberScrollState())){if(list.isEmpty())AdminText("No sessions");list.forEach{AdminText("${if(it.str("kind")=="other_installation")"REJECTED ACTIVATION" else "BOUND DEVICE"} • ${it.str("device_model")}\n${it.str("uid")}\n${date(it["attempted_at"]?:it["last_seen"])}",color=if(it.str("kind")=="other_installation")Violet else Gold)}}},confirmButton={TextButton(onClick={sessions=null}){AdminText("Close")}})}
 confirm?.let{c->AlertDialog(onDismissRequest={confirm=null},title={AdminText("Confirm ${c.str("action")}")},text={AdminText("${c.str("code")} • Existing sessions will lose authorization.")},confirmButton={TextButton(onClick={m.act("manageLicense",mapOf("id" to c["id"],"action" to c["action"]));confirm=null}){AdminText("Confirm")}},dismissButton={TextButton(onClick={confirm=null}){AdminText("Cancel")}})}
}
@Composable fun VpnSettingsPage(s:AdminState,m:AdminModel){
 val provider=ProviderSettings.from(s.provider)
 val draft=s.providerRequest.ifEmpty{s.provider}
 var carrier by remember(draft){mutableStateOf(draft.str("carrier_id"))}
 var address by remember(draft){mutableStateOf(draft.str("base_url"))}
 var enabled by remember(s.provider){mutableStateOf(provider.enabledLocations)}
 var advanced by remember{mutableStateOf(false)}
 val saved=draft.str("carrier_id").isNotBlank()&&draft.str("base_url").isNotBlank()
 val detailsChanged=provider.connected&&saved&&(draft.str("carrier_id")!=provider.carrierId||draft.str("base_url").trimEnd('/')!=provider.baseUrl.trimEnd('/'))
 PageTitle("VPN Connection Settings")
 Glass("Service status"){
  Fact("Provider","Aura Partner SDK")
  Fact("Connection details",if(saved)"Saved"else "Not added")
  Fact("Provider setup",if(provider.connected)"Configured"else "Pending integration")
  Fact("Expiry protection",if(s.provider["expiry_enforcement_verified"]==true)"Verified"else "Pending")
  if(detailsChanged)AdminText("New connection details are saved for integration. The current verified service remains in use until the new details are approved.",color=Gold)
  if(!provider.connected)AdminText("Provider not connected. Saved details become usable after your authorized provider and client app integration are prepared. Saving this form does not create a VPN connection.",color=Gold)
  Action("REFRESH SERVICE STATUS",!s.busy){m.refresh()}
 }
 TextButton(onClick={advanced=!advanced}){AdminText("Provider integration details")}
 if(advanced)Glass("Add provider connection"){
  AdminText("Enter the public details supplied with your authorized partner account.",color=Violet)
  Field("Public partner project ID",carrier,{carrier=it})
  Field("Provider HTTPS backend address",address,{address=it})
  AdminText("Public connection details only. Keep provider passwords and master API keys with your service operator.",fontSize=12.sp,color=Violet)
  Action("SAVE CONNECTION DETAILS",!s.busy&&carrier.isNotBlank()&&address.isNotBlank()){m.act("saveProviderConnectionDraft",mapOf("carrier_id" to carrier,"base_url" to address))}
 }
 Glass("Customer connections"){
  val paused=s.settings["maintenance"]==true
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){AdminText("Allow customer connections",fontWeight=FontWeight.Bold);AdminText(if(paused)"Paused"else if(provider.connected)"Allowed"else "Waiting for provider",color=if(paused)Gold else HackerGreen)}
   Switch(!paused,{allowed->m.act("setVpnServicePaused",mapOf("paused" to !allowed))},enabled=!s.busy)
  }
  AdminText("Allowing connections takes effect when your provider service is ready.",fontSize=12.sp,color=Violet)
 }
 Glass("USA server locations"){
  if(!provider.connected||provider.locations.isEmpty())AdminText("Supported USA locations appear here after provider integration.",color=Gold)
  if(provider.connected)provider.locations.filter{it["country_code"]=="US"}.forEach{location->
   val id=location.str("id")
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){AdminText("🇺🇸 ${location.str("name")}",fontWeight=FontWeight.Bold);AdminText(location.str("city"),color=Violet)}
    Switch(id in enabled,{checked->enabled=if(checked)enabled+id else enabled-id},enabled=!s.busy)
   }
  }
  Fact("Per-app routing",if(provider.connected&&provider.perApp)"Supported"else "Unavailable")
  Action("SAVE LOCATIONS",!s.busy&&provider.connected){m.act("saveProviderLocations",mapOf("enabled_locations" to enabled.toList()))}
 }
}
@Composable fun SettingsPage(s:AdminState,m:AdminModel){var fields by remember(s.settings){mutableStateOf(s.settings.mapValues{it.value.toString()})};PageTitle("App management");Glass("Remote client settings"){
 listOf("welcome","announcement","maintenance_message","default_server","server_order","support","privacy_url","terms_url","help","activation_popup","minimum_version","update_notice").forEach{k->Field(k.replace('_',' '),fields[k]?:if(k=="minimum_version")"1" else "",{fields=fields+(k to it)})};listOf("maintenance","show_social","show_tips").forEach{k->Row(verticalAlignment=Alignment.CenterVertically){Switch(fields[k]=="true",{fields=fields+(k to it.toString())});AdminText(k)}}
 Action("PUBLISH SETTINGS",!s.busy){m.act("saveConfiguration",mapOf("settings" to fields.mapValues{(k,v)->if(k=="minimum_version")v.toIntOrNull()else if(k in listOf("maintenance","show_social","show_tips"))v=="true" else v}))}
}}
@Composable fun SocialPage(s:AdminState,m:AdminModel){var items by remember(s.social){mutableStateOf(s.social)};PageTitle("Social links");AdminText("HTTPS or support email • order controls client display",color=Violet)
 items.forEachIndexed{i,x->Glass(x.str("name").ifEmpty{"New link"}){listOf("name","icon","url").forEach{k->Field(k,x.str(k),{v->items=items.toMutableList().also{it[i]=x+(k to v)}})};Row(verticalAlignment=Alignment.CenterVertically){Switch(x["enabled"]==true,{v->items=items.toMutableList().also{it[i]=x+("enabled" to v)}});TextButton(onClick={if(i>0)items=items.toMutableList().also{val a=it[i-1];it[i-1]=it[i];it[i]=a}},enabled=i>0){AdminText("Move up")};TextButton(onClick={items=items.filterIndexed{n,_->n!=i}}){AdminText("Remove")}}}}
 Action("ADD LINK"){items=items+mapOf("name" to "","icon" to "whatsapp","url" to "","enabled" to true)};AdminText("Icons: whatsapp, telegram, instagram, tiktok, facebook, youtube, website, email, custom",color=Violet);Action("PUBLISH SOCIAL LINKS",!s.busy){m.act("saveSocial",mapOf("items" to items))}
}

@Composable private fun OrganizedDashboard(s:AdminState,navigate:(Int)->Unit,customize:()->Unit){
 val enabled=(s.layout["enabled_panels"] as? List<*>)?.map{it.toString()}?:listOf("overview","license_health","security_alerts","infrastructure","quick_actions")
 PageTitle("Control center","B5 STUDIOS • PRIVATE MANAGEMENT")
 if(s.stats.isEmpty())SparkSetupCard()
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){AdminText("Secure online license management",color=Violet,fontSize=10.sp);TextButton(onClick=customize){AdminText("Customize")}}
 if("overview" in enabled){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){MetricTile("Total licenses",s.stats["total"]?.toString()?:"—",Gold,Modifier.weight(1f)){navigate(1)};MetricTile("Active",s.stats["active"]?.toString()?:"—",androidx.compose.ui.graphics.Color(0xFF65EBA4),Modifier.weight(1f)){navigate(1)}}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){MetricTile("Expired",s.stats["expired"]?.toString()?:"—",Violet,Modifier.weight(1f)){navigate(1)};MetricTile("Banned",s.stats["revoked"]?.toString()?:"—",androidx.compose.ui.graphics.Color(0xFFFF8192),Modifier.weight(1f)){navigate(1)}}
 }
 if("license_health" in enabled)Glass("Subscription overview"){Fact("Unused codes",s.stats.str("unused").ifBlank{"—"});Fact("Activated in last 7 days",s.stats.str("recent").ifBlank{"—"});Action("OPEN LICENSE STUDIO"){navigate(1)}}
 if("security_alerts" in enabled)Glass("Device protection"){Fact("Recorded conflicts (latest 100)",s.conflicts.size.toString());AdminText("One code is bound to one installation. Another installation is refused; you choose whether to ban it.",color=androidx.compose.ui.graphics.Color(0xFFBDB9D3));Action("REVIEW DEVICE ATTEMPTS"){navigate(1)}}
 if("infrastructure" in enabled)Glass("VPN Connection Settings"){Fact("Provider",if(s.stats["provider_connected"]==true)"Configured" else "Not connected");Fact("Enabled USA locations",s.stats.str("enabled_locations").ifBlank{"0"});Action("VPN SETTINGS"){navigate(2)}}
 if("quick_actions" in enabled)Glass("Management workspace"){Action("CLIENT INFORMATION & SECTIONS"){navigate(3)};Action("SOCIAL LINKS & SUPPORT"){navigate(4)}}
 val context=androidx.compose.ui.platform.LocalContext.current
 s.layout["custom_cards"].rows().filter{it["enabled"]==true}.forEach{x->Glass(x.str("title")){AdminText(x.str("body"));if(x.str("url").isNotBlank())Action("OPEN LINK"){runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(x.str("url"))))}}}}
}
@Composable private fun MetricTile(label:String,value:String,color:androidx.compose.ui.graphics.Color,modifier:Modifier,click:()->Unit){
 Box(modifier.clickable(onClick=click)){Glass{AdminText(label,color=androidx.compose.ui.graphics.Color(0xFFB9BDD1),fontSize=12.sp);Spacer(Modifier.height(10.dp));AdminText(value,color=color,fontSize=30.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp));Box(Modifier.height(2.dp).fillMaxWidth().background(color.copy(alpha=.5f)))}}
}
@Composable fun SectionEditor(s:AdminState,m:AdminModel){
 var items by remember(s.sections){mutableStateOf(s.sections)}
 Glass("Client sections"){AdminText("Create cards, native actions and isolated HTML/CSS/JavaScript tools. Changes arrive when the client refreshes.",color=Violet)}
 items.forEachIndexed{i,x->SectionFields(x,i,items.size,{changed->items=items.toMutableList().also{it[i]=changed}},{items=items.filterIndexed{n,_->n!=i}},{if(i>0)items=items.toMutableList().also{val previous=it[i-1];it[i-1]=it[i];it[i]=previous}})}
 Action("ADD CLIENT SECTION",items.size<20){items=items+newSection()};Action("PUBLISH SECTIONS",!s.busy){m.act("saveSections",mapOf("items" to items))}
}
private fun newSection()=mapOf<String,Any?>("id" to java.util.UUID.randomUUID().toString(),"title" to "","body" to "","url" to "","icon" to "info","target" to "home","enabled" to true,"kind" to "card","action" to "none","button_label" to "")
@Composable private fun SectionFields(x:Map<String,Any?>,index:Int,total:Int,change:(Map<String,Any?>)->Unit,remove:()->Unit,up:()->Unit,showTargets:Boolean=true){
 Glass(x.str("title").ifBlank{"New section ${index+1}"}){
  Field("Title",x.str("title"),{change(x+("title" to it))});Field("Body text",x.str("body"),{change(x+("body" to it))});Field("Optional HTTPS / email URL",x.str("url"),{change(x+("url" to it))})
  if(showTargets){
   AdminText("Section type",color=Gold)
   Row{FilterChip(x.str("kind")!="code",{change(x+("kind" to "card"))},label={AdminText("Information card")});Spacer(Modifier.width(8.dp));FilterChip(x.str("kind")=="code",{change(x+("kind" to "code"))},label={AdminText("Code tool")})}
   if(x.str("kind")=="code"){
    AdminText("Code tools run without network, device permissions, Firebase credentials or a native bridge.",fontSize=12.sp,color=Violet)
    CodeField("HTML code",x.str("html")){change(x+("html" to it))}
    CodeField("CSS design",x.str("css")){change(x+("css" to it))}
    CodeField("JavaScript behavior",x.str("javascript")){change(x+("javascript" to it))}
    Action("LOAD EXAMPLE TOOL"){change(x+mapOf("html" to "<h2>My calculator</h2><input id='a' type='number' placeholder='First number'><input id='b' type='number' placeholder='Second number'><button onclick='calculate()'>Add</button><p id='result'></p>","css" to "input,button{display:block;width:90%;margin:12px 0;} h2{color:#67ffa8}","javascript" to "function calculate(){document.getElementById('result').textContent=Number(document.getElementById('a').value)+Number(document.getElementById('b').value);}"))}
   }else{
    AdminText("Button action",color=Gold)
    Row(Modifier.horizontalScroll(rememberScrollState())){SectionPolicy.actions.forEach{action->FilterChip(SectionPolicy.action(x)==action,{change(x+("action" to action))},label={AdminText(action)},modifier=Modifier.padding(end=6.dp))}}
   }
   Field("Button label",x.str("button_label"),{change(x+("button_label" to it))})
  }
  Row(Modifier.horizontalScroll(rememberScrollState())){listOf("info","support","shield","globe","message").forEach{icon->FilterChip(x.str("icon")==icon,{change(x+("icon" to icon))},label={AdminText(icon)},modifier=Modifier.padding(end=6.dp))}}
  if(showTargets)Row{listOf("home","settings").forEach{target->FilterChip(x.str("target")==target,{change(x+("target" to target))},label={AdminText(target)},modifier=Modifier.padding(end=8.dp))}}
  Row(verticalAlignment=Alignment.CenterVertically){Switch(x["enabled"]==true,{change(x+("enabled" to it))});AdminText(if(x["enabled"]==true)"Visible" else "Hidden");Spacer(Modifier.weight(1f));TextButton(onClick=up,enabled=index>0){AdminText("↑")};TextButton(onClick=remove){AdminText("Remove",color=MaterialTheme.colorScheme.error)}}
 }
}
@Composable private fun AdminLayoutEditor(s:AdminState,m:AdminModel){
 val panels=listOf("overview","license_health","security_alerts","infrastructure","quick_actions")
 var enabled by remember(s.layout){mutableStateOf((s.layout["enabled_panels"] as? List<*>)?.map{it.toString()}?:panels)}
 var cards by remember(s.layout){mutableStateOf(s.layout["custom_cards"].rows())}
 Glass("Organize the Admin dashboard"){
  panels.forEach{panel->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Switch(panel in enabled,{checked->enabled=if(checked)enabled+panel else enabled-panel});AdminText(panel.replace('_',' '),Modifier.padding(start=10.dp))}}
  AdminText("Navigation and license authorization remain available even when a dashboard panel is hidden.",color=Violet)
 }
 cards.forEachIndexed{i,x->SectionFields(x+mapOf("target" to "home"),i,cards.size,{value->cards=cards.toMutableList().also{it[i]=value}},{cards=cards.filterIndexed{n,_->n!=i}},{if(i>0)cards=cards.toMutableList().also{val a=it[i-1];it[i-1]=it[i];it[i]=a}},showTargets=false)}
 Action("ADD PRIVATE ADMIN CARD",cards.size<20){cards=cards+newSection()};Action("SAVE DASHBOARD LAYOUT",!s.busy){m.act("saveAdminLayout",mapOf("enabled_panels" to enabled,"custom_cards" to cards))}
}

@Composable private fun SparkSetupCard(){
 Glass("Service help"){
  AdminText("Manage licenses, provider connection details, USA locations, announcements and social links from this Admin app.",color=androidx.compose.ui.graphics.Color(0xFFD9D7E9),fontSize=13.sp)
  AdminText("Your licensing service and authorized VPN provider must be prepared once before customers can activate and connect. The customer app must include the provider's supported VPN integration.",color=Gold,fontSize=12.sp)
  AdminText("If sign-in, saving or activation is unavailable, the service is not fully prepared. Contact your service operator; no server or database console is part of everyday Admin use.",color=Violet,fontSize=12.sp)
 }
}

@Composable private fun CodeField(label:String,value:String,change:(String)->Unit){
 OutlinedTextField(value,onValueChange=change,label={AdminText(label)},modifier=Modifier.fillMaxWidth().padding(vertical=6.dp).heightIn(min=140.dp),minLines=4,maxLines=12,textStyle=androidx.compose.ui.text.TextStyle(fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace,fontSize=13.sp),shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
}
@Composable fun UpdateEditor(s:AdminState,m:AdminModel){
 val update=AppUpdate.from(s.appUpdate)
 var latest by remember(s.appUpdate){mutableStateOf(if(s.appUpdate.isEmpty())(BuildConfig.VERSION_CODE+1).toString()else update.latest.toString())}
 var minimum by remember(s.appUpdate){mutableStateOf(update.minimum.toString())}
 var url by remember(s.appUpdate){mutableStateOf(update.url)};var checksum by remember(s.appUpdate){mutableStateOf(update.sha256)};var message by remember(s.appUpdate){mutableStateOf(update.message)}
 var enabled by remember(s.appUpdate){mutableStateOf(update.enabled)};var required by remember(s.appUpdate){mutableStateOf(update.required)}
 PageTitle("App updates")
 Glass("App updates"){
  AdminText("Use a newer APK signed with the same key. Android asks for installation confirmation and then offers Open.",color=Violet)
  Field("Latest version code",latest,{latest=it.filter(Char::isDigit)});Field("Minimum version code",minimum,{minimum=it.filter(Char::isDigit)})
  Field("APK HTTPS download link",url,{url=it});Field("APK SHA-256 checksum",checksum,{checksum=it.trim()});Field("Update message",message,{message=it})
  Row(verticalAlignment=Alignment.CenterVertically){Switch(enabled,{enabled=it});AdminText("Enable update notice")}
  Row(verticalAlignment=Alignment.CenterVertically){Switch(required,{required=it});AdminText("Require update")}
  Action("PUBLISH UPDATE",!s.busy){m.act("saveAppUpdate",mapOf("enabled" to enabled,"required" to required,"latest_version_code" to latest.toLongOrNull(),"minimum_version" to minimum.toLongOrNull(),"apk_url" to url.trim(),"apk_sha256" to checksum,"message" to message))}
 }
}

@Composable fun PackageLibrary(s:AdminState,m:AdminModel){
 val context=androidx.compose.ui.platform.LocalContext.current
 var pending by remember{mutableStateOf<Pair<String,Map<String,Any?>>?>(null)}
 var label by remember{mutableStateOf("")}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)m.work{
  val result=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){
   val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBounded(524289)}?:error("Cannot read file")
   require(bytes.size<=524288){"Maximum package size is 512 KiB"}
   val json=JSONObject(String(bytes,Charsets.UTF_8))
   fun value(v:Any?):Any?=when(v){JSONObject.NULL->null;is JSONObject->v.keys().asSequence().associateWith{value(v.get(it))};is JSONArray->(0 until v.length()).map{value(v.get(it))};else->v}
   StudioPackage.validate(value(json).obj())
  }
  pending=result;label=result.first
 }}
 PageTitle("Package library")
 Glass("Import a package"){
  AdminText("Import section tools, supported provider details or update settings. Review the package before publishing.",color=Violet)
  Action("CHOOSE JSON PACKAGE",!s.busy){picker.launch(arrayOf("application/json","text/plain","application/octet-stream"))}
  if(pending!=null){Fact("Package action",label);AdminText("The package is validated. Publishing replaces the matching configuration.",color=Gold);Action("PUBLISH PACKAGE",!s.busy){val item=pending!!;m.act(item.first,item.second){pending=null;label=""}}}
 }
}
