package com.joaomgcd.adb

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

private val String.permissionNameToDisplayName
    get() :String {
        val correctedName = this.removePrefix("android.permission.")
        return correctedName.split("_").joinToString(" ") { if (it.isNotEmpty()) it[0].uppercase() + it.substring(1).lowercase() else "" }
    }


sealed interface AdbPermission {
    val displayName: String
    val requiredFor: String
    val singlePermissions: List<Single>


    sealed interface Single : AdbPermission {
        val permissionName: String
        override val displayName get() = permissionName.permissionNameToDisplayName

        fun getCommandToggle(packageName: String, grant: Boolean): String
        fun getCommandIsGranted(packageName: String): String
        fun checkIfGranted(commandIsGrantedOutput: String): Boolean

        override val singlePermissions: List<Single>
            get() = listOf(this)
    }


    interface Normal : Single {
        override fun getCommandToggle(packageName: String, grant: Boolean) =
            "pm ${if (grant) "grant" else "revoke"} $packageName $permissionName"

        override fun getCommandIsGranted(packageName: String) =
            "dumpsys package $packageName | grep \"$permissionName: granted=true\""

        override fun checkIfGranted(commandIsGrantedOutput: String) =
            commandIsGrantedOutput.isNotEmpty()
    }


    interface AppOps : Single {
        override fun getCommandToggle(packageName: String, grant: Boolean) =
            "appops set $packageName $permissionName ${if (grant) "allow" else "deny"}"

        override fun getCommandIsGranted(packageName: String) =
            "appops get $packageName $permissionName"

        override fun checkIfGranted(commandIsGrantedOutput: String) =
            commandIsGrantedOutput.contains("allow")
    }


    interface Settings : Single {
        val scope: String
        val key: String
        val grantedValue: String

        override val permissionName get() = key
        override val displayName get() = key.permissionNameToDisplayName

        override fun getCommandToggle(packageName: String, grant: Boolean) =
            "settings put $scope $key ${if (grant) grantedValue else "0"}"

        override fun getCommandIsGranted(packageName: String) = "settings get $scope $key"

        override fun checkIfGranted(commandIsGrantedOutput: String) =
            commandIsGrantedOutput.trim() == grantedValue
    }


    data object ActivityRecognition : Normal {
        override val displayName get() = "Activity Recognition"
        override val permissionName = "android.permission.ACTIVITY_RECOGNITION"
        override val requiredFor = "Detecting physical activity like walking or driving."
    }


    data object AppUsageStats : AppOps {
        override val displayName get() = "Application Usage Stats"
        override val permissionName = "GET_USAGE_STATS"
        override val requiredFor = "Getting app info and checking which app is currently open."
    }


    data object BatteryStats : Normal {
        override val permissionName = "android.permission.BATTERY_STATS"
        override val requiredFor = "Reading low-level battery usage data."
    }


    data object Calendar : AdbPermission {
        override val displayName get() = "Calendar"
        override val requiredFor = "Reading and writing calendar events."

        private object Read : Normal {
            override val permissionName = "android.permission.READ_CALENDAR"
            override val requiredFor = "Reading calendar events."
        }

        private object Write : Normal {
            override val permissionName = "android.permission.WRITE_CALENDAR"
            override val requiredFor = "Creating or modifying calendar events."
        }

        override val singlePermissions: List<Single> = listOf(Read, Write)
    }


    data object CallLog : AdbPermission {
        override val displayName get() = "Call Log"
        override val requiredFor = "Reading and writing to the call log."

        private object Read : Normal {
            override val permissionName = "android.permission.READ_CALL_LOG"
            override val requiredFor = "Reading call log entries."
        }

        private object Write : Normal {
            override val permissionName = "android.permission.WRITE_CALL_LOG"
            override val requiredFor = "Writing call log entries."
        }

        override val singlePermissions: List<Single> = listOf(Read, Write)
    }


    data object Camera : Normal {
        override val permissionName = "android.permission.CAMERA"
        override val requiredFor = "Accessing the camera to take photos or record videos."
    }


    data object ChangeConfiguration : Normal {
        override val displayName get() = "Change System Locale"
        override val permissionName = "android.permission.CHANGE_CONFIGURATION"
        override val requiredFor = "Changing the system's current locale."
    }


