# Keep Firestore / Room / Cloudinary models
-keep class com.kurupdevs.mynotes.data.** { *; }
-keepclassmembers class com.kurupdevs.mynotes.data.** { *; }
# Keep serialization
-keepattributes *Annotation*, InnerClasses, Signature
-dontwarn com.google.mlkit.**
