# Cunnis - Rabbit Farm Manager
# ProGuard Rules

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Keep model classes
-keep class cu.dandroid.cunnis.data.local.db.entity.** { *; }
-keep class cu.dandroid.cunnis.data.model.** { *; }
