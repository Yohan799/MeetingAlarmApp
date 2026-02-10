# Quick Code Reference - Meeting Alarm App

## Essential Code Snippets

### 1. Setting an Exact Alarm (CORE FEATURE)

```kotlin
// NativeAlarmManager.kt
fun setAlarm(event: CalendarEvent, minutesBefore: Int) {
    val alarmTime = event.startTime - (minutesBefore * 60 * 1000)
    
    // Don't set alarm if time has passed
    if (alarmTime <= System.currentTimeMillis()) {
        return
    }

    val intent = Intent(context, AlarmReceiver::class.java).apply {
        action = "com.meetingalarm.app.ALARM_TRIGGER"
        putExtra(EXTRA_EVENT_ID, event.id)
        putExtra(EXTRA_EVENT_TITLE, event.title)
        putExtra(EXTRA_EVENT_START_TIME, event.startTime)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        event.id.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmClockInfo = AlarmManager.AlarmClockInfo(alarmTime, pendingIntent)
    
    // This is the key method for exact alarms
    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
}
```

### 2. Receiving Alarm Trigger

```kotlin
// AlarmReceiver.kt
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == "com.meetingalarm.app.ALARM_TRIGGER") {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE)
        
        // Start foreground service to handle alarm
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, eventTitle)
        }
        
        context.startForegroundService(serviceIntent)
    }
}
```

### 3. Playing Alarm Sound

```kotlin
// AlarmService.kt
private fun playAlarmSound() {
    try {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, alarmUri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to play alarm sound: ${e.message}")
    }
}
```

### 4. Vibration Pattern

```kotlin
// AlarmService.kt
private fun startVibration() {
    val pattern = longArrayOf(0, 500, 500, 500, 500) // off, on, off, on, off
    val effect = VibrationEffect.createWaveform(pattern, 0) // 0 = repeat
    vibrator?.vibrate(effect)
}
```

### 5. Fetching Calendar Events

```kotlin
// CalendarRepository.kt
suspend fun fetchUpcomingEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
    val events = mutableListOf<CalendarEvent>()
    
    val startMillis = System.currentTimeMillis()
    val endMillis = startMillis + (30L * 24 * 60 * 60 * 1000) // 30 days

    val projection = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND
    )

    val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
    val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
    val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

    val cursor = context.contentResolver.query(
        CalendarContract.Events.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )

    cursor?.use {
        while (it.moveToNext()) {
            val id = it.getString(0)
            val title = it.getString(1) ?: "Untitled"
            val startTime = it.getLong(2)
            val endTime = it.getLong(3)
            
            events.add(CalendarEvent(id, title, null, startTime, endTime, null))
        }
    }

    return@withContext events
}
```

### 6. Google Sign-In Setup

```kotlin
// SignInActivity.kt
private fun setupGoogleSignIn() {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
        .build()

    googleSignInClient = GoogleSignIn.getClient(this, gso)
}

private fun signIn() {
    val signInIntent = googleSignInClient.signInIntent
    signInLauncher.launch(signInIntent)
}
```

### 7. Checking Permissions

```kotlin
// MainActivity.kt
private fun checkExactAlarmPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!nativeAlarmManager.canScheduleExactAlarms()) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs permission to set exact alarms.")
                .setPositiveButton("Grant") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                }
                .show()
        }
    }
}
```

### 8. Full-Screen Notification Activity

```kotlin
// AlarmNotificationActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Show on lock screen and turn screen on
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
    
    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
    )
    
    // Rest of setup...
}
```

### 9. Creating Notification Channel

```kotlin
// MeetingAlarmApplication.kt
private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            ALARM_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = ALARM_CHANNEL_DESC
            enableVibration(true)
            setShowBadge(true)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
```

### 10. Foreground Service Notification

```kotlin
// AlarmService.kt
private fun createForegroundNotification(): Notification {
    val dismissIntent = Intent(this, AlarmService::class.java).apply {
        action = ACTION_DISMISS
    }
    val dismissPendingIntent = PendingIntent.getService(
        this, 0, dismissIntent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
        .setContentTitle("Meeting Alarm")
        .setContentText("Meeting alarm is active")
        .setSmallIcon(R.drawable.ic_alarm)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setOngoing(true)
        .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPendingIntent)
        .build()
}
```

### 11. Rescheduling After Reboot