    data object Contacts : AdbPermission {
        override val displayName get() = "Contacts"
        override val requiredFor = "Reading and writing device contacts."

        private object Read : Normal {
            override val permissionName = "android.permission.READ_CONTACTS"
            override val requiredFor = "Reading contacts."
        }

        private object Write : Normal {
            override val permissionName = "android.permission.WRITE_CONTACTS"
            override val requiredFor = "Creating or modifying contacts."
        }

        override val singlePermissions: List<Single> = listOf(Read, Write)
    }


    data object HardwareKeys : AdbPermission {
        override val displayName get() = "Hardware Keys"
        override val requiredFor = "Intercepting hardware key presses."

        private object LongClickVolume : Normal {
            override val permissionName = "android.permission.SET_VOLUME_KEY_LONG_PRESS_LISTENER"
            override val requiredFor = "Detecting long presses of the volume keys."
        }

        private object MediaButtons : Normal {
            override val permissionName = "android.permission.SET_MEDIA_KEY_LISTENER"
            override val requiredFor = "Detecting when media keys are pressed."
        }

        override val singlePermissions: List<Single> = listOf(LongClickVolume, MediaButtons)
    }


    data object HealthData : AdbPermission {
        override val displayName get() = "Health Data"
        override val requiredFor = "Reading health and sensor data like heart rate."

        private object BodySensors : Normal {
            override val permissionName = "android.permission.BODY_SENSORS"
            override val requiredFor = "Read Heart Rate and More"
        }

        private object BodySensorsBackground : Normal {
            override val permissionName = "android.permission.BODY_SENSORS_BACKGROUND"
            override val requiredFor = "Read Heart Rate and More without the app being open"
        }

        private object HealthReadHeartRate : Normal {
            override val permissionName = "android.permission.health.READ_HEART_RATE"
            override val requiredFor = "Reading your Heart Rate"
        }

        private object HealthReadSkinTemp : Normal {
            override val permissionName = "android.permission.health.READ_SKIN_TEMPERATURE"
            override val requiredFor = "Reading your Skin Temperature"
        }

        private object HealthReadOxygenSaturation : Normal {
            override val permissionName = "android.permission.health.READ_OXYGEN_SATURATION"
            override val requiredFor = "Reading Percentage of Oxygen in Your Blood"
        }

        private object HealthReadInBackground : Normal {
            override val permissionName = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
            override val requiredFor = "Read Heart Rate and More without the app being open"
        }

