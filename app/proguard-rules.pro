# Add project specific ProGuard rules here.

# Keep Google Sign-In classes
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Google API Client classes
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

# Keep Calendar API classes
-keep class com.google.api.services.calendar.** { *; }
-dontwarn com.google.api.services.calendar.**

# Keep model classes
-keep class com.meetingalarm.app.data.model.** { *; }

# Keep AlarmManager related classes
-keep class com.meetingalarm.app.alarm.** { *; }

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
