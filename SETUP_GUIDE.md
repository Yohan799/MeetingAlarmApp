# Quick Setup Guide - Meeting Alarm App

## Step-by-Step Implementation

### Phase 1: Google Cloud Setup (15 minutes)

1. **Create Google Cloud Project**
   ```
   - Visit: https://console.cloud.google.com/
   - Click "New Project"
   - Name: "Meeting Alarm App"
   - Note the Project ID
   ```

2. **Enable APIs**
   ```
   Navigate to: APIs & Services > Library
   Enable:
   ✓ Google Calendar API
   ```


3. **Configure OAuth Consent**
   ```
   Navigate to: APIs & Services > OAuth consent screen
   
   Configuration:
   - User Type: External
   - App name: Meeting Alarm
   - Add scopes:
     * .../auth/userinfo.email
     * .../auth/userinfo.profile  
     * .../auth/calendar.readonly
   ```

4. **Create OAuth Credentials**
   ```
   Navigate to: APIs & Services > Credentials
   
   Create:
   - Type: OAuth 2.0 Client ID
   - Application type: Android
   - Package name: com.alarmify.meetings
   - SHA-1: Get using command below
   ```

5. **Get SHA-1 Fingerprint**
   ```bash
   # Debug (for development)
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # Look for SHA-1 line like:
   # SHA1: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD
   ```

6. **Download google-services.json**
   - Click on created credentials
   - Download JSON file
   - Place in `app/` directory

### Phase 2: Android Studio Setup (10 minutes)

1. **Create New Project**
   ```
   - Open Android Studio
   - New Project > Empty Activity
   - Name: MeetingAlarmApp
   - Package: com.meetingalarm.app
   - Language: Kotlin
   - Minimum SDK: API 26 (Android 8.0)
   ```

2. **Add Dependencies**
   Add to `app/build.gradle`:
   ```gradle
   plugins {
       id 'com.google.gms.google-services'
   }
   
   dependencies {
       // Core
       implementation 'androidx.core:core-ktx:1.12.0'
       implementation 'androidx.appcompat:appcompat:1.6.1'
       implementation 'com.google.android.material:material:1.11.0'
       
       // Google Sign-In
       implementation 'com.google.android.gms:play-services-auth:20.7.0'
       
       // Google Calendar API
       implementation 'com.google.api-client:google-api-client-android:2.2.0'
       implementation 'com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0'
   }
   ```

3. **Add google-services plugin to project-level build.gradle**
   ```gradle
   buildscript {
       dependencies {
           classpath 'com.google.gms:google-services:4.4.0'
       }
   }
   ```

### Phase 3: Copy Source Files (20 minutes)

Copy all provided files to your project:

**Kotlin Files:**
```
app/src/main/java/com/meetingalarm/app/
├── MeetingAlarmApplication.kt
├── ui/
│   ├── splash/SplashActivity.kt
│   ├── auth/SignInActivity.kt
│   ├── main/MainActivity.kt
│   ├── alarm/AlarmNotificationActivity.kt
│   └── adapter/EventsAdapter.kt
├── alarm/
│   ├── NativeAlarmManager.kt ⭐ CORE FEATURE
│   ├── AlarmReceiver.kt
│   ├── AlarmService.kt
│   └── BootReceiver.kt
└── data/
    ├── model/CalendarEvent.kt
    └── repository/CalendarRepository.kt
```

**Layout Files:**
```
app/src/main/res/layout/
├── activity_splash.xml
├── activity_sign_in.xml
├── activity_main.xml
├── activity_alarm_notification.xml
└── item_event.xml
```

**Resource Files:**
```
app/src/main/res/values/
├── colors.xml
├── strings.xml
└── themes.xml
```

**Other Files:**
```
app/src/main/
├── AndroidManifest.xml
└── proguard-rules.pro
```

### Phase 4: Create Drawable Resources (15 minutes)

Create vector drawables in `res/drawable/`:

1. **ic_alarm_logo.xml** (App icon)
   ```xml
   <vector android:height="24dp" android:tint="#FFFFFF"
       android:viewportHeight="24" android:viewportWidth="24"
       android:width="24dp" xmlns:android="http://schemas.android.com/apk/res/android">
       <path android:fillColor="@android:color/white" 
             android:pathData="M12,2C6.5,2 2,6.5 2,12s4.5,10 10,10 10,-4.5 10,-10S17.5,2 12,2zM16.2,16.2L11,13L11,7h1.5v5.2l4.5,2.7 -0.8,1.3z"/>
   </vector>
   ```

