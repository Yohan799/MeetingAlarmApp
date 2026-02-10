# Meeting Alarm App - Android Native Application

A native Android application that helps you never miss a meeting by setting alarms for your Google Calendar events.

## 🎯 Features

### Core Features
- **Splash Screen**: Beautiful animated splash screen on app launch
- **Google Sign-In**: Secure authentication with Google account
- **Google Calendar Integration**: Automatically fetches upcoming calendar events
- **Native Alarm System**: Uses Android's native AlarmManager for exact alarm timing
- **Default Alarm Times**: Quick preset options (5, 10, 15, 30, 60 minutes before meeting)
- **Custom Alarm Times**: Set custom reminder times for any event
- **Full-Screen Alarm Notifications**: Wake up screen and show over lock screen
- **Alarm Sound & Vibration**: Native alarm sound with vibration pattern
- **Snooze Functionality**: Snooze alarms for 5 minutes
- **Boot Persistence**: Alarms are rescheduled after device reboot

## 📋 Requirements

- Android SDK 26 (Android 8.0 Oreo) or higher
- Android Studio (latest version recommended)
- Google account for testing
- Physical device or emulator with Google Play Services

## 🛠️ Setup Instructions

### 1. Google Cloud Console Setup

#### Create a New Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Note your project ID

#### Enable Required APIs
1. In the Google Cloud Console, go to **APIs & Services** > **Library**
2. Enable the following APIs:
   - **Google Calendar API**
   - **Google Sign-In API**

#### Configure OAuth Consent Screen
1. Go to **APIs & Services** > **OAuth consent screen**
2. Choose **External** user type (or Internal if using Google Workspace)
3. Fill in the required information:
   - App name: "Meeting Alarm"
   - User support email: Your email
   - Developer contact email: Your email
4. Add scopes:
   - `../auth/userinfo.email`
   - `../auth/userinfo.profile`
   - `../auth/calendar.readonly`
5. Save and continue

#### Create OAuth 2.0 Credentials
1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **OAuth 2.0 Client ID**
3. Select **Android** as application type
4. Fill in:
   - **Name**: "Meeting Alarm Android"
   - **Package name**: `com.meetingalarm.app`
   - **SHA-1 certificate fingerprint**: Get it using the command below

#### Get SHA-1 Fingerprint
```bash
# For debug keystore (development)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# For release keystore (production)
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias
```

5. Copy the SHA-1 fingerprint and paste it in the credentials setup
6. Click **Create**
7. Download the `google-services.json` file

### 2. Project Setup

#### Clone and Import
1. Clone this repository or create a new Android project
2. Copy all the source files to appropriate directories

#### Add google-services.json
1. Place the downloaded `google-services.json` file in the `app/` directory of your project
2. **Important**: This file contains your OAuth credentials

#### Update build.gradle (Project level)
```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

#### Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/meetingalarm/app/
│   │   │   ├── MeetingAlarmApplication.kt
│   │   │   ├── ui/
│   │   │   │   ├── splash/
│   │   │   │   │   └── SplashActivity.kt
│   │   │   │   ├── auth/
│   │   │   │   │   └── SignInActivity.kt
│   │   │   │   ├── main/
│   │   │   │   │   └── MainActivity.kt
│   │   │   │   ├── alarm/
│   │   │   │   │   └── AlarmNotificationActivity.kt
│   │   │   │   └── adapter/
│   │   │   │       └── EventsAdapter.kt
│   │   │   ├── alarm/
│   │   │   │   ├── NativeAlarmManager.kt
│   │   │   │   ├── AlarmReceiver.kt
│   │   │   │   ├── AlarmService.kt
│   │   │   │   └── BootReceiver.kt
│   │   │   └── data/
│   │   │       ├── model/
│   │   │       │   └── CalendarEvent.kt
│   │   │       └── repository/
│   │   │           └── CalendarRepository.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_splash.xml
│   │   │   │   ├── activity_sign_in.xml
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_alarm_notification.xml
│   │   │   │   └── item_event.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/
│   │   │       └── (icons and drawables)
│   │   └── AndroidManifest.xml
│   └── build.gradle
└── google-services.json
```

### 3. Required Drawables

Create the following drawable resources (use vector drawables or PNGs):

- `ic_alarm_logo.xml` - App logo for splash and sign-in
- `ic_alarm.xml` - Alarm icon for notifications
- `ic_alarm_ringing.xml` - Animated alarm icon for alarm screen
- `ic_refresh.xml` - Refresh icon
- `ic_logout.xml` - Logout icon
- `ic_time.xml` - Time icon
- `ic_location.xml` - Location icon
- `ic_dismiss.xml` - Dismiss button icon

### 4. Permissions Configuration

The app requires the following permissions (already in AndroidManifest.xml):

```xml
<!-- Required for network calls -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Required for calendar access -->
<uses-permission android:name="android.permission.READ_CALENDAR" />

<!-- Required for exact alarms (Android 12+) -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

<!-- Required for notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Required for alarm functionality -->
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
```

## 🚀 Running the App

