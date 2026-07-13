# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
#
# Most third-party libraries here (Retrofit, Moshi, Room, Hilt, Firebase, Media3, Coil,
# kotlinx-coroutines) ship their own consumer R8 rules, so this file only adds keeps for the
# app-specific reflective surfaces R8 cannot infer in full mode (O1).

# Keep line numbers for readable crash stack traces, then hide the original source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Attributes required by reflective serialization (Retrofit / Moshi / Room) ----
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-keepattributes InnerClasses,EnclosingMethod,Exceptions

# ---- Retrofit ----
# Retrofit builds the API from a runtime dynamic proxy, reading the @GET/@POST/@Query/@Body/@Header
# annotations off the interface — keep the interface and its method/parameter annotations intact.
-keep,allowobfuscation interface com.example.newsapp.Api.PulseBackendApi { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# ---- Moshi ----
# Codegen emits a *JsonAdapter per @JsonClass type (reflection-free), but Moshi still loads those
# adapters by name and instantiates the model classes — keep both. Custom adapters
# (VerificationStatusAdapter, TelemetryEventAdapter) are retained automatically because
# provideMoshi() references them.
-keep class **JsonAdapter { *; }
-keepnames @com.squareup.moshi.JsonClass class *
-keepclassmembers @com.squareup.moshi.JsonClass class * { <init>(...); <fields>; }
-dontwarn com.squareup.moshi.**

# App types serialized by Moshi (wire DTOs + the domain types persisted through the Room
# TypeConverter). Keeping members defensively guarantees field-name fidelity under full-mode R8.
-keep class com.example.newsapp.data.remote.dto.** { *; }
-keep class com.example.newsapp.module.Source { *; }
-keep class com.example.newsapp.domain.model.Provenance { *; }

# Sealed VerificationStatus round-trips by `name` via VerificationStatusAdapter — keep the
# interface and its object subtypes so valueOf/name resolution survives.
-keep class com.example.newsapp.domain.model.VerificationStatus { *; }
-keep class com.example.newsapp.domain.model.VerificationStatus$* { *; }

# ---- okio (backs Moshi valueSink()/nextSource() in the telemetry raw-JSON passthrough) ----
-dontwarn okio.**

# ---- Jsoup (reader-mode HTML scraping; accessed via its public API only) ----
-dontwarn org.jsoup.**
