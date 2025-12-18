# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Suppress warnings for common libraries
-dontwarn com.google.**
-dontwarn org.apache.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.**

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Suppress warnings for reflection and serialization
-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**

# Keep Room database classes
-keep class androidx.room.** { *; }

# MPAndroidChart rules
-keep class com.github.mikephil.charting.** { *; }

# Glide rules
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
