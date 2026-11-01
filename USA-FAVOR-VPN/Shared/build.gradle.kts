plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
if (providers.gradleProperty("visualReview").orNull == "true") apply(plugin="app.cash.paparazzi")
android {
 namespace="com.b5studios.shared"; compileSdk=35
 defaultConfig { minSdk=26 }
 buildFeatures { compose=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies {
 testImplementation("app.cash.paparazzi:paparazzi:1.3.5")
 testImplementation("junit:junit:4.13.2")
 api(platform("androidx.compose:compose-bom:2025.02.00"))
 api("androidx.compose.material3:material3")
 api("androidx.compose.material:material-icons-extended")
 api("androidx.activity:activity-compose:1.10.1")
 api("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 api("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
 api("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")
 api(platform("com.google.firebase:firebase-bom:33.10.0"))
 api("com.google.firebase:firebase-auth")
 api("com.google.firebase:firebase-functions")
 api("com.google.firebase:firebase-firestore")
 api("com.google.firebase:firebase-appcheck-playintegrity")
 debugApi("com.google.firebase:firebase-appcheck-debug")
}
