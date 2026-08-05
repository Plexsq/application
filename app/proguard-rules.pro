# Keep Media3 / ExoPlayer reachable.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# NeutralOkHttp DNS + platform modules can hit missing members after R8.
-keepattributes Signature, *Annotation*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# kotlinx.serialization runtime.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class me.plexs.music.**$$serializer { *; }
-keepclassmembers class me.plexs.music.** {
    *** Companion;
}
-keepclasseswithmembers class me.plexs.music.** {
    kotlinx.serialization.KSerializer serializer(...);
}