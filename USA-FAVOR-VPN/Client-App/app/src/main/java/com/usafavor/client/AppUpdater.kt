package com.usafavor.client

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.b5studios.shared.AppUpdate
import com.b5studios.shared.RemoteUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

/** HTTPS + digest + package/version + existing signing certificate before Android installation. */
class AppUpdater(private val context:Context){
 private val folder get()=File(context.cacheDir,"verified-updates").apply{mkdirs()}
 suspend fun download(update:AppUpdate,progress:(Int)->Unit):File=withContext(Dispatchers.IO){
  update.validate();require(update.enabled){"No update is published"}
  val part=File(folder,"download.part")
  var url=update.url
  try{
   var connection:HttpsURLConnection?=null
   for(attempt in 0..5){
    require(RemoteUrls.https(url)){"The update must use a public HTTPS address"}
    val c=URL(url).openConnection() as HttpsURLConnection
    c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=30000
    val response=c.responseCode
    if(response in listOf(301,302,303,307,308)){
     val next=c.getHeaderField("Location")?:error("Update redirect is missing")
     url=URL(URL(url),next).toString();c.disconnect();continue
    }
    check(response==200){"APK download failed ($response)"};connection=c;break
   }
   val c=connection?:error("Too many update redirects")
   try{
    val limit=134217728L;val expected=c.contentLengthLong
    require(expected<=limit){"APK exceeds 128 MiB"}
    val digest=MessageDigest.getInstance("SHA-256");var downloaded=0L
    c.inputStream.use{input->part.outputStream().use{output->
     val buffer=ByteArray(32768)
     while(true){
      currentCoroutineContext().ensureActive();val count=input.read(buffer);if(count<0)break
      downloaded+=count;check(downloaded<=limit){"APK exceeds 128 MiB"}
      output.write(buffer,0,count);digest.update(buffer,0,count)
      progress(if(expected>0)((downloaded*100/expected).toInt().coerceIn(0,99))else -1)
     }
    }}
    check(expected<0||downloaded==expected){"Incomplete APK download"}
    val actual=digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
    check(actual.equals(update.sha256,true)){"Update checksum does not match. Installation stopped."}
    verify(part,update.latest)
    val destination=File(folder,"update.apk");if(destination.exists())check(destination.delete())
    check(part.renameTo(destination)){"Cannot prepare the verified update"};progress(100);destination
   }finally{c.disconnect()}
  }finally{part.delete()}
 }
 @Suppress("DEPRECATION") private fun verify(file:File,expectedVersion:Long){
  val pm=context.packageManager;val flags=if(Build.VERSION.SDK_INT>=28)PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
  val downloaded=pm.getPackageArchiveInfo(file.absolutePath,flags)?:error("The downloaded file is not an Android APK")
  val installed=pm.getPackageInfo(context.packageName,flags)
  check(downloaded.packageName==context.packageName){"The update belongs to another application"}
  val version=if(Build.VERSION.SDK_INT>=28)downloaded.longVersionCode else downloaded.versionCode.toLong()
  val current=if(Build.VERSION.SDK_INT>=28)installed.longVersionCode else installed.versionCode.toLong()
  check(version>current&&version==expectedVersion){"The APK version does not match the published update"}
  fun certificates(info:android.content.pm.PackageInfo):Set<String>{
   val signatures=if(Build.VERSION.SDK_INT>=28)info.signingInfo?.apkContentsSigners else info.signatures
   return signatures?.map{MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString(""){b->"%02x".format(b.toInt() and 255)}}?.toSet().orEmpty()
  }
  val original=certificates(installed)
  check(original.isNotEmpty()&&original==certificates(downloaded)){"The APK signing key differs from this installation. Ask the administrator for a matching update."}
 }
 fun mayInstall()=context.packageManager.canRequestPackageInstalls()
 fun permissionIntent()=Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${context.packageName}"))
 fun installer(file:File)=Intent(Intent.ACTION_VIEW).apply{
  setDataAndType(FileProvider.getUriForFile(context,"${context.packageName}.updates",file),"application/vnd.android.package-archive")
  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
 }
}
