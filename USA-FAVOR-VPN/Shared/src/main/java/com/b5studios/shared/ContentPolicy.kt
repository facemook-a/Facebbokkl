package com.b5studios.shared

import java.net.URI

object RemoteUrls {
 fun https(value:String):Boolean=runCatching {
  val u=URI(value);val host=u.host.orEmpty().lowercase()
  u.scheme=="https"&&u.userInfo==null&&u.port in listOf(-1,443)&&u.fragment==null&&host.matches(Regex("[a-z0-9-]+(\\.[a-z0-9-]+)+"))&&host.any{it in 'a'..'z'}&&!listOf(".local",".localhost",".internal",".invalid").any{host.endsWith(it)}
 }.getOrDefault(false)
 fun link(value:String)=https(value)||runCatching{val u=URI(value);u.scheme=="mailto"&&u.rawQuery==null&&u.schemeSpecificPart.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))}.getOrDefault(false)
}
object SectionPolicy {
 val actions=listOf("none","link","license","activity","settings","refresh","select_apps","connect","disconnect","update")
 fun action(x:Map<String,Any?>)=if(x["kind"]=="code")"code" else (x["action"] as? String)?:if((x["url"] as? String).isNullOrBlank())"none"else "link"
 fun validate(items:List<Map<String,Any?>>){
  require(items.size<=20&&items.map{it["id"]}.distinct().size==items.size){"Choose up to 20 sections with unique IDs"}
  var total=0
  items.forEach{x->
   fun str(key:String)=x[key] as? String?:""
   require(str("id").matches(Regex("[A-Za-z0-9_-]{1,80}"))&&str("title").length in 1..80&&str("body").length<=3000&&x["enabled"] is Boolean&&x["target"] in listOf("home","settings")){"Invalid section"}
   require(x["kind"]==null||x["kind"] in listOf("card","code")){"Unsupported section type"}
   require(action(x) in actions+"code"&&str("button_label").length<=80){"Choose a supported action"}
   if(str("url").isNotBlank())require(RemoteUrls.link(str("url"))){"HTTPS or support email required"}
   if(action(x)=="link")require(RemoteUrls.link(str("url"))){"Add a valid link for this section"}
   for(key in listOf("html","css","javascript")){
    val bytes=str(key).toByteArray(Charsets.UTF_8).size
    require(bytes<=32768){"Each code field is limited to 32 KiB"};total+=bytes
   }
   if(x["kind"]=="code")require(str("html").isNotBlank()){"Add HTML for this tool"}
   total+=str("title").toByteArray().size+str("body").toByteArray().size+str("url").toByteArray().size+512
  }
  require(total<=524288){"Combined sections must fit within 512 KiB"}
 }
}
data class AppUpdate(val enabled:Boolean=false,val latest:Long=1,val minimum:Long=1,val required:Boolean=false,val url:String="",val sha256:String="",val message:String="") {
 fun offered(current:Long)=enabled&&latest>current
 fun mandatory(current:Long)=offered(current)&&(required||current<minimum)
 fun fields():Map<String,Any?> = mapOf("enabled" to enabled,"latest_version_code" to latest,"minimum_version" to minimum,"required" to required,"apk_url" to url,"apk_sha256" to sha256,"message" to message)
 fun validate(){
  require(latest in 1..2100000000L&&minimum in 1..latest&&message.length<=2000&&url.length<=4000){"Check update versions and message"}
  if(enabled)require(RemoteUrls.https(url)&&sha256.matches(Regex("[a-fA-F0-9]{64}"))){"Add an HTTPS APK link and its SHA-256 checksum"}
 }
 companion object {
  fun from(x:Map<String,Any?>)=AppUpdate(x["enabled"]==true,(x["latest_version_code"] as? Number)?.toLong()?:1,(x["minimum_version"] as? Number)?.toLong()?:1,x["required"]==true,x["apk_url"] as? String?:"",x["apk_sha256"] as? String?:"",x["message"] as? String?:"")
 }
}
