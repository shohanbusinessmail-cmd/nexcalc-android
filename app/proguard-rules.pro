# NEXCALC release configuration.
# Kotlin, Compose, Room, Navigation and kotlinx.serialization ship consumer rules; these entries
# cover the app's own data classes used by JSON export/import and by navigation routes.
-keepattributes *Annotation*, InnerClasses
-dontwarn org.jetbrains.annotations.**

-keepclassmembers @kotlinx.serialization.Serializable class com.shohankhan.nexcalc.** {
    *** Companion;
    *** serializer(...);
}
-keepclasseswithmembers class com.shohankhan.nexcalc.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room generated implementations are referenced directly; keep entity accessors for reflection-free
# use by the generated code.
-keep class com.shohankhan.nexcalc.core.data.local.** { *; }
