plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
if (file("google-services.json").exists()) apply(plugin="com.google.gms.google-services")
android {
 ndkVersion="28.0.13004108"
 namespace="com.b5studios.usafavor"; compileSdk=35
 defaultConfig { applicationId="com.b5studios.usafavor"; minSdk=26; targetSdk=35; versionCode=2; versionName="1.1.0-spark" }
 signingConfigs { create("production") {
  System.getenv("B5_KEYSTORE_FILE")?.let { storeFile=file(it) }
  storePassword=System.getenv("B5_KEYSTORE_PASSWORD")
  keyAlias=System.getenv("B5_KEY_ALIAS")
  keyPassword=System.getenv("B5_KEY_PASSWORD")
 } }
 buildTypes {
  getByName("debug") { isMinifyEnabled=true; isShrinkResources=true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro") }
  getByName("release") { signingConfig=signingConfigs.getByName("production"); isMinifyEnabled=true; isShrinkResources=true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro") }
 }
 buildFeatures { compose=true; buildConfig=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
 packaging { resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1"); jniLibs.useLegacyPackaging=true }
}
dependencies { implementation(project(":Shared")) }

val validateProduction by tasks.registering {
 doLast {
  check(file("google-services.json").exists()) { "Matching Firebase Android registration is required" }
  listOf("B5_KEYSTORE_FILE","B5_KEYSTORE_PASSWORD","B5_KEY_ALIAS","B5_KEY_PASSWORD").forEach {
   check(!System.getenv(it).isNullOrBlank()) { "Missing production signing variable: $it" }
  }
 }
}
tasks.matching { it.name=="preReleaseBuild" }.configureEach { dependsOn(validateProduction) }
