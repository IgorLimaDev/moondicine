# Add project specific ProGuard rules here.

# ===== Gson =====
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all Gson-serialized model classes (fields + class names)
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.moondicine.app.data.update.UpdateModelsKt** { *; }
-keep,allowobfuscation,allowshrinking class com.moondicine.app.data.remote.SupabaseModelsKt** { *; }
-keep,allowobfuscation,allowshrinking class com.moondicine.app.ai.models.** { *; }

# Keep all Supabase & Update model inner classes
-keep class com.moondicine.app.data.remote.** { *; }
-keep class com.moondicine.app.data.update.** { *; }
-keep class com.moondicine.app.ai.models.** { *; }

# ===== Retrofit =====
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep API interface methods with their generic return type signatures
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep all app API interfaces (Retrofit + Supabase + Cohere)
-keep,allowobfuscation interface com.moondicine.app.data.remote.** { *; }
-keep,allowobfuscation interface com.moondicine.app.ai.** { *; }

# ===== Room =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.moondicine.app.data.database.entity.** { *; }
-keep class com.moondicine.app.data.database.dao.** { *; }
-keep class com.moondicine.app.data.database.** { *; }
-dontwarn androidx.room.paging.**

# ===== PDFBox =====
-keep class org.apache.pdfbox.** { *; }
-dontwarn org.apache.pdfbox.**
-dontwarn com.gemalto.jp2.**

# ===== Kotlin Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
