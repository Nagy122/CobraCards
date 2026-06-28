# ═══════════════════════════════════════
# 🔒 COBRA CARDS - SECURITY RULES
# ═══════════════════════════════════════

# ── Keep Core Classes ──
-keep class com.cobra.cards.model.** { *; }
-keep class com.cobra.cards.api.** { *; }
-keep class com.cobra.cards.repository.** { *; }
-keep class com.cobra.cards.utils.SessionManager { *; }
-keep class com.cobra.cards.utils.SecurityManager { *; }
-keep class com.cobra.cards.ui.** { *; }
-keep class com.cobra.cards.CobraApp { *; }

# ── Obfuscation ──
-repackageclasses 'a'
-allowaccessmodification
-optimizationpasses 5

# ── Remove Debug Info ──
-dontusemixedcaseclassnames
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# ── Retrofit ──
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-dontwarn com.google.gson.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── Kotlin ──
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.Metadata { *; }

# ── Native Methods ──
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── Enums ──
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Verbose ──
-verbose

