# ShareCard Proguard / R8 Optimization Rules

# Preserve annotations and inner classes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep ZXing QR Code Generation classes
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlinx Serialization Rules
-dontnote kotlinx.serialization.SerializationKt

# Keep companion objects that provide serializer()
-keepclassmembers class * {
    *** Companion;
}

# Keep serializer methods
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializable classes and fields with @SerialName
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# AndroidX Glance AppWidget
-keep class androidx.glance.** { *; }
-keep class com.sharecontact.app.widget.** { *; }

# Coroutines ServiceLoader & Exception Handlers
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
