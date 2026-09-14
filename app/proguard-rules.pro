-keep class com.hairsalon.manager.data.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
