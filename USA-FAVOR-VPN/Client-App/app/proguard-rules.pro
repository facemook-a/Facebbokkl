-keep class com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory { public static *** getInstance(...); *; }
-keep class de.blinkt.openvpn.VpnProfile { *; }
-keep class de.blinkt.openvpn.core.** { *; }
-keep class de.blinkt.openvpn.api.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

-keep class com.usafavor.client.AuraProvider { public *; }