### Development Build
1. Connect your Android device or start an emulator
2. Make sure Google Play Services is installed on the device
3. In Android Studio, click **Run** or use:
```bash
./gradlew installDebug
```

### Testing the App
1. Launch the app - you'll see the splash screen
2. Sign in with your Google account
3. Grant calendar and notification permissions
4. The app will fetch your upcoming calendar events
5. Tap "Set Alarm" on any event
6. Choose a preset time or enter a custom time
7. The alarm will trigger at the scheduled time

## 🔧 Key Components Explained

### 1. Native Alarm Manager (`NativeAlarmManager.kt`)
This is the **CORE FEATURE** of the app. It uses Android's native `AlarmManager` API to schedule exact alarms:

```kotlin
// Uses setAlarmClock() for exact timing
alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
```

**Key features:**
- Uses `AlarmManager.setAlarmClock()` for exact timing
- Fallback to `setExactAndAllowWhileIdle()` if needed
- Shows alarm in system status bar
- Wakes device from sleep

### 2. Alarm Service (`AlarmService.kt`)
Handles the alarm trigger with:
- Foreground service for reliability
- Native alarm sound using `RingtoneManager`
- Vibration pattern
- Full-screen notification activity

### 3. Calendar Repository (`CalendarRepository.kt`)
Fetches events from native Android Calendar using:
- `CalendarContract.Events` content provider
- Proper permission handling
- Efficient cursor management

### 4. Alarm Receiver (`AlarmReceiver.kt`)
BroadcastReceiver that:
- Listens for alarm triggers
- Starts AlarmService
- Passes event data to service

### 5. Boot Receiver (`BootReceiver.kt`)
Reschedules alarms after device reboot:
- Listens for `BOOT_COMPLETED` broadcast
- Restores all saved alarms
- Ensures no alarms are missed

## 📱 User Flow

1. **App Launch** → Splash Screen (2 seconds)
2. **Authentication Check**:
   - If not signed in → Sign In Activity
   - If signed in → Main Activity
3. **Main Activity**:
   - Fetches calendar events
   - Displays events in RecyclerView
   - User can set/cancel alarms
4. **Alarm Trigger**:
   - AlarmReceiver receives broadcast
   - Starts AlarmService (foreground)
   - Shows AlarmNotificationActivity
   - Plays alarm sound + vibration
5. **User Actions**:
   - Dismiss → Stops alarm
   - Snooze → Reschedules for 5 min later

## 🔐 Security Considerations

1. **OAuth 2.0**: Secure authentication with Google
2. **Permission Runtime Checks**: All permissions checked at runtime
3. **Secure Data Storage**: Use SharedPreferences or Room for alarm persistence
4. **API Key Security**: Keep `google-services.json` secure, add to `.gitignore`

## 🐛 Troubleshooting

### Google Sign-In Fails
- Verify SHA-1 fingerprint matches
- Check package name is correct
- Ensure Google Play Services is updated
- Verify `google-services.json` is in `app/` directory

### Alarms Not Triggering
- Check if exact alarm permission is granted (Android 12+)
- Verify battery optimization is disabled for the app
- Ensure the app has notification permission (Android 13+)

### Calendar Events Not Loading
- Grant calendar permission
- Check if Google Calendar app has events
- Verify account has calendar data

### Alarm Not Showing on Lock Screen
- Check "Display over other apps" permission
- Verify notification permission is granted
- Ensure "Do Not Disturb" is not blocking alarms

## 📊 Testing Checklist

- [ ] Splash screen displays correctly
- [ ] Google Sign-In works
- [ ] Calendar events are fetched
- [ ] Can set alarm with default times
- [ ] Can set alarm with custom time
- [ ] Alarm triggers at correct time
- [ ] Alarm sound plays
- [ ] Device vibrates
- [ ] Full-screen notification shows
- [ ] Dismiss button stops alarm
- [ ] Snooze button reschedules alarm
- [ ] Alarms persist after app restart
- [ ] Alarms reschedule after device reboot

## 🔄 Future Enhancements

1. **Room Database**: Persist alarm settings locally
2. **Multiple Alarm Sounds**: Let users choose custom alarm tones
3. **Recurring Alarms**: Support for recurring events
4. **Widget**: Home screen widget showing next meeting
5. **Dark Mode**: Full dark theme support
6. **Analytics**: Track alarm usage and effectiveness
7. **Smart Snooze**: Intelligent snooze times based on meeting duration
8. **Calendar Sync**: Two-way sync with Google Calendar

## 📄 License

This project is created for educational purposes. Ensure you comply with Google's terms of service when using their APIs.

## 🤝 Contributing

Contributions are welcome! Please ensure:
1. Code follows Kotlin style guide
2. All permissions are properly handled
3. Native alarm functionality is maintained
4. Proper error handling is implemented

## 📞 Support

For issues or questions:
1. Check existing GitHub issues
2. Review Google Calendar API documentation
3. Consult Android AlarmManager documentation

---

**Built with ❤️ using Android Native Development**
