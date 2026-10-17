# BiliPai release R8 configuration.
# Keep the configuration intentionally minimal so R8 can shrink, optimize and
# obfuscate all statically reachable app and library code.

# Runtime reflection metadata used by Retrofit/kotlinx.serialization and
# framework callbacks. These preserve metadata only; they do not keep classes.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# WorkManager startup compatibility workaround for crash dfcc3eef05f5b473fa7d2c78264b72f7.
# Preserve the SDK_INT >= 34 branch and its isolated API-34 implementation;
# do not inline/merge/optimize these boundaries into startup callers. Keep names
# obfuscatable and unused code shrinkable. This does not handle ROMs that report
# API 34+ while lacking JobScheduler.forNamespace on its own. The namespace guard
# plugin adds a missing-API fallback inside JobScheduler34; keep that boundary intact too.
-keep,allowshrinking,allowobfuscation class androidx.work.impl.background.systemjob.JobSchedulerExtKt {
    *;
}
-keep,allowshrinking,allowobfuscation class androidx.work.impl.background.systemjob.JobScheduler34 {
    *;
}
-keepclassmembers,allowshrinking,allowobfuscation class androidx.work.impl.background.systemjob.SystemJobScheduler {
    public static void cancelAllInAllNamespaces(android.content.Context);
}

# Optional classes referenced by third-party libraries on specific code paths.
# Consumer rules supplied by each dependency remain authoritative.
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn dev.chrisbanes.haze.**
-dontwarn io.github.alexzhirkevich.cupertino.**
-dontwarn androidx.room.paging.**
-dontwarn androidx.media3.**
-dontwarn coil3.**
-dontwarn com.google.zxing.**
-dontwarn org.fourthline.cling.**
-dontwarn javax.enterprise.context.**
-dontwarn javax.inject.**
-dontwarn org.seamless.**

# slf4j-api 1.7.x (pulled in transitively by readability4j) looks up its logging
# binding reflectively from LoggerFactory's static initializer. This app bundles
# no binding, so SLF4J deliberately falls back to the NOP logger; tell R8 that the
# absent binding class is expected instead of keeping a dead implementation.
-dontwarn org.slf4j.impl.StaticLoggerBinder

# WebView JavaScript bridges are discovered by annotation at runtime.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
