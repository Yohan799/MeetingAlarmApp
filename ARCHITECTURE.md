# Meeting Alarm App - Architecture Overview

## 📐 Architecture Pattern

This app follows a **simplified MVVM-like architecture** with native Android components:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
├─────────────────────────────────────────────────────────┤
│  Activities & Fragments                                  │
│  ├── SplashActivity                                      │
│  ├── SignInActivity                                      │
│  ├── MainActivity                                        │
│  └── AlarmNotificationActivity                          │
├─────────────────────────────────────────────────────────┤
│                      Adapters                            │
│  └── EventsAdapter (RecyclerView)                       │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                     Business Layer                       │
├─────────────────────────────────────────────────────────┤
│  Alarm Management (CORE FEATURE)                         │
│  ├── NativeAlarmManager ⭐                               │
│  ├── AlarmReceiver                                       │
│  ├── AlarmService                                        │
│  └── BootReceiver                                        │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                      Data Layer                          │
├─────────────────────────────────────────────────────────┤
│  Data Models                                             │
│  └── CalendarEvent                                       │
├─────────────────────────────────────────────────────────┤
│  Repository                                              │
│  └── CalendarRepository                                  │
├─────────────────────────────────────────────────────────┤
│  Data Sources                                            │
│  ├── Android Calendar ContentProvider                   │
│  └── Google Sign-In API                                 │
└─────────────────────────────────────────────────────────┘
```

## 🔑 Key Components

### 1. NativeAlarmManager ⭐ (CORE FEATURE)

**Purpose**: Manages all alarm operations using Android's native AlarmManager

**Key Responsibilities:**
- Set exact alarms using `setAlarmClock()`
- Cancel existing alarms
- Check alarm permissions
- Reschedule alarms after reboot

**Why Native?**
- Most reliable way to trigger alarms
- Survives app termination
- Works in Doze mode
- Integrates with system alarm UI

**Code Highlights:**
```kotlin
// Exact alarm with system UI integration
val alarmClockInfo = AlarmManager.AlarmClockInfo(alarmTime, pendingIntent)
alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
```

### 2. AlarmReceiver

**Purpose**: BroadcastReceiver that catches alarm triggers

**Flow:**
```
AlarmManager triggers
    ↓
AlarmReceiver.onReceive()
    ↓
Extracts event data
    ↓
Starts AlarmService
```

**Why Receiver Pattern?**
- Decouples alarm scheduling from alarm handling
- Can receive broadcasts even when app is closed
- Android system requirement for alarm handling

### 3. AlarmService

**Purpose**: Foreground service that handles the active alarm

**Responsibilities:**
- Play alarm sound (uses system ringtone)
- Vibrate device with pattern
- Show full-screen notification activity
- Handle dismiss/snooze actions

**Why Foreground Service?**
- Prevents system from killing the alarm
- Can show notification while alarm is active
- Required for audio playback in background

**Code Highlights:**
```kotlin
// Foreground service with notification
startForeground(NOTIFICATION_ID, notification)

// System alarm sound
val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
mediaPlayer.setDataSource(context, alarmUri)
```

### 4. CalendarRepository

**Purpose**: Fetches calendar events from Android Calendar

**Data Source**: Android's CalendarContract ContentProvider

**Why ContentProvider?**
- Native Android API for calendar access
- Works with all calendar apps (Google, Outlook, etc.)
- Efficient querying with cursors
- Proper permission handling

**Code Highlights:**
```kotlin
val cursor = context.contentResolver.query(
    CalendarContract.Events.CONTENT_URI,
    projection,
    selection,
    selectionArgs,
    sortOrder
)
```

### 5. BootReceiver

**Purpose**: Reschedules all alarms after device reboot

**Why Needed?**
- Android clears all alarms on reboot
- Ensures no meetings are missed
- Restores alarm state automatically

## 🔄 Data Flow

### Setting an Alarm

```
User selects event
    ↓
MainActivity.showAlarmOptionsDialog()
    ↓
User chooses time (e.g., 15 minutes)
    ↓
MainActivity.setAlarm(event, 15)
    ↓
NativeAlarmManager.setAlarm(event, 15)
    ↓
Calculate alarm time (startTime - 15 min)
    ↓
AlarmManager.setAlarmClock()
    ↓
PendingIntent created with event data
    ↓
Alarm scheduled in Android system
```

### Alarm Triggers

```
Scheduled time reached
    ↓
AlarmManager triggers PendingIntent
    ↓
AlarmReceiver.onReceive()
    ↓
Extract event data from Intent
    ↓
Start AlarmService (foreground)
    ↓
AlarmService plays sound + vibrates
    ↓
Start AlarmNotificationActivity (full screen)
    ↓
User sees alarm screen
    ↓
User taps Dismiss or Snooze
    ↓
AlarmService stops
```

### Fetching Calendar Events

```
App opens / User refreshes
    ↓
MainActivity.loadCalendarEvents()
    ↓
CalendarRepository.fetchUpcomingEvents()
    ↓
