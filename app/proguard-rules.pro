# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin serialization companions
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# Keep engine hook entry points (filled in M2)
-keep class com.multi2.app.engine.** { *; }

# Keep proxy components (reflection-instantiated by the engine)
-keep class com.multi2.app.proxy.** { *; }
