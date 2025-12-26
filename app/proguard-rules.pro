# ==================== EXISTING RULES ====================
# Add project specific ProGuard rules here.

# ==================== SECURITY: Obfuscate HMAC ====================
# Obfuscate toàn bộ network package
-keep class com.nguyendevs.ecolens.network.HMACInterceptor {
    public <methods>;
}
-keepclassmembers class com.nguyendevs.ecolens.network.HMACInterceptor {
    private static final java.lang.String APP_SECRET;
}

# Obfuscate string constants
-keepclassmembers class com.nguyendevs.ecolens.network.** {
    private static final java.lang.String *;
}

# ==================== STRING ENCRYPTION ====================
# Dùng với StringFog hoặc DexGuard (nếu có)
# https://github.com/MegatronKing/StringFog

# ==================== RETROFIT & OKHTTP ====================
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

-keep class retrofit2.** { *; }
-keepclassmembernames interface retrofit2.** { *; }

# ==================== GSON ====================
-keep class com.google.gson.** { *; }
-keep class com.nguyendevs.ecolens.api.** { *; }

# ==================== GENERAL ====================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile