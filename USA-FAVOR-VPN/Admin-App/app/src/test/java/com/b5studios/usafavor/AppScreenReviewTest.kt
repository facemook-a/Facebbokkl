package com.b5studios.usafavor
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
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
  paparazzi.snapshot(name=name){CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner){FavorTheme(admin=true){content()}}}
 }
 private fun model(state:AdminState):AdminModel {
  val model=Mockito.mock(AdminModel::class.java)
  Mockito.`when`(model.state).thenReturn(MutableStateFlow(state))
  return model
 }
 private fun panel(name:String,tab:Int,content:@Composable ()->Unit){render(name){Backdrop{Scaffold(containerColor=Color.Transparent,bottomBar={PremiumNav(tab,admin=true){}}){padding->Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal=20.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){Brand()};TextButton(onClick={}){Text("Refresh")};TextButton(onClick={}){Text("Sign out")}};content();Spacer(Modifier.height(24.dp))}}}}}
 @Test fun licenseStudio(){val state=AdminState(ready=true);val m=model(state);panel("admin_license_studio",1){LicensePage(state,m)}}
 @Test fun vpnConnectionSettings(){val state=AdminState(ready=true);val m=model(state);panel("admin_vpn_connection_settings",2){VpnSettingsPage(state,m)}}
 @Test fun login(){val m=model(AdminState());render("admin_login"){AdminScreen(m)}}
 @Test fun dashboardBeforeSetup(){val m=model(AdminState(ready=true));render("admin_dashboard_empty"){AdminScreen(m)}}
 @Test fun sectionBuilder(){val state=AdminState(ready=true);val m=model(state);panel("admin_sections_builder",3){SectionEditor(state,m)}}
 @Test fun updateEditor(){val state=AdminState(ready=true);val m=model(state);panel("admin_update_editor",3){UpdateEditor(state,m)}}
 @Test fun arabicProvider(){val state=AdminState(ready=true);val m=model(state);render("admin_arabic_provider"){CompositionLocalProvider(LocalAdminLanguage provides "ar",androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl){Backdrop{androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(20.dp)){VpnSettingsPage(state,m)}}}}}
 @Test fun savedOwnerUnlock(){val m=model(AdminState(savedSession=true));render("admin_owner_unlock"){AdminScreen(m)}}
}
