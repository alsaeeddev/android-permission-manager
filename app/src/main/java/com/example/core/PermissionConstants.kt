package com.example.core

import android.Manifest
import android.os.Build

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class PermissionGroupType(
    val displayName: String,
    val description: String,
    val defaultRisk: RiskLevel
) {
    CAMERA("Camera", "Allows capturing pictures and recording video", RiskLevel.HIGH),
    MICROPHONE("Microphone", "Allows recording audio and background listening", RiskLevel.HIGH),
    LOCATION("Location", "Accesses precise GPS or network-based device coordinates", RiskLevel.CRITICAL),
    CONTACTS("Contacts", "Reads or modifies your personal address book", RiskLevel.HIGH),
    SMS("SMS", "Sends, reads, or intercepts SMS/MMS text messages", RiskLevel.CRITICAL),
    CALL_LOG("Call Log", "Reads or modifies telephone call history", RiskLevel.CRITICAL),
    STORAGE("Storage & Media", "Accesses saved photos, videos, audio, or filesystem", RiskLevel.MEDIUM),
    PHONE("Phone", "Makes phone calls, reads phone state, cellular identifiers", RiskLevel.HIGH),
    CALENDAR("Calendar", "Reads or modifies your calendar events and schedule", RiskLevel.MEDIUM),
    SENSORS("Sensors & Health", "Reads biometric sensor data, step counting, health trackers", RiskLevel.MEDIUM),
    NEARBY_DEVICES("Nearby Devices", "Discovers and communicates with Bluetooth/WiFi/UWB devices", RiskLevel.MEDIUM),
    NOTIFICATIONS("Notifications", "Displays alerts, status notifications, and foreground prompts", RiskLevel.LOW),
    SPECIAL("Special Access", "Privileged system-level capabilities and overlays", RiskLevel.CRITICAL),
    OTHER("Other Permissions", "Standard or developer-defined permissions", RiskLevel.LOW)
}

data class PermissionDefinition(
    val permission: String,
    val group: PermissionGroupType,
    val userFriendlyName: String,
    val description: String,
    val riskLevel: RiskLevel,
    val minSdk: Int = 1
)

object PermissionConstants {

