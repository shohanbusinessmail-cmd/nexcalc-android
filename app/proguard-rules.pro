# NEXCALC release rules.
#
# The engine and data layer are plain Kotlin/Room code; R8's default Android rules handle them.
# Room's generated implementations are kept by the Room artifact consumer rules.
-keepattributes *Annotation*, InnerClasses
-dontwarn org.jetbrains.annotations.**

# kotlinx.serialization: keep serializers for exported/imported data.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
