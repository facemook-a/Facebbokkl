package com.usafavor.client

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b5studios.shared.*
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

private fun englishContext(base:Context):Context {
 val config=Configuration(base.resources.configuration)
 config.setLocales(LocaleList(Locale.ENGLISH));config.setLayoutDirection(Locale.ENGLISH)
 return base.createConfigurationContext(config)
}
class FavorApplication:Application(){
 override fun attachBaseContext(base:Context){super.attachBaseContext(englishContext(base))}
 override fun onCreate(){
  Locale.setDefault(Locale.ENGLISH)
  if(android.os.Build.VERSION.SDK_INT>=33){val manager=getSystemService(android.app.LocaleManager::class.java);if(manager.applicationLocales.toLanguageTags()!="en")manager.applicationLocales=LocaleList(Locale.ENGLISH)}
  super.onCreate();initializeFirebase(this,BuildConfig.DEBUG)
 }
}
data class ClientState(val initialized:Boolean=false,val licensed:Boolean=false,val busy:Boolean=false,val error:String="",val status:String="Disconnected",val license:Map<String,Any?> = emptyMap(),val servers:List<Map<String,Any?>> = emptyList(),val settings:Map<String,Any?> = emptyMap(),val social:List<Map<String,Any?>> = emptyList(),val serverId:String="",val ip:String="",val started:Long=0,val lastConnection:Long=0,val sessionId:String="",val tips:Boolean=false,val sections:List<Map<String,Any?>> = emptyList(),val received:Long=0,val sent:Long=0,val provider:Map<String,Any?> = emptyMap(),val selectedApps:Set<String> = emptySet(),val appUpdate:Map<String,Any?> = emptyMap(),val updateProgress:Int=0,val updateDownloading:Boolean=false,val updateFile:String="",val updateDismissedVersion:Long=0,val activationSuccess:Boolean=false)
class ClientModel(app:Application):AndroidViewModel(app){
 val api=Backend(app);val store=SecureStore(app);private val _state=MutableStateFlow(ClientState());val state=_state.asStateFlow();val tunnel=providerTunnel(app,::engineStatus)
 val updater=AppUpdater(app)
 private var connectGeneration=0
 private var licenseWatch:com.google.firebase.firestore.ListenerRegistration?=null
 init{
  val cached=runCatching{JSONObject(store.read()?:"{}")}.getOrDefault(JSONObject())
  val savedApps=cached.optJSONArray("selected_apps")
  _state.value=_state.value.copy(lastConnection=cached.optLong("last_connection"),selectedApps=(0 until (savedApps?.length()?:0)).map{savedApps!!.getString(it)}.toSet())
  viewModelScope.launch{try{api.anonymous();runCatching{checkUpdates()};reload()}catch(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"Initialization failed")}finally{_state.value=_state.value.copy(initialized=true)}}
  viewModelScope.launch{while(isActive){delay(30000);runCatching{checkUpdates()};if(_state.value.licensed)try{reload()}catch(e:Exception){_state.value=_state.value.copy(licensed=false,error=e.localizedMessage?:"License refresh failed");disconnect()}}}
 }
 fun work(block:suspend ()->Unit){if(_state.value.busy)return;viewModelScope.launch{_state.value=_state.value.copy(busy=true,error="");try{block()}catch(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"Operation failed")}finally{_state.value=_state.value.copy(busy=false)}}}
 private suspend fun reload(){
  val l=try{api.call("licenseInfo")}catch(_:LicenseNotActivated){return}
  val c=api.call("getConfiguration");val settings=c["settings"].obj();val provider=c["provider"].obj();val connection=ProviderSettings.from(provider)
  val servers=if(connection.connected)connection.available.map{it+mapOf("country" to "United States","flag" to "🇺🇸")}else emptyList()
  val selected=_state.value.serverId.takeIf{id->servers.any{it.str("id")==id}}?:servers.firstOrNull()?.str("id").orEmpty()
  val previous=_state.value.serverId
  _state.value=_state.value.copy(licensed=true,license=l,servers=servers,settings=settings,social=c["social"].rows(),sections=c["sections"].rows(),serverId=selected,provider=provider)
  if(_state.value.status in listOf("Connected","Connecting")&&(!connection.connected||servers.none{it.str("id")==previous}||settings["maintenance"]==true))disconnect()
  persist()
  if(licenseWatch==null)licenseWatch=api.watchLicense{reason->viewModelScope.launch{_state.value=_state.value.copy(licensed=false,error=reason);disconnect()}}
 }
 private fun persist(){val s=_state.value;store.save(JSONObject(s.license+mapOf("last_connection" to s.lastConnection,"selected_apps" to org.json.JSONArray(s.selectedApps.toList()))).toString())}
 fun activate(code:String)=work{api.anonymous();val l=api.call("activateLicense",mapOf("code" to code));_state.value=_state.value.copy(licensed=true,license=l,activationSuccess=true,tips=false);reload()}
 private suspend fun checkUpdates(){
  val update=api.call("getAppUpdate")["update"].obj()
  _state.update{old->old.copy(appUpdate=update,updateFile=old.updateFile.takeIf{old.appUpdate["apk_sha256"]==update["apk_sha256"]&&old.appUpdate["latest_version_code"]==update["latest_version_code"]}?:"")}
 }
 fun refresh()=work{checkUpdates();reload()}
 fun dismissUpdate(){_state.value=_state.value.copy(updateDismissedVersion=AppUpdate.from(_state.value.appUpdate).latest)}
 fun downloadUpdate()=work{
  val update=AppUpdate.from(_state.value.appUpdate)
  check(update.offered(BuildConfig.VERSION_CODE.toLong())){"No newer update is published"}
  _state.value=_state.value.copy(updateDownloading=true,updateProgress=0,updateFile="")
  try{
   val file=updater.download(update){progress->_state.update{it.copy(updateProgress=progress)}}
   check(AppUpdate.from(_state.value.appUpdate).let{it.latest==update.latest&&it.sha256==update.sha256}){"The published update changed. Download the current version again."}
   _state.value=_state.value.copy(updateFile=file.absolutePath)
  }finally{_state.value=_state.value.copy(updateDownloading=false)}
 }
 fun select(id:String){if(_state.value.status in listOf("Disconnected","Error"))_state.value=_state.value.copy(serverId=id)}
 fun apps(packages:Set<String>){check(_state.value.status in listOf("Disconnected","Error")){"Disconnect before changing apps"};_state.value=_state.value.copy(selectedApps=packages);persist()}
 fun connect()=work{
  check(_state.value.status in listOf("Disconnected","Error")){"Disconnect before switching locations"}
  reload()
  val s=_state.value;val provider=ProviderSettings.from(s.provider)
  check(s.licensed){"Activate your license first"}
  check(!AppUpdate.from(s.appUpdate).mandatory(BuildConfig.VERSION_CODE.toLong())){"Install the required app update before connecting"}
  check(provider.connected&&tunnel.available){"Live VPN service requires connecting an authorized provider and its Android SDK."}
  check(s.settings["maintenance"]!=true){s.settings.str("maintenance_message").ifBlank{"VPN service is under maintenance"}}
  check((s.settings["minimum_version"] as? Number)?.toInt()?.let{it<=BuildConfig.VERSION_CODE}!=false){"Update the app before connecting"}
  check(provider.available.any{it.str("id")==s.serverId}){"Select an enabled USA location"}
  check(s.selectedApps.isEmpty()||provider.perApp){"Your provider does not support per-app routing. Choose all apps."}
  val generation=++connectGeneration
  _state.value=s.copy(status="Connecting",ip="")
  try{
   val result=FirebaseFunctions.getInstance().getHttpsCallable("issueProviderGrant").call(mapOf("location_id" to s.serverId)).await().data.obj()
   if(generation!=connectGeneration)return@work
   tunnel.connect(s.provider,s.serverId,result.str("grant"),s.selectedApps)
  }catch(e:Exception){tunnel.disconnect();_state.value=_state.value.copy(status="Error");throw e}
 }
 private fun engineStatus(status:String){viewModelScope.launch{
  val now=System.currentTimeMillis()
  _state.value=_state.value.copy(status=status,ip="",started=if(status=="Connected")now else 0,lastConnection=if(status=="Connected")now else _state.value.lastConnection)
  persist()
 }}
 fun disconnect(){connectGeneration++;tunnel.disconnect();_state.value=_state.value.copy(status="Disconnected",ip="",started=0)}
 fun activationDone(){_state.value=_state.value.copy(activationSuccess=false,tips=!store.tipsDismissed())}
 fun tips(show:Boolean){_state.value=_state.value.copy(tips=show)}
 fun dismiss(){store.dismissTips();tips(false)}
 fun permissionError(){_state.value=_state.value.copy(error="VPN permission was not granted")}
 fun localError(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"VPN service unavailable")}
 override fun onCleared(){licenseWatch?.remove();tunnel.disconnect();tunnel.close();super.onCleared()}
}
class MainActivity:ComponentActivity(){override fun attachBaseContext(base:Context){super.attachBaseContext(englishContext(base))};override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr){FavorTheme{ClientScreen()}}}}}
@Composable fun ClientScreen(m:ClientModel=viewModel()){
 val s by m.state.collectAsStateWithLifecycle();var tab by remember{mutableIntStateOf(0)};var code by remember{mutableStateOf("")};var requestedApps by remember{mutableStateOf(false)};var requestedUpdate by remember{mutableStateOf(false)};val context=androidx.compose.ui.platform.LocalContext.current
 val installPermission=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){
  if(m.updater.mayInstall()&&s.updateFile.isNotBlank())runCatching{context.startActivity(m.updater.installer(java.io.File(s.updateFile)))}.onFailure{m.localError(Exception("Unable to open the Android installer"))}
 }
 fun installUpdate(){try{if(m.updater.mayInstall())context.startActivity(m.updater.installer(java.io.File(s.updateFile)))else installPermission.launch(m.updater.permissionIntent())}catch(e:Exception){m.localError(e)}}
 val vpnPermission=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){if(it.resultCode==android.app.Activity.RESULT_OK)m.connect()else m.permissionError()}
 val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
 fun connect(){try{
  if(!m.tunnel.available||!ProviderSettings.from(s.provider).connected){m.localError(Exception("Live VPN service requires connecting an authorized provider and its Android SDK."));return}
  if(android.os.Build.VERSION.SDK_INT>=33)notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
  val permission=android.net.VpnService.prepare(context)
  if(permission!=null)vpnPermission.launch(permission)else m.connect()
 }catch(e:Exception){m.localError(e)}}
 fun open(url:String){runCatching{val u=Uri.parse(url);require(u.scheme in listOf("https","mailto"));context.startActivity(Intent(Intent.ACTION_VIEW,u))}.onFailure{m.localError(Exception("No application can open this link"))}}
 fun perform(action:String,item:Map<String,Any?>){when(action){
  "license"->tab=2;"activity"->tab=1;"settings"->tab=3;"refresh"->m.refresh()
  "connect"->connect();"disconnect"->m.disconnect();"select_apps"->requestedApps=true;"update"->requestedUpdate=true
  "code"->context.startActivity(Intent(context,CodeSectionActivity::class.java).putExtra("section_id",item.str("id")))
 }}
 Backdrop{Scaffold(containerColor=androidx.compose.ui.graphics.Color.Transparent,bottomBar={if(s.licensed)PremiumNav(tab){tab=it}}){padding->Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal=22.dp),horizontalAlignment=Alignment.CenterHorizontally){
 if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Violet)
 if(s.error.isNotEmpty())Glass("Action required"){Text(s.error,color=MaterialTheme.colorScheme.error)}
 if(!s.initialized){Spacer(Modifier.height(50.dp));Brand(large=true);AnimatedSecureBoot();LinearProgressIndicator(color=Violet,modifier=Modifier.padding(24.dp));Skyline()}
 else if(!s.licensed){Spacer(Modifier.height(20.dp));Brand(large=true);Shield();Glass("Activate your access"){Field("USA-XXXX-XXXX",code,{code=it.uppercase().take(13)});Action("ACTIVATE LICENSE",!s.busy&&code.length==13){m.activate(code)}};Text("One code • one installation. Activation is checked online; a second installation is rejected.",color=Violet);Skyline()}
 else{Brand();when(tab){
  0->{
   Text("PRIVATE CONNECTION",color=Gold,fontSize=10.sp,letterSpacing=3.sp)
   Text(s.settings.str("welcome").ifBlank{"USA FAVOR VPN"},fontSize=27.sp,fontWeight=FontWeight.Bold,color=androidx.compose.ui.graphics.Color.White,modifier=Modifier.padding(top=7.dp))
   if(s.settings.str("announcement").isNotBlank())Glass{Text(s.settings.str("announcement"))}
   if(s.settings["maintenance"]==true)Glass("Maintenance"){Text(s.settings.str("maintenance_message"))}
   CyberConnectionControl(s.status){if(s.status in listOf("Disconnected","Error"))connect()else m.disconnect()}
   val stateColor=if(s.status=="Connected")androidx.compose.ui.graphics.Color(0xFF4EFF9A)else if(s.status=="Error")MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color(0xFFB3C4BF)
   androidx.compose.material3.Surface(color=stateColor.copy(alpha=.12f),shape=androidx.compose.foundation.shape.RoundedCornerShape(30.dp),border=BorderStroke(1.dp,stateColor.copy(alpha=.35f))){Row(Modifier.padding(horizontal=18.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(7.dp).background(stateColor,androidx.compose.foundation.shape.CircleShape));Spacer(Modifier.width(9.dp));Text(s.status,color=stateColor,fontSize=14.sp,fontWeight=FontWeight.Bold)}}
   Text(if(s.status in listOf("Disconnected","Error"))"Tap the power button to connect" else "Tap the power button to disconnect",fontSize=12.sp,color=androidx.compose.ui.graphics.Color(0xFFABA7C4),modifier=Modifier.padding(top=10.dp,bottom=16.dp))
   if(!ProviderSettings.from(s.provider).connected||!m.tunnel.available)Glass("Service unavailable"){
    Text("Your administrator is preparing the VPN service.",color=Gold,fontSize=13.sp)
   }
   AppRoutingSelector(s,m)
   var selectServer by remember{mutableStateOf(false)}
   val chosen=s.servers.firstOrNull{it.str("id")==s.serverId}
   Glass(if(chosen==null)"Your VPN server" else "Selected location"){
    if(chosen==null){Row(verticalAlignment=Alignment.CenterVertically){Shield(Modifier.size(48.dp));Spacer(Modifier.width(12.dp));Column{Text("Choose your location",color=Gold,fontWeight=FontWeight.SemiBold);Text("Your administrator has not enabled a location yet.",fontSize=12.sp,color=androidx.compose.ui.graphics.Color(0xFFB5B3CE))}}}
    else{Row(verticalAlignment=Alignment.CenterVertically){Text(chosen.str("flag").ifBlank{"🇺🇸"},fontSize=32.sp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(chosen.str("name"),fontWeight=FontWeight.Bold,color=androidx.compose.ui.graphics.Color.White);Text("${chosen.str("country")} · ${chosen.str("city")}",color=androidx.compose.ui.graphics.Color(0xFFB5B3CE),fontSize=12.sp)};Text("VPN",color=Cyan,fontSize=10.sp,letterSpacing=1.sp)}
     Action("CHANGE LOCATION",s.status in listOf("Disconnected","Error")){selectServer=true}
    }
   }
   if(selectServer)AlertDialog(onDismissRequest={selectServer=false},title={Text("Choose a location")},text={Column(Modifier.verticalScroll(rememberScrollState())){s.servers.forEach{server->Row(Modifier.fillMaxWidth().clickable{m.select(server.str("id"));selectServer=false}.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(s.serverId==server.str("id"),{m.select(server.str("id"));selectServer=false});Column{Text("${server.str("flag").ifBlank{"🇺🇸"}} ${server.str("name")}",color=Gold);Text(server.str("city"),fontSize=12.sp)}}}}},confirmButton={TextButton(onClick={selectServer=false}){Text("Close")}})
   Glass("Connection details"){
    Fact("Public IP",s.ip.ifBlank{"Not measured"})
    Fact("Service","Encrypted provider VPN")
    var now by remember{mutableLongStateOf(System.currentTimeMillis())};LaunchedEffect(s.started){while(true){delay(1000);now=System.currentTimeMillis()}}
    val seconds=if(s.started>0)kotlin.math.max(0,(now-s.started)/1000)else 0
    Fact("Session time",String.format(java.util.Locale.US,"%02d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60))
    Fact("License expires",date(s.license["expires_at"]))
   }
   if(s.settings["show_social"]!=false)Social(s.social,::open)
  }
  1->{PageTitle("Connection activity");var now by remember{mutableLongStateOf(System.currentTimeMillis())};LaunchedEffect(Unit){while(true){delay(1000);now=System.currentTimeMillis()}};Glass("Live diagnostics"){Fact("Tunnel state",s.status);Fact("Public IP",s.ip.ifBlank{"Not measured"});Fact("Connected duration",if(s.started>0)"${(now-s.started)/1000}s" else "0s");Fact("Last connection",if(s.lastConnection>0)date(s.lastConnection)else "No recorded connection");Text("Tunnel state comes from the provider SDK. Public IP and traffic measurements are not available in this build.",color=Violet)}}
  2->{PageTitle("Your license");Glass("Subscription"){Fact("License",s.license.str("masked"));Fact("Status",s.license.str("status"));Fact("Activated",date(s.license["activated_at"]));Fact("Expires",date(s.license["expires_at"]));val expiry=(s.license["expires_at"] as? Number)?.toLong()?:0;Fact("Days remaining",kotlin.math.max(0,(expiry-System.currentTimeMillis()+86399999)/86400000).toString());Action("REFRESH LICENSE",!s.busy){m.refresh()};Text(s.settings.str("support").ifBlank{"Contact your license seller to renew your subscription."})}}
  3->{PageTitle("Settings");Glass("Connection preferences"){Text("Your authorized provider manages the encrypted VPN network. Choose an enabled USA location on Home.");AppRoutingSelector(s,m);Action("REFRESH CONFIGURATION",!s.busy){m.refresh()};Action("APP UPDATES"){requestedUpdate=true};Action("ANDROID VPN SETTINGS"){context.startActivity(Intent(android.provider.Settings.ACTION_VPN_SETTINGS))};if(s.settings["show_tips"]!=false)Action("USA CONTENT TIPS"){m.tips(true)};Text(s.settings.str("help"));Text(s.settings.str("support"));Fact("App version",BuildConfig.VERSION_NAME);Fact("Minimum supported",s.settings.str("minimum_version").ifBlank{"1"});Text(s.settings.str("update_notice"));if(s.settings.str("privacy_url").isNotBlank())Action("PRIVACY POLICY"){open(s.settings.str("privacy_url"))};if(s.settings.str("terms_url").isNotBlank())Action("TERMS OF SERVICE"){open(s.settings.str("terms_url"))}};if(s.settings["show_social"]!=false)Social(s.social,::open)}
 };if(tab==0)CustomSections(s.sections,"home",::open,::perform);if(tab==3)CustomSections(s.sections,"settings",::open,::perform);Skyline()}
 }}}
 val update=AppUpdate.from(s.appUpdate)
 val offered=update.offered(BuildConfig.VERSION_CODE.toLong())
 val mandatory=update.mandatory(BuildConfig.VERSION_CODE.toLong())
 if(requestedApps)AppPicker(s,m){requestedApps=false}
 if(requestedUpdate||(offered&&(mandatory||s.updateDismissedVersion!=update.latest))){
  AlertDialog(onDismissRequest={if(!mandatory){requestedUpdate=false;m.dismissUpdate()}},title={Text(if(offered)"Update available"else "App updates")},text={Column{
   Text(if(offered)update.message.ifBlank{"A new version of USA FAVOR VPN is available."}else "You are using the latest published version.")
   if(offered)Text("Android will ask you to confirm installation. After installation, tap Open to return. Your activation stays on this installation.",color=Violet)
   if(s.updateDownloading){LinearProgressIndicator(Modifier.fillMaxWidth());Text(if(s.updateProgress>=0)"${s.updateProgress}%"else "Downloading…",color=HackerGreen)}
   if(s.error.isNotBlank())Text(s.error,color=MaterialTheme.colorScheme.error)
  }},confirmButton={TextButton(onClick={if(!offered){requestedUpdate=false;m.dismissUpdate()}else if(s.updateFile.isNotBlank())installUpdate()else m.downloadUpdate()},enabled=!s.busy){Text(if(!offered)"Close"else if(s.updateFile.isNotBlank())"INSTALL UPDATE"else "DOWNLOAD UPDATE")}},dismissButton={if(offered&&!mandatory)TextButton(onClick={requestedUpdate=false;m.dismissUpdate()}){Text("Later")}})
 }
 if(s.activationSuccess)AlertDialog(onDismissRequest={m.activationDone()},title={Text("License activated")},text={Column{Text("Your code is now active on this installation.");Text("Expires: ${date(s.license["expires_at"])}",color=HackerGreen);Text("You can disconnect and reconnect until expiry. VPN service also requires a connected provider.",color=Violet)}},confirmButton={TextButton(onClick={m.activationDone()}){Text("CONTINUE")}})
 if(s.tips&&!s.activationSuccess)AlertDialog(onDismissRequest={m.tips(false)},title={Text("Before you connect")},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState())){Text(s.settings.str("activation_popup").ifBlank{DEFAULT_TIPS})}},confirmButton={TextButton(onClick={m.dismiss()}){Text("Got it")}},dismissButton={TextButton(onClick={m.dismiss()}){Text("Do not show again")}})
}
@Composable fun Social(items:List<Map<String,Any?>>,open:(String)->Unit){items.filter{it["enabled"]==true}.forEach{v->OutlinedButton(onClick={open(v.str("url"))},modifier=Modifier.fillMaxWidth()){SocialBrandIcon(v.str("icon"));Spacer(Modifier.width(10.dp));Text(v.str("name"),color=Cyan)}}}