```kotlin
// BootReceiver.kt
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
        CoroutineScope(Dispatchers.IO).launch {
            val alarmManager = NativeAlarmManager(context)
            // Fetch saved events with alarms from storage
            // alarmManager.rescheduleAllAlarms(events)
        }
    }
}
```

### 12. RecyclerView Adapter Binding

```kotlin
// EventsAdapter.kt
fun bind(event: CalendarEvent) {
    binding.tvEventTitle.text = event.title
    binding.tvEventTime.text = event.getFormattedStartTime()
    
    if (event.isAlarmSet) {
        binding.btnSetAlarm.text = "Alarm: ${event.alarmMinutesBefore} min"
        binding.btnSetAlarm.setBackgroundColor(
            ContextCompat.getColor(context, R.color.alarm_active)
        )
        binding.btnCancelAlarm.visibility = View.VISIBLE
    } else {
        binding.btnSetAlarm.text = "Set Alarm"
        binding.btnSetAlarm.setBackgroundColor(
            ContextCompat.getColor(context, R.color.alarm_inactive)
        )
        binding.btnCancelAlarm.visibility = View.GONE
    }
    
    binding.btnSetAlarm.setOnClickListener { onAlarmClick(event) }
    binding.btnCancelAlarm.setOnClickListener { onCancelAlarmClick(event) }
}
```

## AndroidManifest.xml Essential Permissions

```xml
<!-- Exact Alarms (Android 12+) -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

<!-- Calendar Access -->
<uses-permission android:name="android.permission.READ_CALENDAR" />

<!-- Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Alarm Functionality -->
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />

<!-- Reboot Persistence -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

## AndroidManifest.xml Essential Components

```xml
<!-- Alarm Receiver -->
<receiver
    android:name=".alarm.AlarmReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="com.meetingalarm.app.ALARM_TRIGGER" />
    </intent-filter>
</receiver>

<!-- Boot Receiver -->
<receiver
    android:name=".alarm.BootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>

<!-- Alarm Service -->
<service
    android:name=".alarm.AlarmService"
    android:enabled="true"
    android:exported="false" />

<!-- Full Screen Alarm Activity -->
<activity
    android:name=".ui.alarm.AlarmNotificationActivity"
    android:exported="false"
    android:showOnLockScreen="true"
    android:turnScreenOn="true"
    android:theme="@style/Theme.MeetingAlarm.AlarmScreen" />
```

## Gradle Dependencies

```gradle
// Google Sign-In
implementation 'com.google.android.gms:play-services-auth:20.7.0'

// Google Calendar API
implementation 'com.google.api-client:google-api-client-android:2.2.0'
implementation 'com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0'

// Material Design
implementation 'com.google.android.material:material:1.11.0'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

## Common Gotchas

### 1. Exact Alarm Permission (Android 12+)
```kotlin
// Always check before setting alarms
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (!alarmManager.canScheduleExactAlarms()) {
        // Request permission
        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
    }
}
```

### 2. PendingIntent Flags
```kotlin
// Use FLAG_IMMUTABLE for security (required on Android 12+)
PendingIntent.getBroadcast(
    context,
    requestCode,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
```

### 3. Foreground Service Android 12+
```kotlin
// Use startForegroundService instead of startService
context.startForegroundService(serviceIntent)

// Call startForeground() within 5 seconds in the service
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForeground(NOTIFICATION_ID, notification)
    // ... rest of code
}
```

### 4. Release MediaPlayer
```kotlin
// Always release in onDestroy
override fun onDestroy() {
    super.onDestroy()
    mediaPlayer?.let {
        if (it.isPlaying) it.stop()
        it.release()
    }
    mediaPlayer = null
}
```

### 5. Close Cursors
```kotlin
// Always close cursors
cursor?.use {
    // Process cursor
} // Automatically closed here
```

## Testing Commands

```bash
# Test alarm trigger manually
adb shell am broadcast -a com.meetingalarm.app.ALARM_TRIGGER

# Test boot receiver
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED

# Check scheduled alarms
adb shell dumpsys alarm | grep meetingalarm

# Grant permissions via ADB
adb shell pm grant com.meetingalarm.app android.permission.READ_CALENDAR
adb shell pm grant com.meetingalarm.app android.permission.POST_NOTIFICATIONS
```

---

Use these snippets as reference while implementing the app!
