# ---- Kotlin / metadata ----
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ---- Compose (compiler generates its own consumer rules; keep it lean) ----
-dontwarn androidx.compose.**

# ---- Kotlin serialization ----
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-dontwarn kotlinx.serialization.**

# ---- Hilt (generated components; plugin adds its own keeps) ----
-dontwarn dagger.hilt.**
-keep class *_HiltModules* { *; }
-keep class *HiltWrapper_* { *; }

# ---- Room ----
-dontwarn androidx.room.**
-keep class *Impl extends androidx.room.RoomDatabase { *; }

# ---- DataStore ----
-dontwarn androidx.datastore.**

# ---- Engine hook entry points (reflection) ----
-keep class com.multi2.app.engine.** { *; }

# ---- Proxy components: manifest-declared, reflection-instantiated ----
-keep class com.multi2.app.proxy.** { *; }

# ---- Reflection targets used by the guest runtime ----
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn dalvik.system.**

# ---- ZXing ----
-dontwarn com.google.zxing.**

# ---- OkHttp / Retrofit ----
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**
