# kotlinx.serialization keeps the generated serializers on the serializable types.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.vh.health.core.content.** {
    *** Companion;
}
-keepclasseswithmembers class com.vh.health.core.content.** {
    kotlinx.serialization.KSerializer serializer(...);
}
