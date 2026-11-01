package com.usafavor.client

import android.content.*
import android.os.IBinder
import de.blinkt.openvpn.api.IOpenVPNAPIService
import de.blinkt.openvpn.api.IOpenVPNStatusCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.*
import org.json.JSONObject
import java.net.URL

/** Binds the embedded OpenVPN engine in this APK, never a simulated tunnel. */
class Tunnel(private val context:Context,private val onStatus:(String)->Unit){
 var api:IOpenVPNAPIService?=null;private set
 private var registered=false
 private val callback=object:IOpenVPNStatusCallback.Stub(){override fun newStatus(uuid:String?,state:String?,message:String?,level:String?){onStatus(state?:"UNKNOWN")}}
 private val connection=object:ServiceConnection{
  override fun onServiceConnected(name:ComponentName?,service:IBinder?){api=IOpenVPNAPIService.Stub.asInterface(service);runCatching{if(api?.prepare(context.packageName)==null)observe()}}
  override fun onServiceDisconnected(name:ComponentName?){api=null;onStatus("SERVICE_LOST")}
 }
 init{context.bindService(Intent().setClassName(context.packageName,"de.blinkt.openvpn.api.ExternalOpenVPNService"),connection,Context.BIND_AUTO_CREATE)}
 fun permission():Intent? { val a=api?:error("VPN engine is initializing; try again");return a.prepare(context.packageName) }
 fun prepareVpn():Intent?=android.net.VpnService.prepare(context)
 fun observe(){if(!registered){api?.registerStatusCallback(callback);registered=true}}
 fun start(profile:String){val a=api?:error("VPN engine unavailable");observe();a.startVPN(profile)}
 fun disconnect(){api?.disconnect()}
 fun close(){if(registered)runCatching{api?.unregisterStatusCallback(callback)};runCatching{context.unbindService(connection)}}
 suspend fun verify(expected:String):String=withContext(Dispatchers.IO){
  val cm=context.getSystemService(ConnectivityManager::class.java)
  val network=cm.allNetworks.firstOrNull{cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)==true}?:error("Tunnel network not established")
  val c=network.openConnection(URL("https://api.ipify.org?format=json")) as javax.net.ssl.HttpsURLConnection
  c.connectTimeout=8000;c.readTimeout=8000
  try{check(c.responseCode==200){"Public IP verification failed"};val result=JSONObject(c.inputStream.bufferedReader().use{it.readText()}).getString("ip");check(result==expected){"Tunnel egress does not match verified gateway"};result}finally{c.disconnect()}
 }
}
