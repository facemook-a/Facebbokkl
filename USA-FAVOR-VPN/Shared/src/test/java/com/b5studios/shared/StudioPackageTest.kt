package com.b5studios.shared
import org.junit.Assert.*
import org.junit.Test
class StudioPackageTest {
 @Test fun providerPackageSavesOnlyPrivateDraftWithoutReadinessOrSecrets(){val pair=StudioPackage.validate(mapOf("schema_version" to 1,"kind" to "provider","payload" to mapOf("provider" to "aura","carrier_id" to "public-id","base_url" to "https://partner.example.com/")));assertEquals("saveProviderConnectionDraft",pair.first);assertEquals(setOf("carrier_id","base_url"),pair.second.keys)}
 @Test fun arbitraryNativeCodeAndMasterCredentialsAreRejected(){for(data in listOf(mapOf("schema_version" to 1,"kind" to "dex","payload" to emptyMap<String,Any?>()),mapOf("schema_version" to 1,"kind" to "provider","payload" to mapOf("provider" to "aura","master_secret" to "synthetic-test-only")))){try{StudioPackage.validate(data);fail("Unsafe package accepted")}catch(_:IllegalArgumentException){}catch(_:IllegalStateException){}}}
 @Test fun codeSectionsMustPassTheSameLimitsAsTheEditor(){val invalid=mapOf("schema_version" to 1,"kind" to "sections","payload" to mapOf("items" to listOf(mapOf("id" to "bad","title" to "Tool","body" to "","target" to "home","enabled" to true,"kind" to "code","html" to "x".repeat(32769)))));try{StudioPackage.validate(invalid);fail("oversized code accepted")}catch(_:IllegalArgumentException){}}
}
