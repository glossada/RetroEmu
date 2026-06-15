# LibretroDroid — mantener clases nativas
-keep class com.swordfish.libretrodroid.** { *; }

# Room — mantener entidades, DAOs y migraciones
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.migration.Migration { *; }
