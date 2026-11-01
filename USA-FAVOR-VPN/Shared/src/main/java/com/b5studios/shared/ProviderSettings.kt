package com.b5studios.shared

/** Public partner configuration only; private partner credentials never enter Firestore client settings. */
data class ProviderSettings(
    val connected: Boolean = false,
    val carrierId: String = "",
    val baseUrl: String = "",
    val perApp: Boolean = false,
    val locations: List<Map<String, Any?>> = emptyList(),
    val enabledLocations: Set<String> = emptySet()
) {
    val available get() = if(!connected) emptyList() else locations.filter { it["id"] in enabledLocations && it["country_code"] == "US" }
    companion object {
        fun from(data: Map<String, Any?>): ProviderSettings = ProviderSettings(
            connected = data["provider"] == "aura" && data["carrier_id"] is String && (data["carrier_id"] as String).isNotBlank() && (data["base_url"] as? String)?.startsWith("https://") == true && data["integration_verified"] == true && data["expiry_enforcement_verified"] == true,
            carrierId = data["carrier_id"] as? String ?: "",
            baseUrl = data["base_url"] as? String ?: "",
            perApp = data["per_app_supported"] == true,
            locations = (data["locations"] as? List<*>)?.mapNotNull { @Suppress("UNCHECKED_CAST") (it as? Map<String, Any?>) } ?: emptyList(),
            enabledLocations = (data["enabled_locations"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
        )
    }
}
class LicenseNotActivated : IllegalStateException("Activate your license first.")
