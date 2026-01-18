# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Model sınıflarınızı koruyun
-keep class com.hasanege.materialtv.model.** { *; }
-keep class com.hasanege.materialtv.network.** { *; }

# LibVLC rules
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.vlc.** { *; }
-keep interface org.videolan.libvlc.** { *; }
-keep class org.videolan.libvlc.interfaces.** { *; }
-dontwarn org.videolan.libvlc.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keepclassmembers class com.hasanege.materialtv.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.hasanege.materialtv.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response