# Permora - One-Place Android Permission Manager

**Permora** is a complete, production-quality Android application that gives users full visibility and granular control over app permissions, background sensor access, and system-level privileges.

---

## 🎬 App Demo

<p align="center">
  <a href="https://youtube.com/shorts/ljmUUdx1PwY">
    <img src="screenshot.png" alt="App Demo — Click to watch on YouTube" width="220" height="391" />
  </a>
</p>
*Click the screenshot above to watch the app in action on YouTube.*

---

## Features

1. **Dashboard & Privacy Score**
   - Live audit score (0–100) reflecting device privacy health based on sensitive permissions granted.
   - Real-time tally for sensitive permission categories: Camera, Microphone, Location, Contacts, SMS, Call Log, Storage, Phone, Calendar, Body Sensors, Nearby Devices, and Notifications.
   - Quick breakdown of User vs. System installed apps.
   - Top high-risk apps card with instant navigation to inspect.

2. **App Inventory & Filter System**
   - Fast, cached app list powered by Room and Kotlin Coroutines.
   - Filter by: User Apps, System Apps, Risky Apps, All Apps.
   - Sort by: Risk Level, Permission Count, Name (A-Z), Name (Z-A).
   - Real-time search query matching app name and package ID.
   - Multi-select mode for batch operations.

3. **Detailed App Inspector**
   - Inspect every permission declared by any application.
   - Grouped by official Android permission groups.
   - Plain-language risk assessments (Critical, High, Medium, Low) and descriptions.
   - Individual switches to toggle permissions.
   - Deep links into system App Info and special access management.

4. **Permission-Centric Explorer**
   - Select any permission (e.g., Location or Microphone) to see every app with active access.
   - Bulk revocation capability across all apps holding that permission.

5. **Permission Usage Timeline (AppOps)**
   - Historical audit log showing which apps accessed sensitive capabilities and when.

6. **Revoke All & Undo Engine**
   - "Revoke All Permissions" for any individual app or selected batch.
   - Automatic snapshot stored in Room database before revocation.
   - Undo snackbar allows restoring previous permission states with a single tap.

7. **Special Access Management**
   - Audit system-level capabilities: Display over other apps (Overlay), Usage Data Access, Install Unknown Apps, Notification Listener, All Files Access, and Ignore Battery Optimizations.
   - One-tap intent redirection to the corresponding Android Settings screen.

8. **Live Package Listener**
   - `PackageChangeReceiver` listens for package addition, removal, and replacement broadcasts to keep the cache automatically updated without requiring full rescans.

9. **Security Audit Export**
   - Export structured reports in JSON or CSV format using the Android Storage Access Framework (`CreateDocument`).

10. **100% Private & Local**
    - Zero network permissions (`INTERNET` permission is absent from `AndroidManifest.xml`).
    - Zero analytics, trackers, or external telemetry.

---

## Architecture & Tech Stack

- **Architecture**: Clean Architecture + MVVM + UDF (Unidirectional Data Flow)
- **UI**: Jetpack Compose, Material 3, Dynamic Color (Material You), Adaptive layouts
- **Language**: Kotlin 2.x
- **Persistence**: Jetpack Room, DataStore Preferences
- **Background Work**: AndroidX WorkManager (`ScanAppsWorker`)
- **Image Loading**: Coil
- **Logging**: Timber