    val DEFINITIONS: Map<String, PermissionDefinition> = listOf(
        // Location
        PermissionDefinition(
            Manifest.permission.ACCESS_FINE_LOCATION,
            PermissionGroupType.LOCATION,
            "Precise Location",
            "Accesses accurate GPS coordinates of the device.",
            RiskLevel.CRITICAL
        ),
        PermissionDefinition(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            PermissionGroupType.LOCATION,
            "Approximate Location",
            "Accesses cellular and Wi-Fi based approximate location.",
            RiskLevel.HIGH
        ),
        PermissionDefinition(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            PermissionGroupType.LOCATION,
            "Background Location",
            "Accesses device coordinates even when the app is closed or unused.",
            RiskLevel.CRITICAL,
            minSdk = 29
        ),

        // Camera & Mic
        PermissionDefinition(
            Manifest.permission.CAMERA,
            PermissionGroupType.CAMERA,
            "Camera",
            "Takes photos and captures video at any time.",
            RiskLevel.HIGH
        ),
        PermissionDefinition(
            Manifest.permission.RECORD_AUDIO,
            PermissionGroupType.MICROPHONE,
            "Record Audio",
            "Records sound and voice via the device microphone.",
            RiskLevel.HIGH
        ),

        // Contacts & Accounts
        PermissionDefinition(
            Manifest.permission.READ_CONTACTS,
            PermissionGroupType.CONTACTS,
            "Read Contacts",
            "Accesses your saved contacts and address book details.",
            RiskLevel.HIGH
        ),
        PermissionDefinition(
            Manifest.permission.WRITE_CONTACTS,
            PermissionGroupType.CONTACTS,
            "Modify Contacts",
            "Adds, alters, or removes contact data.",
            RiskLevel.MEDIUM
        ),
        PermissionDefinition(
            Manifest.permission.GET_ACCOUNTS,
            PermissionGroupType.CONTACTS,
            "Get Accounts",
            "Accesses the list of registered user accounts on the device.",
            RiskLevel.MEDIUM
        ),

        // SMS
        PermissionDefinition(
            Manifest.permission.READ_SMS,
            PermissionGroupType.SMS,
            "Read SMS",
            "Reads received SMS text messages (potential OTP leakage).",
            RiskLevel.CRITICAL
        ),
        PermissionDefinition(
            Manifest.permission.SEND_SMS,
            PermissionGroupType.SMS,
            "Send SMS",
            "Dispatches SMS text messages without user confirmation.",
            RiskLevel.CRITICAL
        ),
        PermissionDefinition(
            Manifest.permission.RECEIVE_SMS,
            PermissionGroupType.SMS,
            "Receive SMS",
            "Intercepts incoming text messages.",
            RiskLevel.CRITICAL
        ),

        // Call Log & Phone
        PermissionDefinition(
            Manifest.permission.READ_CALL_LOG,
            PermissionGroupType.CALL_LOG,
            "Read Call Log",
            "Accesses incoming, outgoing, and missed call history.",
            RiskLevel.CRITICAL
        ),
        PermissionDefinition(
            Manifest.permission.WRITE_CALL_LOG,
            PermissionGroupType.CALL_LOG,
            "Modify Call Log",
            "Changes or clears phone call logs.",
            RiskLevel.HIGH
        ),
        PermissionDefinition(
            Manifest.permission.READ_PHONE_STATE,
            PermissionGroupType.PHONE,
            "Read Phone State",
            "Reads cellular network details, phone number, and carrier status.",
            RiskLevel.HIGH
        ),
        PermissionDefinition(
            Manifest.permission.CALL_PHONE,
            PermissionGroupType.PHONE,
            "Make Phone Calls",
            "Initiates telephone calls directly without dialer prompt.",
            RiskLevel.HIGH
        ),

        // Calendar
        PermissionDefinition(
            Manifest.permission.READ_CALENDAR,
            PermissionGroupType.CALENDAR,
            "Read Calendar",
            "Inspects scheduled appointments, meetings, and personal events.",
            RiskLevel.MEDIUM
        ),
        PermissionDefinition(
            Manifest.permission.WRITE_CALENDAR,
            PermissionGroupType.CALENDAR,
            "Modify Calendar",
            "Creates, modifies, or deletes calendar events.",
            RiskLevel.MEDIUM
        ),

        // Storage & Media
        PermissionDefinition(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            PermissionGroupType.STORAGE,
            "Read External Storage",
            "Accesses shared files on device storage.",
            RiskLevel.HIGH
        ),
        PermissionDefinition(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            PermissionGroupType.STORAGE,
            "Write External Storage",
            "Saves or edits files on external storage.",
            RiskLevel.MEDIUM
        ),
        PermissionDefinition(
            "android.permission.READ_MEDIA_IMAGES",
            PermissionGroupType.STORAGE,
            "Read Photos & Images",
            "Reads photos and images stored on the device.",
            RiskLevel.MEDIUM,
            minSdk = 33
        ),
        PermissionDefinition(
            "android.permission.READ_MEDIA_VIDEO",
            PermissionGroupType.STORAGE,
            "Read Videos",
            "Reads video media stored on the device.",
            RiskLevel.MEDIUM,
            minSdk = 33
        ),
        PermissionDefinition(
            "android.permission.READ_MEDIA_AUDIO",
            PermissionGroupType.STORAGE,
            "Read Audio",
            "Reads songs and music files stored on the device.",
            RiskLevel.LOW,
            minSdk = 33
        ),

        // Sensors
        PermissionDefinition(
            Manifest.permission.BODY_SENSORS,
            PermissionGroupType.SENSORS,
            "Body Sensors",
            "Reads heart rate and physiological biometric sensors.",
            RiskLevel.HIGH
        ),
        PermissionDefinition(
            Manifest.permission.ACTIVITY_RECOGNITION,
            PermissionGroupType.SENSORS,
            "Activity Recognition",
            "Detects walking, driving, running, and physical movements.",
            RiskLevel.MEDIUM,
            minSdk = 29
        ),

        // Nearby
        PermissionDefinition(
            "android.permission.BLUETOOTH_SCAN",
            PermissionGroupType.NEARBY_DEVICES,
            "Bluetooth Scan",
            "Discovers nearby Bluetooth beacons and devices.",
            RiskLevel.MEDIUM,
            minSdk = 31
        ),
        PermissionDefinition(
            "android.permission.BLUETOOTH_CONNECT",
            PermissionGroupType.NEARBY_DEVICES,
            "Bluetooth Connect",
            "Connects to paired Bluetooth peripherals.",
            RiskLevel.LOW,
            minSdk = 31
        ),
        PermissionDefinition(
            "android.permission.NEARBY_WIFI_DEVICES",
            PermissionGroupType.NEARBY_DEVICES,
            "Nearby Wi-Fi Devices",
            "Discovers nearby Wi-Fi endpoints without location permission.",
            RiskLevel.LOW,
            minSdk = 33
        ),

        // Notifications
        PermissionDefinition(
            "android.permission.POST_NOTIFICATIONS",
            PermissionGroupType.NOTIFICATIONS,
            "Post Notifications",
            "Shows banners, sounds, and badges in the notification shade.",
            RiskLevel.LOW,
            minSdk = 33
        )
    ).associateBy { it.permission }

