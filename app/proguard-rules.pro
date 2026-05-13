# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

# Compose
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }

# Media3
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.session.** { *; }

# Coil
-keep class coil.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, EnclosingMethod, InnerClasses
-keep class kotlinx.serialization.json.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}

# Chaquopy (if still needed for other features)
-keep class com.chaquo.python.** { *; }

# SyncTax Models
-keep class com.just_for_fun.synctax.core.ml.models.** { *; }
-keep class com.just_for_fun.synctax.data.local.entities.** { *; }
-keep class com.just_for_fun.synctax.presentation.viewmodels.** { *; }

# Optimization
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*