@Composable private fun AppRoutingSelector(s:ClientState,m:ClientModel){
 val context=androidx.compose.ui.platform.LocalContext.current
 var show by remember{mutableStateOf(false)}
 val supported=ProviderSettings.from(s.provider).perApp&&m.tunnel.available
 Text(if(s.selectedApps.isEmpty())"Routing: all apps" else "Routing: ${s.selectedApps.size} selected apps",color=Violet)
 if(s.selectedApps.isNotEmpty())Action("USE VPN FOR ALL APPS",s.status in listOf("Disconnected","Error")){m.apps(emptySet())}
 Action("SELECT APPS (OPTIONAL)",supported&&s.status in listOf("Disconnected","Error")){show=true}
 if(!supported)Text("App selection becomes available when supported by your connected provider.",fontSize=11.sp,color=Violet)
 if(show)AppPicker(s,m){show=false}
}
@Composable private fun AppPicker(s:ClientState,m:ClientModel,close:()->Unit){
 val context=androidx.compose.ui.platform.LocalContext.current
 if(!ProviderSettings.from(s.provider).perApp||!m.tunnel.available||s.status !in listOf("Disconnected","Error")){
  AlertDialog(onDismissRequest=close,title={Text("App selection unavailable")},text={Text("App routing requires a supported provider and a disconnected VPN.")},confirmButton={TextButton(onClick=close){Text("Close")}});return
 }
  val apps=remember{context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).map{it.activityInfo.applicationInfo}.distinctBy{it.packageName}.filter{it.packageName!=context.packageName}.sortedBy{it.loadLabel(context.packageManager).toString()}}
  var selected by remember{mutableStateOf(s.selectedApps)}
  var search by remember{mutableStateOf("")}
  AlertDialog(onDismissRequest={close()},title={Text("Apps using VPN")},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState())){
   Text("Leave all apps unchecked to protect all apps. Unselected apps use your normal connection when a selection is made.")
   OutlinedTextField(search,{search=it},label={Text("Search apps")},singleLine=true,modifier=Modifier.fillMaxWidth())
   apps.filter{it.loadLabel(context.packageManager).toString().contains(search,true)}.forEach{app->
    Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
     val bitmap=remember(app.packageName){app.loadIcon(context.packageManager).toBitmap(40,40).asImageBitmap()}
     Image(bitmap,contentDescription=null,modifier=Modifier.size(36.dp));Spacer(Modifier.width(10.dp))
     Column(Modifier.weight(1f)){Text(app.loadLabel(context.packageManager).toString(),fontWeight=FontWeight.SemiBold);Text(app.packageName,fontSize=10.sp,color=Violet)}
     Checkbox(app.packageName in selected,{checked->selected=if(checked)selected+app.packageName else selected-app.packageName})
    }
   }
  }},confirmButton={TextButton(onClick={m.apps(selected);close()}){Text("Save")}},dismissButton={TextButton(onClick={close()}){Text("Cancel")}})
}
