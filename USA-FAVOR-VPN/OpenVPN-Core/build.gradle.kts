plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android {
 namespace="de.blinkt.openvpn"; compileSdk=35; ndkVersion="28.0.13004108"
 defaultConfig {
  minSdk=26
  buildConfigField("boolean","openvpn3","false")
  buildConfigField("String","FLAVOR","\"skeleton\"")
  externalNativeBuild { cmake { arguments += "-DB5_OPENVPN2_ONLY=ON"; abiFilters += listOf("arm64-v8a", "x86_64") } }
 }
 buildFeatures { aidl=true; buildConfig=true }
 sourceSets.getByName("main") { java.srcDir("src/skeleton/java"); res.srcDir("src/skeleton/res"); assets.srcDir("build/ovpnassets") }
 externalNativeBuild { cmake { path=file("src/main/cpp/CMakeLists.txt"); version="3.22.1" } }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
 packaging { jniLibs { useLegacyPackaging=true } }
}
dependencies { implementation("androidx.core:core:1.15.0"); implementation("androidx.annotation:annotation:1.9.1") }
