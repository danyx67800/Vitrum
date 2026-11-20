# Vitrum release rules.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.vitrum.tv.**$$serializer { *; }
-keepclassmembers class com.vitrum.tv.** {
    *** Companion;
}
-keepclasseswithmembers class com.vitrum.tv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data enum names for settings JSON stability.
-keepclassmembers enum com.vitrum.tv.** { *; }

# Keep names referenced by the hand-authored startup baseline profile.
-keepnames class com.vitrum.tv.VitrumApp
-keepnames class com.vitrum.tv.MainActivity
-keepnames class com.vitrum.tv.ui.MainViewModel

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
