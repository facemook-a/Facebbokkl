package com.usafavor.client

import android.content.Context

/** Provider adapters may emit Connected only from the SDK's real tunnel callbacks. */
interface ProviderTunnel {
    val available: Boolean
    suspend fun connect(config: Map<String,Any?>, locationId: String, grant: String, apps: Set<String>)
    fun disconnect()
    fun close()
}
class MissingProvider(private val status:(String)->Unit):ProviderTunnel {
    override val available=false
    override suspend fun connect(config:Map<String,Any?>,locationId:String,grant:String,apps:Set<String>):Unit =
        error("Live VPN service requires connecting an authorized provider and its Android SDK.")
    override fun disconnect(){status("Disconnected")}
    override fun close()=Unit
}
fun providerTunnel(context:Context,status:(String)->Unit):ProviderTunnel = try {
    val type=Class.forName("com.usafavor.client.AuraProvider")
    type.getConstructor(Context::class.java, kotlin.jvm.functions.Function1::class.java).newInstance(context,status) as ProviderTunnel
} catch(_:ClassNotFoundException){MissingProvider(status)}