2. **ic_alarm_ringing.xml** (Ringing alarm)
   ```xml
   <vector android:height="24dp" android:tint="#FFFFFF"
       android:viewportHeight="24" android:viewportWidth="24"
       android:width="24dp" xmlns:android="http://schemas.android.com/apk/res/android">
       <path android:fillColor="@android:color/white" 
             android:pathData="M22,5.7l-4.6,-3.9 -1.3,1.5 4.6,3.9L22,5.7zM7.9,3.4L6.6,1.9 2,5.7l1.3,1.5 4.6,-3.8zM12.5,8L11,8v6l4.7,2.9 0.8,-1.2 -4,-2.4L12.5,8zM12,4c-5,0 -9,4 -9,9s4,9 9,9 9,-4 9,-9 -4,-9 -9,-9zM12,20c-3.9,0 -7,-3.1 -7,-7s3.1,-7 7,-7 7,3.1 7,7 -3.1,7 -7,7z"/>
   </vector>
   ```

3. **Other icons** - Use Material Design icons from:
   - ic_refresh.xml
   - ic_logout.xml
   - ic_time.xml
   - ic_location.xml
   - ic_dismiss.xml

### Phase 5: Configure AndroidManifest.xml

Ensure all activities and permissions are declared (already provided in the file).

### Phase 6: Build and Test (30 minutes)

1. **Sync Gradle Files**
   ```
   - Click "Sync Project with Gradle Files"
   - Wait for dependencies to download
   ```

2. **Connect Device/Emulator**
   ```
   - Enable USB debugging on Android device
   - Or start Android Emulator with Google Play
   ```

3. **Install Google Play Services**
   ```
   - Ensure device has Google Play Services
   - Sign in with Google account on device
   ```

4. **Run the App**
   ```
   - Click Run button or Shift+F10
   - Select your device
   - Wait for installation
   ```

5. **Test Flow**
   ```
   ✓ Splash screen appears
   ✓ Sign in with Google works
   ✓ Permissions are requested and granted
   ✓ Calendar events load
   ✓ Can set alarm on an event
   ✓ Alarm triggers at scheduled time
   ✓ Full screen alarm appears
   ✓ Sound and vibration work
   ✓ Dismiss/Snooze buttons work
   ```

## Common Issues and Solutions

### Issue: Google Sign-In Fails
**Solution:**
- Verify SHA-1 fingerprint is correct
- Check package name matches exactly
- Ensure google-services.json is in app/ folder
- Check OAuth consent screen is configured

### Issue: Calendar Events Not Loading
**Solution:**
- Grant calendar permission in app settings
- Ensure device has Google Calendar with events
- Check internet connection
- Verify Calendar API is enabled in Cloud Console

### Issue: Alarms Not Triggering
**Solution:**
- Go to Settings > Apps > Meeting Alarm > Permissions
- Enable "Alarms & reminders" permission (Android 12+)
- Disable battery optimization for the app
- Check Do Not Disturb settings

### Issue: Alarm Not Showing on Lock Screen
**Solution:**
- Enable "Display over other apps" permission
- Grant notification permission (Android 13+)
- Check lock screen notification settings

## Testing Checklist

### Functional Tests
- [ ] App launches without crashes
- [ ] Splash screen displays for 2 seconds
- [ ] Google Sign-In flow completes successfully
- [ ] Calendar events are fetched and displayed
- [ ] Can set default alarm times (5, 10, 15, 30, 60 min)
- [ ] Can set custom alarm time
- [ ] Alarm badge shows on event card
- [ ] Can cancel alarm
- [ ] Pull to refresh updates events list

### Alarm Tests
- [ ] Alarm triggers at exact scheduled time
- [ ] Full-screen activity appears
- [ ] Alarm sound plays continuously
- [ ] Device vibrates with pattern
- [ ] Shows on lock screen
- [ ] Turns screen on
- [ ] Dismiss button stops alarm
- [ ] Snooze button reschedules for 5 min
- [ ] Multiple alarms can coexist

### Persistence Tests
- [ ] Alarms survive app restart
- [ ] Alarms survive device reboot
- [ ] User remains signed in after app restart

### Permission Tests
- [ ] Calendar permission is requested
- [ ] Notification permission is requested (Android 13+)
- [ ] Exact alarm permission is requested (Android 12+)
- [ ] App handles permission denial gracefully

## Performance Tips

1. **Optimize Calendar Queries**
   - Fetch only necessary fields
   - Use appropriate date ranges
   - Cache results when possible

2. **Battery Efficiency**
   - Use setAlarmClock() for exact timing
   - Minimize wake locks
   - Use foreground service only when needed

3. **Memory Management**
   - Release MediaPlayer resources
   - Cancel alarms properly
   - Clean up receivers

## Production Checklist

Before releasing to Play Store:

- [ ] Create release keystore
- [ ] Add release SHA-1 to Google Cloud Console
- [ ] Update app version in build.gradle
- [ ] Add ProGuard rules
- [ ] Test on multiple devices and Android versions
- [ ] Create privacy policy
- [ ] Prepare Play Store listing
- [ ] Add app screenshots
- [ ] Write detailed app description

## Support Resources

- [Google Calendar API Docs](https://developers.google.com/calendar)
- [Android AlarmManager Guide](https://developer.android.com/training/scheduling/alarms)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Android Permissions](https://developer.android.com/guide/topics/permissions/overview)

---

**Estimated Total Setup Time: 90 minutes**

For questions or issues, refer to the main README.md file.
