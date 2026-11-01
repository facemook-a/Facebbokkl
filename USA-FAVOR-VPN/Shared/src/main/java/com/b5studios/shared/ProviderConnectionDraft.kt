package com.b5studios.shared

import java.net.URI

/** Public partner identifiers only. Never accepts or stores a provider master credential. */
data class ProviderConnectionDraft(val carrierId:String,val baseUrl:String) {
    fun fields():Map<String,Any?> = mapOf("provider" to "aura","carrier_id" to carrierId,"base_url" to baseUrl)
    companion object {
        fun parse(carrierId:String,baseUrl:String):ProviderConnectionDraft {
            val carrier=carrierId.trim()
            require(carrier.matches(Regex("[A-Za-z0-9._-]{1,128}"))){"Enter the public partner project ID supplied by your provider."}
            val uri=runCatching{URI(baseUrl.trim())}.getOrNull()
            require(uri!=null&&uri.scheme=="https"&&uri.userInfo==null&&uri.port==-1&&uri.rawQuery==null&&uri.rawFragment==null&&uri.path in listOf("","/")&&publicHostname(uri.host.orEmpty())){"Enter the public HTTPS backend address from your partner account, without credentials or a query string."}
            return ProviderConnectionDraft(carrier,"https://${uri!!.host.lowercase()}/")
        }
        private fun publicHostname(host:String):Boolean {
            if(!host.matches(Regex("[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+"))||host.endsWith('.'))return false
            val lower=host.lowercase()
            if(lower.endsWith(".local")||lower.endsWith(".localhost")||lower.endsWith(".internal")||lower.endsWith(".invalid"))return false
            // Partner backend addresses are DNS names, never private or numeric endpoints.
            return lower.any{it in 'a'..'z'}
        }
    }
}
