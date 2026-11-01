package com.usafavor.client
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import com.b5studios.shared.*
import java.util.Locale
/** Full production composables rendered locally. Empty preview state, no backend/tunnel assertions. */
class AppScreenReviewTest {
 @get:Rule val paparazzi=Paparazzi(deviceConfig=DeviceConfig.PIXEL_5,theme="android:style/Theme.Material.NoActionBar")
 private val registryOwner=object:ActivityResultRegistryOwner {
  override val activityResultRegistry=object:ActivityResultRegistry(){
   override fun <I,O> onLaunch(requestCode:Int,contract:ActivityResultContract<I,O>,input:I,options:ActivityOptionsCompat?)=Unit
  }
 }
 private fun render(name:String,content:@Composable ()->Unit){
  Locale.setDefault(Locale.ENGLISH)
  paparazzi.snapshot(name=name){CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner){FavorTheme(admin=false){content()}}}
 }
 private fun model(state:ClientState):ClientModel {
  val model=Mockito.mock(ClientModel::class.java)
  Mockito.`when`(model.state).thenReturn(MutableStateFlow(state))
  Mockito.`when`(model.tunnel).thenReturn(MissingProvider{})
  return model
 }
 @Test fun splash(){val m=model(ClientState());render("client_initializing"){ClientScreen(m)}}
 @Test fun activation(){val m=model(ClientState(initialized=true));render("client_activation"){ClientScreen(m)}}
 @Test fun homeProviderNotConnected(){val m=model(ClientState(initialized=true,licensed=true));render("client_home_provider_not_connected"){ClientScreen(m)}}
 @Test fun activationSuccess(){val m=model(ClientState(initialized=true,licensed=true,activationSuccess=true));render("client_activation_success"){ClientScreen(m)}}
 @Test fun updateNotice(){val m=model(ClientState(initialized=true,appUpdate=mapOf("enabled" to true,"latest_version_code" to 6L,"minimum_version" to 1L,"required" to false,"apk_url" to "https://example.com/app.apk","apk_sha256" to "a".repeat(64),"message" to "A new version is ready.")));render("client_update_notice"){ClientScreen(m)}}
}
