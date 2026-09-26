# Proguard rules for Share Contact App
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class com.google.zxing.** { *; }
