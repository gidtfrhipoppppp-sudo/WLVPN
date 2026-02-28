# Retrofit
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes Signature
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Kotlin
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Koin
-keep class org.koin.** { *; }

# VPN Configs Models
-keep class com.example.wlvpn.data.models.** { *; }
-keep class com.example.wlvpn.data.api.** { *; }
