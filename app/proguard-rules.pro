# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.forma.app.**$$serializer { *; }
-keepclassmembers class com.forma.app.** { *** Companion; }
-keepclasseswithmembers class com.forma.app.** { kotlinx.serialization.KSerializer serializer(...); }
