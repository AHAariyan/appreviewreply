# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class app.appreviewreply.**$$serializer { *; }
-keepclassmembers class app.appreviewreply.** { *** Companion; }
-keepclasseswithmembers class app.appreviewreply.** { kotlinx.serialization.KSerializer serializer(...); }
