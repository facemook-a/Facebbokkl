package com.b5studios.shared

/** Data packages extend configured features; never load dex, native libraries or owner credentials. */
object StudioPackage {
 fun validate(packageData:Map<String,Any?>):Pair<String,Map<String,Any?>> {
  require(packageData.keys.all{it in setOf("schema_version","kind","payload")}){"Unsupported package field"}
  require((packageData["schema_version"] as? Number)?.toInt()==1){"Unsupported package version"}
  val payload=packageData["payload"].obj()
  return when(packageData["kind"]){
   "sections"->{require(payload.keys==setOf("items")&&payload["items"] is List<*>){"Add the section items"};SectionPolicy.validate(payload["items"].rows());"saveSections" to payload}
   "provider"->{require(payload.keys.all{it in setOf("provider","carrier_id","base_url")}&&payload["provider"]=="aura"){"A supported provider with public connection details is required"};val draft=ProviderConnectionDraft.parse(payload.str("carrier_id"),payload.str("base_url"));"saveProviderConnectionDraft" to draft.fields().filterKeys{it!="provider"}}
   "update"->{require(payload["latest_version_code"] is Number&&payload["minimum_version"] is Number&&payload["enabled"] is Boolean&&payload["required"] is Boolean){"Check update versions and switches"};require(payload.keys.all{it in setOf("enabled","latest_version_code","minimum_version","required","apk_url","apk_sha256","message")}){"Unsupported update field"};AppUpdate.from(payload).validate();"saveAppUpdate" to payload}
   else->error("Only sections, provider and update data packages are supported")
  }
 }
}