    fun getGroupForPermission(permission: String): PermissionGroupType {
        return DEFINITIONS[permission]?.group ?: when {
            permission.contains("LOCATION", ignoreCase = true) -> PermissionGroupType.LOCATION
            permission.contains("CAMERA", ignoreCase = true) -> PermissionGroupType.CAMERA
            permission.contains("AUDIO", ignoreCase = true) || permission.contains("RECORD", ignoreCase = true) -> PermissionGroupType.MICROPHONE
            permission.contains("CONTACTS", ignoreCase = true) -> PermissionGroupType.CONTACTS
            permission.contains("SMS", ignoreCase = true) || permission.contains("MMS", ignoreCase = true) -> PermissionGroupType.SMS
            permission.contains("CALL", ignoreCase = true) -> PermissionGroupType.CALL_LOG
            permission.contains("STORAGE", ignoreCase = true) || permission.contains("MEDIA", ignoreCase = true) -> PermissionGroupType.STORAGE
            permission.contains("PHONE", ignoreCase = true) -> PermissionGroupType.PHONE
            permission.contains("CALENDAR", ignoreCase = true) -> PermissionGroupType.CALENDAR
            permission.contains("SENSOR", ignoreCase = true) -> PermissionGroupType.SENSORS
            permission.contains("BLUETOOTH", ignoreCase = true) || permission.contains("NEARBY", ignoreCase = true) -> PermissionGroupType.NEARBY_DEVICES
            permission.contains("NOTIFICATION", ignoreCase = true) -> PermissionGroupType.NOTIFICATIONS
            else -> PermissionGroupType.OTHER
        }
    }

    fun getRiskLevelForPermission(permission: String): RiskLevel {
        return DEFINITIONS[permission]?.riskLevel ?: when (getGroupForPermission(permission)) {
            PermissionGroupType.LOCATION, PermissionGroupType.SMS, PermissionGroupType.CALL_LOG, PermissionGroupType.SPECIAL -> RiskLevel.CRITICAL
            PermissionGroupType.CAMERA, PermissionGroupType.MICROPHONE, PermissionGroupType.CONTACTS, PermissionGroupType.PHONE -> RiskLevel.HIGH
            PermissionGroupType.STORAGE, PermissionGroupType.CALENDAR, PermissionGroupType.SENSORS, PermissionGroupType.NEARBY_DEVICES -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    fun getFriendlyName(permission: String): String {
        return DEFINITIONS[permission]?.userFriendlyName
            ?: permission.substringAfterLast(".").replace("_", " ").lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    fun getDescription(permission: String): String {
        return DEFINITIONS[permission]?.description
            ?: "Standard system or application-declared permission ($permission)."
    }
}
