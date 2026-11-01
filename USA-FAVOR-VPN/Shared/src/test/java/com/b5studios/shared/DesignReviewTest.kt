package com.b5studios.shared
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
/** Actual component rendering fixtures; no Firebase authorization or real VPN tunnel is simulated. */
class DesignReviewTest {
 @get:Rule val paparazzi=Paparazzi(deviceConfig=DeviceConfig.PIXEL_5,theme="android:style/Theme.Material.NoActionBar")
 @Test fun clientConnectionArt(){paparazzi.snapshot(name="client_connection_art"){FavorTheme{Backdrop{Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Brand();Text("USA FAVOR VPN",color=Gold,fontSize=24.sp);CyberConnectionControl("Disconnected"){};Glass("License activation"){Text("One license · one device",fontSize=16.sp);Field("License key","USA-XXXX-XXXX",{});Action("ACTIVATE LICENSE"){} };Skyline()}}}}}
 @Test fun adminControls(){paparazzi.snapshot(name="admin_controls"){FavorTheme(admin=true){Backdrop{Column(Modifier.fillMaxSize().padding(20.dp)){Brand(admin=true,large=true);Glass("Administrator sign in"){Field("Email","",{});Field("Password","",{},secret=true);Action("SIGN IN"){} };Glass("Management studio"){Text("التراخيص والأجهزة والخوادم",fontSize=16.sp);Action("MANAGE LICENSES"){};Action("VPN FILE LIBRARY"){} };Skyline()}}}}}
}