Query CalendarContract ContentProvider
    ↓
Process Cursor with event data
    ↓
Map to CalendarEvent objects
    ↓
Return list to MainActivity
    ↓
Update RecyclerView via EventsAdapter
```

## 🎯 Design Decisions

### 1. Why Native Alarm Over WorkManager?

**Chosen: Native AlarmManager**

Reasons:
- ✅ Exact timing guaranteed (critical for meetings)
- ✅ Works in Doze mode without restrictions
- ✅ Survives app termination
- ✅ Shows in system alarm UI
- ✅ Most reliable for time-critical tasks

WorkManager limitations:
- ❌ Not guaranteed exact timing
- ❌ Can be delayed by system
- ❌ Not suitable for alarm use case

### 2. Why Foreground Service?

**Chosen: Foreground Service for alarm handling**

Reasons:
- ✅ Prevents system from killing alarm
- ✅ Can play audio in background
- ✅ User knows alarm is active (notification)
- ✅ Required for reliable alarm experience

### 3. Why ContentProvider for Calendar?

**Chosen: CalendarContract ContentProvider**

Reasons:
- ✅ Native Android API (no extra dependencies)
- ✅ Works with all calendar apps
- ✅ Direct access to device calendars
- ✅ Efficient data querying

Alternative (not chosen):
- ❌ Google Calendar API REST: Requires network, slower
- ❌ Custom database: Data duplication, sync issues

### 4. Why No ViewModel?

**Decision: Direct Activity implementation**

Reasons:
- Simple app with straightforward flows
- No complex state management needed
- Coroutines handle async operations
- Easier to understand for learning

**Note**: For production, consider adding ViewModels for better testing and state management.

## 📱 Android Components Used

### Activities (4)
1. **SplashActivity**: Initial app screen
2. **SignInActivity**: Google authentication
3. **MainActivity**: Event list and alarm management
4. **AlarmNotificationActivity**: Full-screen alarm display

### Services (1)
1. **AlarmService**: Foreground service for active alarms

### BroadcastReceivers (2)
1. **AlarmReceiver**: Handles alarm triggers
2. **BootReceiver**: Reschedules after reboot

### ContentProviders (1 - System)
1. **CalendarContract**: Android's calendar data

### Adapters (1)
1. **EventsAdapter**: RecyclerView adapter for events

## 🔐 Permission Model

### Runtime Permissions
```kotlin
// Calendar access
Manifest.permission.READ_CALENDAR

// Notifications (Android 13+)
Manifest.permission.POST_NOTIFICATIONS

// Exact alarms (Android 12+)
Manifest.permission.SCHEDULE_EXACT_ALARM
Manifest.permission.USE_EXACT_ALARM
```

### Permission Handling Flow
```
App starts
    ↓
Check if permissions granted
    ↓
If not granted → Request permissions
    ↓
User grants/denies
    ↓
Handle result appropriately
```

## 🧪 Testing Strategy

### Unit Tests (Recommended)
- CalendarEvent data model transformations
- Alarm time calculations
- Event filtering logic

### Integration Tests
- Calendar data fetching
- Alarm scheduling and cancellation
- Google Sign-In flow

### UI Tests
- Event list display
- Alarm setting dialogs
- Full-screen alarm interactions

### Manual Tests
- Alarm triggers at exact time
- Sound and vibration work
- Works on lock screen
- Survives reboot
- Handles permission denials

## 🚀 Performance Considerations

### Memory
- Release MediaPlayer after use
- Close cursors properly
- Avoid memory leaks in receivers

### Battery
- Use exact alarms sparingly
- Stop foreground service when alarm dismissed
- Efficient calendar queries with proper selection

### Network
- Minimal network usage (only Google Sign-In)
- Calendar data from local ContentProvider
- No continuous syncing

## 📊 Scalability

### Current Limitations
- No local database for alarm persistence
- Single user (device owner)
- Limited to 30 days of events
- No recurring alarm patterns

### Recommended Enhancements
1. **Add Room Database**
   - Persist alarm settings
   - Store event details locally
   - Faster app startup

2. **Add ViewModel Layer**
   - Better state management
   - Easier testing
   - Lifecycle awareness

3. **Add Repository Pattern**
   - Abstract data sources
   - Easier to add remote calendar sources
   - Better error handling

4. **Add Dependency Injection**
   - Use Hilt or Koin
   - Easier testing
   - Better code organization

## 🔧 Maintenance Tips

### Common Updates Needed
1. **Gradle Dependencies**: Update regularly
2. **Google Play Services**: Keep auth library current
3. **Target SDK**: Update annually for Play Store
4. **Permissions**: Review changes in new Android versions

### Monitoring
- Track alarm trigger success rate
- Monitor battery usage
- Check crash reports for permission issues
- Verify calendar data access

---

This architecture provides a **solid foundation** for a meeting alarm app while keeping the code **simple and maintainable**. The native approach ensures **maximum reliability** for the critical alarm functionality.
