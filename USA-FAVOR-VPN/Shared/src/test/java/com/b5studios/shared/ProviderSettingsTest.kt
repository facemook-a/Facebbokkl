package com.b5studios.shared
import org.junit.Assert.*
import org.junit.Test
class ProviderSettingsTest {
 private fun valid()=mapOf<String,Any?>("provider" to "aura","carrier_id" to "public-test-id","base_url" to "https://partner.example.com","integration_verified" to true,"expiry_enforcement_verified" to true,"per_app_supported" to true,"enabled_locations" to listOf("us-east","gb","unknown"),"locations" to listOf(mapOf("id" to "us-east","country_code" to "US"),mapOf("id" to "us-west","country_code" to "US"),mapOf("id" to "gb","country_code" to "GB")))
 @Test fun missingAndLegacyConfigurationCannotEnableVpn(){assertFalse(ProviderSettings.from(emptyMap()).connected);assertFalse(ProviderSettings.from(mapOf("host" to "legacy.example.com")).connected)}
 @Test fun readinessRequiresProviderIdentityAndBothVerificationSteps(){for(patch in listOf(mapOf("provider" to "other"),mapOf("carrier_id" to ""),mapOf("base_url" to "http://partner.example.com"),mapOf("integration_verified" to false),mapOf("expiry_enforcement_verified" to false))){val settings=ProviderSettings.from(valid()+patch);assertFalse(settings.connected);assertTrue(settings.available.isEmpty())}}
 @Test fun onlyEnabledProviderCatalogUsaLocationsAreOffered(){val settings=ProviderSettings.from(valid());assertTrue(settings.connected);assertEquals(listOf("us-east"),settings.available.map{it["id"]})}
 @Test fun perAppRoutingMustBeExplicitlySupported(){assertTrue(ProviderSettings.from(valid()).perApp);assertFalse(ProviderSettings.from(valid()-"per_app_supported").perApp)}
}
