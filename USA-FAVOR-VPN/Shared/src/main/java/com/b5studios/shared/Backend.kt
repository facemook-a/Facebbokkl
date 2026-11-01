package com.b5studios.shared

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class Backend(private val context: Context) {
    val configured get() = FirebaseApp.getApps(context).isNotEmpty()
    val auth get() = FirebaseAuth.getInstance()
    suspend fun anonymous() { require(configured) { "Add the matching Firebase registration and rebuild. See Documentation/FIREBASE.md." }; if (auth.currentUser==null) auth.signInAnonymously().await() }
    suspend fun login(email:String,password:String) { require(configured) { "Firebase configuration missing for this application package" }; require(email.isNotBlank()&&password.isNotEmpty()) { "Enter your email and password." }; auth.signInWithEmailAndPassword(email.trim(),password).await(); val token=auth.currentUser!!.getIdToken(true).await(); if(token.claims["admin"]!=true) { auth.signOut(); error("This account is not an authorized administrator") } }
    suspend fun reset(email:String) { require(configured) { "Firebase configuration missing" }; auth.sendPasswordResetEmail(email).await() }
    private val spark by lazy { SparkBackend(context,auth) }
    suspend fun call(name:String,data:Map<String,Any?> = emptyMap()):Map<String,Any?> {
        require(configured) { "Firebase configuration missing" }
        return spark.call(name,data)
    }
    fun watchLicense(onInvalid:(String)->Unit):ListenerRegistration = spark.watchLicense(onInvalid)

}
/** Metadata only. Authorization always goes through backend + gateway. */
class SecureStore(context:Context) {
    private val prefs=context.getSharedPreferences("activation_metadata",Context.MODE_PRIVATE)
    private fun key():SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (ks.getKey("favor.metadata",null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply { init(KeyGenParameterSpec.Builder("favor.metadata",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()) }.generateKey()
    }
    fun save(value:String) { val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key()); prefs.edit().putString("data",Base64.encodeToString(c.iv+c.doFinal(value.toByteArray()),Base64.NO_WRAP)).apply() }
    fun read():String? = runCatching { val b=Base64.decode(prefs.getString("data",null)?:return null,Base64.NO_WRAP);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,b.copyOfRange(0,12)));String(c.doFinal(b.copyOfRange(12,b.size))) }.getOrNull()
    fun tipsDismissed()=prefs.getBoolean("tips",false)
    fun dismissTips(){prefs.edit().putBoolean("tips",true).apply()}
}
const val DEFAULT_TIPS="To explore US content in apps such as TikTok, Instagram and YouTube, connect to a US server and then restart the app you want to use. Clearing the cache can sometimes refresh recommendations. A VPN does not guarantee US content: platforms also consider your account, language, location and activity history. Clearing all app data may sign you out or remove locally saved information. This app never clears another app's data automatically."
fun date(value:Any?):String = (value as? Number)?.toLong()?.let{java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT).format(java.util.Date(it))} ?: "Not activated"
@Suppress("UNCHECKED_CAST") fun Any?.obj() = this as? Map<String,Any?> ?: emptyMap()
@Suppress("UNCHECKED_CAST") fun Any?.rows() = (this as? List<*>)?.map{it.obj()} ?: emptyList()
fun Map<String,Any?>.str(k:String)=this[k]?.toString()?:""
fun initializeFirebase(context:Context,debug:Boolean){
    if(FirebaseApp.initializeApp(context)==null)return
    val factory:com.google.firebase.appcheck.AppCheckProviderFactory=if(debug){
        Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory").getMethod("getInstance").invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
    }else com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
    com.google.firebase.appcheck.FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
}

fun java.io.InputStream.readBounded(limit:Int):ByteArray { val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096);while(out.size()<limit){val n=read(buffer,0,kotlin.math.min(buffer.size,limit-out.size()));if(n<0)break;out.write(buffer,0,n)};return out.toByteArray() }
