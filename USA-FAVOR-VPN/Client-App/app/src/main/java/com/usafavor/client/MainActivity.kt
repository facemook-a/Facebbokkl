package com.usafavor.client

import android.app.Application
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
import androidx.compose.ui.unit.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b5studios.shared.*
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import de.blinkt.openvpn.core.ICSOpenVPNApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class FavorApplication:ICSOpenVPNApplication(){override fun onCreate(){super.onCreate();initializeFirebase(this,BuildConfig.DEBUG)}}
data class ClientState(val initialized:Boolean=false,val licensed:Boolean=false,val busy:Boolean=false,val error:String="",val status:String="Disconnected",val license:Map<String,Any?> = emptyMap(),val servers:List<Map<String,Any?>> = emptyList(),val settings:Map<String,Any?> = emptyMap(),val social:List<Map<String,Any?>> = emptyList(),val serverId:String="",val ip:String="",val started:Long=0,val lastConnection:Long=0,val sessionId:String="",val tips:Boolean=false,val received:Long=0,val sent:Long=0)
class ClientModel(app:Application):AndroidViewModel(app){
 val api=Backend(app);val store=SecureStore(app);private val _state=MutableStateFlow(ClientState());val state=_state.asStateFlow();val tunnel=Tunnel(app,::engineStatus)
 private val byteListener=de.blinkt.openvpn.core.VpnStatus.ByteCountListener{received,sent,_,_->viewModelScope.launch{_state.value=_state.value.copy(received=received,sent=sent)}}
 private var expected="";private var verifier:Job?=null;private var connectGeneration=0
 init{val cached=runCatching{JSONObject(store.read()?:"{}")}.getOrDefault(JSONObject());expected=cached.optString("egress_ip");_state.value=_state.value.copy(lastConnection=cached.optLong("last_connection"),sessionId=cached.optString("session_id"),serverId=cached.optString("server_id"),started=cached.optLong("started"));de.blinkt.openvpn.core.VpnStatus.addByteCountListener(byteListener);viewModelScope.launch{try{api.anonymous();reload()}catch(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"Initialization failed")}finally{_state.value=_state.value.copy(initialized=true)}};viewModelScope.launch{while(isActive){delay(60000);if(_state.value.licensed)try{reload()}catch(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"License refresh failed");disconnect()}}}}
 fun work(block:suspend ()->Unit){if(_state.value.busy)return;viewModelScope.launch{_state.value=_state.value.copy(busy=true,error="");try{block()}catch(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"Operation failed")}finally{_state.value=_state.value.copy(busy=false)}}}
 private suspend fun reload(){
  val l=try{api.call("licenseInfo")}catch(e:Exception){if(!_state.value.licensed)return else throw e}
  val c=api.call("getConfiguration");val settings=c["settings"].obj();val order=settings.str("server_order").split(',').map{it.trim()};val servers=c["servers"].rows().sortedWith(compareBy<Map<String,Any?>>{order.indexOf(it.str("id")).let{i->if(i<0)Int.MAX_VALUE else i}}.thenBy{(it["priority"] as? Number)?.toInt()?:0});val selected=listOf(_state.value.serverId,settings.str("default_server"),servers.firstOrNull{it.str("role")=="primary"}?.str("id").orEmpty()).firstOrNull{id->servers.any{it.str("id")==id}}?:servers.firstOrNull()?.str("id").orEmpty()
  _state.value=_state.value.copy(licensed=true,license=l,servers=servers,settings=settings,social=c["social"].rows(),serverId=selected)
  persist()
 }
 private fun persist(){val s=_state.value;store.save(JSONObject(s.license+mapOf("last_connection" to s.lastConnection,"session_id" to s.sessionId,"server_id" to s.serverId,"started" to s.started,"egress_ip" to expected)).toString())}
 fun activate(code:String)=work{api.anonymous();val l=api.call("activateLicense",mapOf("code" to code));_state.value=_state.value.copy(licensed=true,license=l,tips=!store.tipsDismissed());reload()}
 fun refresh()=work{reload()}
 fun select(id:String){if(_state.value.status=="Disconnected"||_state.value.status=="Error")_state.value=_state.value.copy(serverId=id)}
 fun connect()=work{
  check(_state.value.status in listOf("Disconnected","Error")){"Disconnect before switching servers"}
  check(_state.value.serverId.isNotBlank()){ "No verified gateway has been published" }
  val generation=++connectGeneration
  _state.value=_state.value.copy(status="Connecting",ip="")
  try{val result=api.call("startSession",mapOf("server_id" to _state.value.serverId,"version" to BuildConfig.VERSION_CODE));if(generation!=connectGeneration){api.call("endSession",mapOf("session_id" to result.str("session_id")));return@work};expected=result.str("egress_ip");check(expected.isNotBlank()){ "Gateway did not supply an egress verification IP" };_state.value=_state.value.copy(sessionId=result.str("session_id"));persist();tunnel.start(result.str("profile"))}catch(e:Exception){releaseSession();_state.value=_state.value.copy(status="Error");throw e}
 }
 private fun engineStatus(raw:String){viewModelScope.launch{
  when(raw){
   "CONNECTED"->{verifier?.cancel();verifier=viewModelScope.launch{_state.value=_state.value.copy(status="Connecting");try{val ip=tunnel.verify(expected);val start=_state.value.started.takeIf{it>0}?:System.currentTimeMillis();_state.value=_state.value.copy(status="Connected",ip=ip,started=start,lastConnection=start);persist();while(isActive){delay(15000);tunnel.verify(expected)}}catch(e:CancellationException){throw e}catch(e:Exception){tunnel.disconnect();releaseSession();_state.value=_state.value.copy(status="Error",ip="",error="Tunnel connectivity verification failed. Check gateway, DNS and egress IP.")}}}
   "RECONNECTING","WAIT","AUTH","GET_CONFIG","ASSIGN_IP","ADD_ROUTES"->{_state.value=_state.value.copy(status=if(_state.value.started>0)"Reconnecting" else "Connecting",ip="")}
   "DISCONNECTED","NOPROCESS","EXITING"->{verifier?.cancel();releaseSession();_state.value=_state.value.copy(status="Disconnected",ip="",started=0)}
   "AUTH_FAILED","SERVICE_LOST"->{verifier?.cancel();releaseSession();_state.value=_state.value.copy(status="Error",ip="",error="VPN authorization or engine failure")}
  }
 }}
 private suspend fun releaseSession(){val id=_state.value.sessionId;if(id.isNotBlank())runCatching{api.call("endSession",mapOf("session_id" to id))};_state.value=_state.value.copy(sessionId="",started=0);persist()}
 fun disconnect(){connectGeneration++;verifier?.cancel();_state.value=_state.value.copy(status="Disconnecting");viewModelScope.launch{runCatching{tunnel.disconnect()};releaseSession();_state.value=_state.value.copy(status="Disconnected",ip="",started=0)}}
 fun tips(show:Boolean){_state.value=_state.value.copy(tips=show)}
 fun dismiss(){store.dismissTips();tips(false)}
 fun permissionError(){_state.value=_state.value.copy(error="VPN permission was not granted")}
 fun localError(e:Exception){_state.value=_state.value.copy(error=e.localizedMessage?:"VPN service unavailable")}
 override fun onCleared(){de.blinkt.openvpn.core.VpnStatus.removeByteCountListener(byteListener);verifier?.cancel();tunnel.close();super.onCleared()}
}
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{FavorTheme{ClientScreen()}}}}
@Composable fun ClientScreen(m:ClientModel=viewModel()){
 val s by m.state.collectAsStateWithLifecycle();var tab by remember{mutableIntStateOf(0)};var code by remember{mutableStateOf("")};val context=androidx.compose.ui.platform.LocalContext.current
 val vpnPermission=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){if(it.resultCode==android.app.Activity.RESULT_OK)m.connect()else m.permissionError()}
 val apiPermission=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){if(it.resultCode==android.app.Activity.RESULT_OK){runCatching{m.tunnel.observe();val p=m.tunnel.prepareVpn();if(p!=null)vpnPermission.launch(p)else m.connect()}.onFailure{m.localError(it as Exception)}}else m.permissionError()}
 val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
 fun connect(){try{if(android.os.Build.VERSION.SDK_INT>=33)notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS);val p=m.tunnel.permission();if(p!=null)apiPermission.launch(p)else{m.tunnel.observe();val v=m.tunnel.prepareVpn();if(v!=null)vpnPermission.launch(v)else m.connect()}}catch(e:Exception){m.localError(e)}}
 fun open(url:String){runCatching{val u=Uri.parse(url);require(u.scheme in listOf("https","mailto"));context.startActivity(Intent(Intent.ACTION_VIEW,u))}.onFailure{m.localError(Exception("No application can open this link"))}}
 Backdrop{Scaffold(containerColor=androidx.compose.ui.graphics.Color.Transparent,bottomBar={if(s.licensed)NavigationBar(containerColor=Navy){listOf("Home","Activity","License","Settings").forEachIndexed{i,v->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(listOf("⌂","◷","◇","⚙")[i],color=if(tab==i)Gold else Violet)},label={Text(v)})}}}){padding->Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal=22.dp),horizontalAlignment=Alignment.CenterHorizontally){
 if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Violet)
 if(s.error.isNotEmpty())Glass("Action required"){Text(s.error,color=MaterialTheme.colorScheme.error)}
 if(!s.initialized){Spacer(Modifier.height(50.dp));Brand(large=true);Shield();CircularProgressIndicator(color=Violet,modifier=Modifier.padding(24.dp));Text("Initializing secure services…");Skyline()}
 else if(!s.licensed){Spacer(Modifier.height(30.dp));Brand(large=true);Shield();Glass("Enter License Key to Activate VPN"){Field("USA-XXXX-XXXX",code,{code=it.uppercase().take(13)});Action("ACTIVATE LICENSE",!s.busy&&code.length==13){m.activate(code)}};Text("Your license is verified by the secure service before every VPN session.",color=Violet);Skyline()}
 else{Brand();when(tab){
  0->{Text(s.settings.str("welcome").ifBlank{"USA FAVOR VPN"},fontSize=22.sp,fontWeight=FontWeight.Bold,color=Gold);if(s.settings.str("announcement").isNotBlank())Glass{Text(s.settings.str("announcement"))};if(s.settings["maintenance"]==true)Glass("Maintenance"){Text(s.settings.str("maintenance_message"))}
   PowerButton(s.status){if(s.status in listOf("Disconnected","Error"))connect()else m.disconnect()};Text(s.status,fontSize=20.sp,fontWeight=FontWeight.Bold,color=if(s.status=="Connected")Cyan else Violet)
   Text(if(s.status in listOf("Disconnected","Error"))"Tap to connect" else "Tap to disconnect",color=Gold,modifier=Modifier.padding(8.dp))
   if(s.servers.isEmpty())Glass("No operational gateway"){Text("An administrator must deploy, register and verify a gateway before connection is available.")}
   s.servers.forEach{server->Glass{Row(verticalAlignment=Alignment.CenterVertically){RadioButton(s.serverId==server.str("id"),{m.select(server.str("id"))},enabled=s.status in listOf("Disconnected","Error"));Column{Text("${server.str("flag").ifBlank{"🇺🇸"}} ${server.str("name")}",fontWeight=FontWeight.Bold);Text("${server.str("country")} · ${server.str("city")}",color=Violet);val health=server["health"].obj();Text(if(health["online"]==true&&(health["checked_at"] as? Number)?.toLong()?.let{System.currentTimeMillis()-it<120000}==true)"Gateway online" else "Health unavailable / offline",color=Gold)}}}}
   Glass{Fact("Public IP",s.ip.ifBlank{"Not verified"});Fact("Protocol","OpenVPN · "+s.servers.firstOrNull{it.str("id")==s.serverId}?.str("transport").orEmpty());Fact("License expires",date(s.license["expires_at"]))}
   if(s.settings["show_social"]!=false)Social(s.social,::open)
  }
  1->{PageTitle("Connection activity");var now by remember{mutableLongStateOf(System.currentTimeMillis())};LaunchedEffect(Unit){while(true){delay(1000);now=System.currentTimeMillis()}};Glass("Live diagnostics"){Fact("Tunnel state",s.status);Fact("Verified public IP",s.ip.ifBlank{"Unavailable"});Fact("Connected duration",if(s.started>0)"${(now-s.started)/1000}s" else "0s");Fact("Last connection",if(s.lastConnection>0)date(s.lastConnection)else "No recorded connection");Fact("Tunnel received","${s.received} bytes");Fact("Tunnel sent","${s.sent} bytes");Text("Traffic counters are supplied by the OpenVPN engine for the tunnel, not individual applications.",color=Violet)}}
  2->{PageTitle("Your license");Glass("Subscription"){Fact("License",s.license.str("masked"));Fact("Status",s.license.str("status"));Fact("Activated",date(s.license["activated_at"]));Fact("Expires",date(s.license["expires_at"]));val expiry=(s.license["expires_at"] as? Number)?.toLong()?:0;Fact("Days remaining",kotlin.math.max(0,(expiry-System.currentTimeMillis()+86399999)/86400000).toString());Action("REFRESH LICENSE",!s.busy){m.refresh()};Text(s.settings.str("support").ifBlank{"Contact your license seller to renew your subscription."})}}
  3->{PageTitle("Settings");Glass("Connection preferences"){Text("OpenVPN • verified gateways only • IPv6 blocked • gateway-managed DNS. Select your preferred published gateway on Home.");Action("REFRESH CONFIGURATION",!s.busy){m.refresh()};Action("ANDROID VPN SETTINGS"){context.startActivity(Intent(android.provider.Settings.ACTION_VPN_SETTINGS))};if(s.settings["show_tips"]!=false)Action("USA CONTENT TIPS"){m.tips(true)};Text(s.settings.str("help"));Text(s.settings.str("support"));Fact("App version",BuildConfig.VERSION_NAME);Fact("Minimum supported",s.settings.str("minimum_version").ifBlank{"1"});Text(s.settings.str("update_notice"));if(s.settings.str("privacy_url").isNotBlank())Action("PRIVACY POLICY"){open(s.settings.str("privacy_url"))};if(s.settings.str("terms_url").isNotBlank())Action("TERMS OF SERVICE"){open(s.settings.str("terms_url"))}};if(s.settings["show_social"]!=false)Social(s.social,::open)}
 };Skyline()}
 }}}
 if(s.tips)AlertDialog(onDismissRequest={m.tips(false)},title={Text("معلومة مهمة قبل الاستخدام")},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState())){Text(s.settings.str("activation_popup").ifBlank{DEFAULT_TIPS})}},confirmButton={TextButton(onClick={m.dismiss()}){Text("حسناً، فهمت")}},dismissButton={TextButton(onClick={m.dismiss()}){Text("عدم الإظهار مرة أخرى")}})
}
@Composable fun Social(items:List<Map<String,Any?>>,open:(String)->Unit){items.filter{it["enabled"]==true}.forEach{v->OutlinedButton(onClick={open(v.str("url"))},modifier=Modifier.fillMaxWidth()){Text(when(v.str("icon")){"whatsapp"->"◉ ";"telegram"->"➤ ";"instagram"->"◎ ";"email"->"✉ ";"youtube"->"▶ ";else->"↗ "}+v.str("name"),color=Cyan)}}}
