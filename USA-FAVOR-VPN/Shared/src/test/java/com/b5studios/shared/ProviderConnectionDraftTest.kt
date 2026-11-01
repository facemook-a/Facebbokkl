package com.b5studios.shared
import org.junit.Assert.*
import org.junit.Test
class ProviderConnectionDraftTest {
 @Test fun publicPartnerDetailsNormalizeWithoutReadinessOrCredentials(){
  val draft=ProviderConnectionDraft.parse(" partner-public-id "," https://PARTNER.example.com ")
  assertEquals("partner-public-id",draft.carrierId)
  assertEquals("https://partner.example.com/",draft.baseUrl)
  assertEquals(setOf("provider","carrier_id","base_url"),draft.fields().keys)
  assertFalse(ProviderSettings.from(draft.fields()).connected)
 }
 @Test fun credentialsAndNonPublicEndpointsAreRejected(){
  for(url in listOf("http://partner.example.com","https://user:secret@partner.example.com","https://partner.example.com?token=private","https://partner.example.com/#secret","https://127.0.0.1","https://10.0.0.1","https://localhost","https://service.local","https://service.internal","https://partner.example.com:8443")) {
   try{ProviderConnectionDraft.parse("public-id",url);fail("Accepted unsafe endpoint: $url")}catch(_:IllegalArgumentException){}
  }
 }
 @Test fun projectIdentifiersRejectEmptyAndStructuredCredentialInput(){
  for(id in listOf(""," ","a".repeat(129),"{\"private_key\":\"value\"}","name\nsecret")) {
   try{ProviderConnectionDraft.parse(id,"https://partner.example.com");fail("Accepted invalid public project identifier")}catch(_:IllegalArgumentException){}
  }
 }
}
