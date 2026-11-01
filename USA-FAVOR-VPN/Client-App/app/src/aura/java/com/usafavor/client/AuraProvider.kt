package com.usafavor.client

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.b5studios.shared.str
import kotlinx.coroutines.suspendCancellableCoroutine
import unified.vpn.sdk.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Compile only with the supported SDK version supplied by your authorized Aura partner. */
class AuraProvider(private val context:Context,private val status:(String)->Unit):ProviderTunnel {
    override val available=true
    private var generation=0
    private var sdk:UnifiedSDK?=null
    private val listener=object:VpnStateListener {
        override fun vpnStateChanged(state:VpnState) {
            when(state.toString()) {
                "CONNECTED" -> {
                    val cm=context.getSystemService(ConnectivityManager::class.java)
                    val tunnel=cm.allNetworks.any{cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)==true}
                    if(tunnel)status("Connected")else {disconnect();status("Error")}
                }
                "IDLE", "DISCONNECTED" -> status("Disconnected")
                "CONNECTING", "RECONNECTING" -> status("Connecting")
                "DISCONNECTING" -> status("Disconnecting")
            }
        }
        override fun vpnError(error:VpnException){status("Error")}
    }
    init { UnifiedSDK.addVpnStateListener(listener) }
    override suspend fun connect(config:Map<String,Any?>,locationId:String,grant:String,apps:Set<String>) {
        require(config.str("base_url").startsWith("https://")&&config.str("carrier_id").isNotBlank()){"Partner configuration missing"}
        val instance=UnifiedSDK.getInstance(ClientInfo.newBuilder().baseUrl(config.str("base_url")).carrierId(config.str("carrier_id")).build())
        val operation=++generation
        sdk=instance
        suspendCancellableCoroutine<Unit>{continuation->
            instance.backend.login(AuthMethod.customOauth(grant),object:Callback<User>{
                override fun success(response:User){if(continuation.isActive)continuation.resume(Unit)}
                override fun failure(error:VpnException){if(continuation.isActive)continuation.resumeWithException(error)}
            })
        }
        if(operation!=generation)return
        // Always include the VPN application's control requests in a selected-app tunnel.
        val policy=if(apps.isEmpty())AppPolicy.forAll()else AppPolicy.newBuilder().policy(AppPolicy.POLICY_FOR_LIST).appList((apps+context.packageName).toList()).build()
        val session=SessionConfig.Builder().withCountry("US").withLocation(locationId).withPolicy(policy).withReason(TrackingConstants.GprReasons.M_UI).build()
        suspendCancellableCoroutine<Unit>{continuation->
            continuation.invokeOnCancellation{disconnect()}
            instance.vpn.start(session,object:CompletableCallback{
                override fun complete(){if(continuation.isActive)continuation.resume(Unit)}
                override fun error(error:VpnException){status("Error");if(continuation.isActive)continuation.resumeWithException(error)}
            })
        }
    }
    override fun disconnect(){generation++;sdk?.vpn?.stop(TrackingConstants.GprReasons.M_UI,object:CompletableCallback{
        override fun complete(){status("Disconnected")}
        override fun error(error:VpnException){status("Error")}
    })}
    override fun close(){UnifiedSDK.removeVpnStateListener(listener)}
}
