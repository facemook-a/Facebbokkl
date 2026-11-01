package com.b5studios.shared

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
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
    suspend fun login(email:String,password:String) { require(configured) { "Firebase configuration missing for this application package" }; auth.signInWithEmailAndPassword(email,password).await(); val token=auth.currentUser!!.getIdToken(true).await(); if(token.claims["admin"]!=true) { auth.signOut(); error("This account is not an authorized administrator") } }
    suspend fun reset(email:String) { require(configured) { "Firebase configuration missing" }; auth.sendPasswordResetEmail(email).await() }
    @Suppress("UNCHECKED_CAST")
    suspend fun call(name:String,data:Map<String,Any?> = emptyMap()):Map<String,Any?> { require(configured) { "Firebase configuration missing" }; return FirebaseFunctions.getInstance("us-central1").getHttpsCallable(name).call(data).await().data as? Map<String,Any?> ?: emptyMap() }
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
const val DEFAULT_TIPS="إذا كنت ترغب في استكشاف محتوى أمريكي في تطبيقات مثل TikTok وInstagram وYouTube، اتصل بخادم الولايات المتحدة ثم أعد تشغيل التطبيق المستهدف. يمكن أن يساعد مسح ذاكرة التخزين المؤقت أحياناً على تحديث الاقتراحات. لكن VPN لا يضمن ظهور محتوى أمريكي، لأن المنصات تعتمد أيضاً على الحساب واللغة والموقع وسجل التفاعل. انتبه أن مسح جميع بيانات التطبيق قد يؤدي إلى تسجيل خروجك أو فقدان بيانات محفوظة محلياً."
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
