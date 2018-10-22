# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/hojjatimani/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keep public class io.resana.Resana { public *; }
-keep public class io.resana.ResanaConfig { public *; }
-keep public class io.resana.NativeAd { public *; }
-keep public class io.resana.NativeAd$* { public *; }
-keep public class io.resana.SplashAdView { public *; }
-keep public class io.resana.SplashAdView$* { public *; }
-keep public class io.resana.DismissOption { public *; }