        override val singlePermissions: List<Single>
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                listOf(
                    HealthReadHeartRate,
                    HealthReadSkinTemp,
                    HealthReadOxygenSaturation,
                    HealthReadInBackground
                )
            } else {
                listOf(BodySensors, BodySensorsBackground)
            }
    }


    data object HiddenApiPolicy : Settings {
        override val displayName get() = "Access Hidden APIs"
        override val scope = "global"
        override val key = "hidden_api_policy"
        override val grantedValue = "1"
        override val requiredFor = "On Android 11+, enables actions like ADB Wifi or Mobile Network Type."
    }


    data object Location : AdbPermission {
        override val displayName get() = "Location"
        override val requiredFor = "Accessing device location."

        private object Fine : Normal {
            override val permissionName = "android.permission.ACCESS_FINE_LOCATION"
            override val requiredFor = "Accessing precise location."
        }

        private object Coarse : Normal {
            override val permissionName = "android.permission.ACCESS_COARSE_LOCATION"
            override val requiredFor = "Accessing approximate location."
        }

        private object Background : Normal {
            override val permissionName = "android.permission.ACCESS_BACKGROUND_LOCATION"
            override val requiredFor = "Accessing location while the app is in the background."
        }

        override val singlePermissions: List<Single>
            get() = buildList {
                add(Fine)
                add(Coarse)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Background)
                }
            }
    }


    data object ManageExternalStorage : AppOps {
        override val displayName get() = "Manage External Storage"
        override val permissionName = "MANAGE_EXTERNAL_STORAGE"
        override val requiredFor = "Accessing and modifying all files on shared storage."
    }


    data object Microphone : Normal {
        override val permissionName = "android.permission.RECORD_AUDIO"
        override val requiredFor = "Recording audio from the microphone."
    }


    data object ModifyAudioSettings : Normal {
        override val permissionName = "android.permission.MODIFY_AUDIO_SETTINGS"
        override val requiredFor = "Changing global audio settings like ringer mode."
    }


    data object NearbyDevices : AdbPermission {
        override val displayName get() = "Nearby Devices"
        override val requiredFor = "Interacting with nearby Bluetooth and Wi-Fi devices."

        private object BluetoothScan : Normal {
            override val permissionName = "android.permission.BLUETOOTH_SCAN"
            override val requiredFor = "Scanning for nearby Bluetooth devices."
        }

        private object BluetoothConnect : Normal {
            override val permissionName = "android.permission.BLUETOOTH_CONNECT"
            override val requiredFor = "Connecting to paired Bluetooth devices."
        }

        private object BluetoothAdvertise : Normal {
            override val permissionName = "android.permission.BLUETOOTH_ADVERTISE"
            override val requiredFor = "Advertising to other Bluetooth devices."
        }

        private object WifiDevices : Normal {
            override val permissionName = "android.permission.NEARBY_WIFI_DEVICES"
            override val requiredFor = "Finding and connecting to devices on the local Wi-Fi network."
        }

        override val singlePermissions: List<Single>
            get() = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(BluetoothScan)
                    add(BluetoothConnect)
                    add(BluetoothAdvertise)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(WifiDevices)
                }
            }
    }


    data object Notifications : Normal {
        override val displayName get() = "Post Notifications"
        override val permissionName = "android.permission.POST_NOTIFICATIONS"
        override val requiredFor = "Sending notifications to the user."
    }


    data object Overlays : AppOps {
        override val displayName get() = "Draw Over Other Apps"
        override val permissionName = "SYSTEM_ALERT_WINDOW"
        override val requiredFor = "Displaying UI elements, scenes, or bubbles over other applications."
    }


    data object Phone : AdbPermission {
        override val displayName get() = "Phone"
        override val requiredFor = "Managing phone calls and reading phone state."

        private object ReadState : Normal {
            override val permissionName = "android.permission.READ_PHONE_STATE"
            override val requiredFor = "Reading phone state, such as incoming call information."
        }

        private object Call : Normal {
            override val permissionName = "android.permission.CALL_PHONE"
            override val requiredFor = "Initiating phone calls."
        }

        private object AnswerCalls : Normal {
            override val permissionName = "android.permission.ANSWER_PHONE_CALLS"
            override val requiredFor = "Answering incoming phone calls."
        }

        private object ReadNumbers : Normal {
            override val permissionName = "android.permission.READ_PHONE_NUMBERS"
            override val requiredFor = "Reading the device's phone number."
        }

        private object ModifyState : Normal {
            override val permissionName = "android.permission.MODIFY_PHONE_STATE"
            override val requiredFor = "Modifying phone state, like toggling airplane mode."
        }

        override val singlePermissions: List<Single>
            get() = buildList {
                add(ReadState)
                add(Call)
                add(ReadNumbers)
                add(ModifyState)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    add(AnswerCalls)
                }
            }
    }


    data object ReadLogs : Normal {
        override val displayName get() = "Read System Logs"
        override val permissionName = "android.permission.READ_LOGS"
        override val requiredFor = "The 'Logcat Entry' event and clipboard access on Android 10+."
    }


    data object RequestInstallPackages : AppOps {
        override val displayName get() = "Request Install Packages"
        override val permissionName = "REQUEST_INSTALL_PACKAGES"
        override val requiredFor = "Allowing the app to prompt for app installations."
    }


    data object ScreenCapture : AppOps {
        override val displayName get() = "Capture Screen"
        override val permissionName = "PROJECT_MEDIA"
        override val requiredFor = "Taking screenshots and recording the screen without system prompts."
    }


    data object SecureSettings : Normal {
        override val displayName get() = "Write Secure Settings"
        override val permissionName = "android.permission.WRITE_SECURE_SETTINGS"
        override val requiredFor = "Changing system settings like display size or clipboard access."
    }


    data object ServiceBinding : AdbPermission {
        override val displayName get() = "Service Binding"
        override val requiredFor = "Binding to privileged system services."

        private object BindDeviceAdmin : Normal {
            override val permissionName = "android.permission.BIND_DEVICE_ADMIN"
            override val requiredFor = "Required for an app to become a device administrator."
        }

        private object BindNotificationListener : Normal {
            override val permissionName = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            override val requiredFor = "Intercepting notifications for sync or automation."
        }

        private object BindAccessibilityService : Normal {
            override val permissionName = "android.permission.BIND_ACCESSIBILITY_SERVICE"
            override val requiredFor = "Required for an app to provide an Accessibility Service."
        }

        override val singlePermissions: List<Single> = listOf(BindDeviceAdmin, BindNotificationListener, BindAccessibilityService)
    }


    data object Sms : AdbPermission {
        override val displayName get() = "SMS"
        override val requiredFor = "Sending and reading SMS/MMS messages."

        private object Read : Normal {
            override val permissionName = "android.permission.READ_SMS"
            override val requiredFor = "Reading SMS messages."
        }

        private object Send : Normal {
            override val permissionName = "android.permission.SEND_SMS"
            override val requiredFor = "Sending SMS messages."
        }

        private object Receive : Normal {
            override val permissionName = "android.permission.RECEIVE_SMS"
            override val requiredFor = "Receiving SMS messages."
        }

        override val singlePermissions: List<Single> = listOf(Read, Send, Receive)
    }


    data object Storage : AdbPermission {
        override val displayName get() = "Storage"
        override val requiredFor = "Accessing files on external storage."

        private object ReadExternal : Normal {
            override val permissionName = "android.permission.READ_EXTERNAL_STORAGE"
            override val requiredFor = "Reading files from external storage."
        }

        private object WriteExternal : Normal {
            override val permissionName = "android.permission.WRITE_EXTERNAL_STORAGE"
            override val requiredFor = "Writing files to external storage."
        }

        private object ReadMediaImages : Normal {
            override val permissionName = "android.permission.READ_MEDIA_IMAGES"
            override val requiredFor = "Reading images from media storage."
        }

        private object ReadMediaVideo : Normal {
            override val permissionName = "android.permission.READ_MEDIA_VIDEO"
            override val requiredFor = "Reading videos from media storage."
        }

        private object ReadMediaAudio : Normal {
            override val permissionName = "android.permission.READ_MEDIA_AUDIO"
            override val requiredFor = "Reading audio from media storage."
        }

        override val singlePermissions: List<Single>
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(ReadMediaImages, ReadMediaVideo, ReadMediaAudio)
            } else {
                listOf(ReadExternal, WriteExternal)
            }
    }


    data object SystemServices : AdbPermission {
        override val displayName get() = "System Services"
        override val requiredFor = "Querying system service information."

        private object Dump : Normal {
            override val permissionName = "android.permission.DUMP"
            override val requiredFor = "Checking which services are currently running."
        }

        private object PackageUsageStats : Normal {
            override val permissionName = "android.permission.PACKAGE_USAGE_STATS"
            override val requiredFor = "Getting app usage statistics for specific contexts."
        }

        override val singlePermissions: List<Single> = listOf(Dump, PackageUsageStats)
    }


    data object SystemState : AdbPermission {
        override val displayName get() = "System State"
        override val requiredFor = "Modifying system-level state and settings."

        private object ChangeNetworkState : Normal {
            override val permissionName = "android.permission.CHANGE_NETWORK_STATE"
            override val requiredFor = "Changing network connectivity state."
        }

        private object ExpandStatusBar : Normal {
            override val permissionName = "android.permission.EXPAND_STATUS_BAR"
            override val requiredFor = "Expanding or collapsing the status bar."
        }

        private object SetTimeZone : Normal {
            override val permissionName = "android.permission.SET_TIME_ZONE"
            override val requiredFor = "Setting the system's time zone."
        }

        private object WriteSyncSettings : Normal {
            override val permissionName = "android.permission.WRITE_SYNC_SETTINGS"
            override val requiredFor = "Modifying sync settings for accounts."
        }

        override val singlePermissions: List<Single> = listOf(ChangeNetworkState, ExpandStatusBar, SetTimeZone, WriteSyncSettings)
    }


    data object Vibration : AppOps {
        override val permissionName = "VIBRATE"
        override val requiredFor = "Controlling the device's vibrator."
    }


    data object WriteSettings : AppOps {
        override val displayName get() = "Write Settings"
        override val permissionName = "WRITE_SETTINGS"
        override val requiredFor = "Changing various system settings."
    }

    data object ScheduleExactAlarm : AppOps {
        override val displayName get() = "Schedule Exact Alarms"
        override val permissionName = "SCHEDULE_EXACT_ALARM"
        override val requiredFor = "Scheduling alarms that trigger at an exact time."
    }

    data object UseFullScreenIntent : AppOps {
        override val displayName get() = "Use Full Screen Intent"
        override val permissionName = "USE_FULL_SCREEN_INTENT"
        override val requiredFor = "Showing full-screen notifications that interrupt the user."
    }

    data object PictureInPicture : AppOps {
        override val displayName get() = "Picture-in-Picture"
        override val permissionName = "PICTURE_IN_PICTURE"
        override val requiredFor = "Displaying video in a floating window while the app is in background."
    }

    companion object {
        private val allPermissions by lazy {
            listOf(
                ActivityRecognition, AppUsageStats, BatteryStats, Calendar, CallLog, Camera,
                ChangeConfiguration, Contacts, HardwareKeys, HealthData, HiddenApiPolicy,
                Location, ManageExternalStorage, Microphone, ModifyAudioSettings,
                NearbyDevices, Notifications, Overlays, Phone, ReadLogs, RequestInstallPackages,
                ScreenCapture, SecureSettings, ServiceBinding, Sms, Storage, SystemServices,
                SystemState, Vibration, WriteSettings, ScheduleExactAlarm, UseFullScreenIntent, PictureInPicture
            )
        }
        private val manifestPermissionMap by lazy {
            val map = mutableMapOf<String, MutableList<Single>>()
            allPermissions.flatMap { it.singlePermissions }.forEach { perm ->
                val key: String? = when (perm) {
                    is Normal -> perm.permissionName
                    is Vibration -> "android.permission.VIBRATE"
                    Overlays -> "android.permission.SYSTEM_ALERT_WINDOW"
                    WriteSettings -> "android.permission.WRITE_SETTINGS"
                    AppUsageStats -> "android.permission.PACKAGE_USAGE_STATS"
                    ManageExternalStorage -> "android.permission.MANAGE_EXTERNAL_STORAGE"
                    RequestInstallPackages -> "android.permission.REQUEST_INSTALL_PACKAGES"
                    ScreenCapture -> "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION"
                    ScheduleExactAlarm -> "android.permission.SCHEDULE_EXACT_ALARM"
                    UseFullScreenIntent -> "android.permission.USE_FULL_SCREEN_INTENT"
                    PictureInPicture -> "android.permission.PICTURE_IN_PICTURE"
                    else -> null
                }
                key?.let { map.getOrPut(it) { mutableListOf() }.add(perm) }
            }
            map
        }

        fun fromString(key: String): AdbPermission? {
            manifestPermissionMap[key]?.firstOrNull()?.let { return singleToParentMap[it] }

            allPermissions.firstOrNull { it.displayName.equals(key, ignoreCase = true) }?.let { return it }

            return allPermissions.firstOrNull { parent ->
                parent.singlePermissions.any { single ->
                    single.permissionName.equals(key, ignoreCase = true) || single.displayName.equals(key, ignoreCase = true)
                }
            }
        }

        private val singleToParentMap by lazy {
            val map = mutableMapOf<Single, AdbPermission>()
            allPermissions.forEach { parentPermission ->
                parentPermission.singlePermissions.forEach { singlePermission ->
                    map[singlePermission] = parentPermission
                }
            }
            map
        }


        fun fromManifest(manifestPermissions: List<String>): List<AdbPermission.Single> {
            return manifestPermissions.flatMap { manifestPermissionMap[it] ?: emptyList() }.distinct()
        }


        fun fromPackage(context: Context, packageName: String): List<AdbPermission> {
            val manifestPermissions = try {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_PERMISSIONS
                ).requestedPermissions?.toList() ?: emptyList()
            } catch (e: PackageManager.NameNotFoundException) {
                emptyList()
            }
            val requestedSingles = fromManifest(manifestPermissions)
            return requestedSingles.mapNotNull { singleToParentMap[it] }.distinct()
        }
    }
}

@JvmInline
value class AdbPermissionStatuses(private val statuses: Map<AdbPermission, Boolean>) {
    operator fun get(permission: AdbPermission): Boolean? = statuses[permission]
    val list get() = statuses.toList()
}