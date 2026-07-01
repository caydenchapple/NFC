package com.zephyrcloud.nfckit.tasks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class TaskCategory { CONNECTIVITY, MEDIA_SOUND, DEVICE, APPS_WEB, COMMUNICATION, PRODUCTIVITY, ADVANCED }

/**
 * One automation step. Each variant maps to a concrete Android API call in [TaskExecutor].
 *
 * This isn't a literal 200-item switch statement -- it's the same shape real automation
 * apps converge on: a couple dozen well-supported actions per category, plus generic
 * escape hatches (custom intent / broadcast / shortcut) that cover the long tail without
 * hand-writing hundreds of near-duplicate cases. Where the OS blocks a direct toggle
 * (WiFi/Bluetooth/Airplane mode since Android 10, exact-alarm on Android 12+, etc.) the
 * action opens the relevant system panel instead of silently no-op'ing -- see the comments
 * in TaskExecutor for exactly which actions have this fallback.
 */
@Serializable
sealed class TaskActionSpec {
    abstract val category: TaskCategory
    abstract val label: String

    // ---- Connectivity ----
    @Serializable
    @SerialName("wifi_toggle")
    data class WifiToggle(val enable: Boolean) : TaskActionSpec() {
        override val category = TaskCategory.CONNECTIVITY
        override val label get() = if (enable) "Turn Wi-Fi on" else "Turn Wi-Fi off"
    }

    @Serializable
    @SerialName("bluetooth_toggle")
    data class BluetoothToggle(val enable: Boolean) : TaskActionSpec() {
        override val category = TaskCategory.CONNECTIVITY
        override val label get() = if (enable) "Turn Bluetooth on" else "Turn Bluetooth off"
    }

    @Serializable
    @SerialName("open_wifi_settings")
    data object OpenWifiSettings : TaskActionSpec() {
        override val category = TaskCategory.CONNECTIVITY
        override val label = "Open Wi-Fi settings"
    }

    @Serializable
    @SerialName("open_bluetooth_settings")
    data object OpenBluetoothSettings : TaskActionSpec() {
        override val category = TaskCategory.CONNECTIVITY
        override val label = "Open Bluetooth settings"
    }

    @Serializable
    @SerialName("open_airplane_mode_settings")
    data object OpenAirplaneModeSettings : TaskActionSpec() {
        override val category = TaskCategory.CONNECTIVITY
        override val label = "Open airplane mode settings"
    }

    @Serializable
    @SerialName("open_nfc_settings")
    data object OpenNfcSettings : TaskActionSpec() {
        override val category = TaskCategory.CONNECTIVITY
        override val label = "Open NFC settings"
    }

    @Serializable
    @SerialName("open_hotspot_settings")
    data object OpenHotspotSettings : TaskActionSpec() {
        override val category = TaskCategory.CONNECTIVITY
        override val label = "Open hotspot settings"
    }

    // ---- Media & sound ----
    @Serializable
    @SerialName("set_ringer_mode")
    data class SetRingerMode(val mode: RingerMode) : TaskActionSpec() {
        override val category = TaskCategory.MEDIA_SOUND
        override val label get() = "Set ringer to ${mode.name.lowercase()}"
    }

    @Serializable
    @SerialName("set_volume")
    data class SetVolume(val stream: VolumeStream, val percent: Int) : TaskActionSpec() {
        override val category = TaskCategory.MEDIA_SOUND
        override val label get() = "Set ${stream.name.lowercase()} volume to $percent%"
    }

    @Serializable
    @SerialName("play_pause_media")
    data object PlayPauseMedia : TaskActionSpec() {
        override val category = TaskCategory.MEDIA_SOUND
        override val label = "Play/pause media"
    }

    // ---- Device ----
    @Serializable
    @SerialName("toggle_flashlight")
    data class ToggleFlashlight(val enable: Boolean) : TaskActionSpec() {
        override val category = TaskCategory.DEVICE
        override val label get() = if (enable) "Turn flashlight on" else "Turn flashlight off"
    }

    @Serializable
    @SerialName("set_brightness")
    data class SetBrightness(val percent: Int) : TaskActionSpec() {
        override val category = TaskCategory.DEVICE
        override val label get() = "Set brightness to $percent%"
    }

    @Serializable
    @SerialName("vibrate")
    data class Vibrate(val milliseconds: Long) : TaskActionSpec() {
        override val category = TaskCategory.DEVICE
        override val label get() = "Vibrate ${milliseconds}ms"
    }

    @Serializable
    @SerialName("show_toast")
    data class ShowToast(val message: String) : TaskActionSpec() {
        override val category = TaskCategory.DEVICE
        override val label = "Show message"
    }

    @Serializable
    @SerialName("show_notification")
    data class ShowNotification(val title: String, val text: String) : TaskActionSpec() {
        override val category = TaskCategory.DEVICE
        override val label = "Show notification"
    }

    @Serializable
    @SerialName("open_dnd_settings")
    data object OpenDoNotDisturbSettings : TaskActionSpec() {
        override val category = TaskCategory.DEVICE
        override val label = "Open Do Not Disturb settings"
    }

    // ---- Apps & web ----
    @Serializable
    @SerialName("open_app")
    data class OpenApp(val packageName: String) : TaskActionSpec() {
        override val category = TaskCategory.APPS_WEB
        override val label = "Open app"
    }

    @Serializable
    @SerialName("open_url")
    data class OpenUrl(val url: String) : TaskActionSpec() {
        override val category = TaskCategory.APPS_WEB
        override val label = "Open website"
    }

    @Serializable
    @SerialName("open_play_store")
    data class OpenPlayStoreListing(val packageName: String) : TaskActionSpec() {
        override val category = TaskCategory.APPS_WEB
        override val label = "Open Play Store listing"
    }

    // ---- Communication ----
    @Serializable
    @SerialName("dial_number")
    data class DialNumber(val phoneNumber: String) : TaskActionSpec() {
        override val category = TaskCategory.COMMUNICATION
        override val label = "Dial number"
    }

    @Serializable
    @SerialName("compose_sms")
    data class ComposeSms(val phoneNumber: String, val message: String? = null) : TaskActionSpec() {
        override val category = TaskCategory.COMMUNICATION
        override val label = "Compose text message"
    }

    @Serializable
    @SerialName("compose_email")
    data class ComposeEmail(val address: String, val subject: String? = null, val body: String? = null) : TaskActionSpec() {
        override val category = TaskCategory.COMMUNICATION
        override val label = "Compose email"
    }

    // ---- Productivity ----
    @Serializable
    @SerialName("set_alarm")
    data class SetAlarm(val hour: Int, val minute: Int, val label: String? = null) : TaskActionSpec() {
        override val category = TaskCategory.PRODUCTIVITY
        override val label = "Set alarm for %02d:%02d".format(hour, minute)
    }

    @Serializable
    @SerialName("start_timer")
    data class StartTimer(val seconds: Int, val label: String? = null) : TaskActionSpec() {
        override val category = TaskCategory.PRODUCTIVITY
        override val label = "Start ${seconds}s timer"
    }

    @Serializable
    @SerialName("add_calendar_event")
    data class AddCalendarEvent(
        val title: String,
        val startEpochMillis: Long,
        val endEpochMillis: Long,
        val location: String? = null,
    ) : TaskActionSpec() {
        override val category = TaskCategory.PRODUCTIVITY
        override val label = "Add calendar event"
    }

    @Serializable
    @SerialName("open_maps_location")
    data class OpenMapsLocation(val latitude: Double, val longitude: Double, val label: String? = null) : TaskActionSpec() {
        override val category = TaskCategory.PRODUCTIVITY
        override val label = "Open location in Maps"
    }

    // ---- Advanced / escape hatches ----
    @Serializable
    @SerialName("send_broadcast")
    data class SendBroadcast(val action: String, val extrasJson: String? = null) : TaskActionSpec() {
        override val category = TaskCategory.ADVANCED
        override val label = "Send broadcast"
    }

    @Serializable
    @SerialName("custom_intent")
    data class CustomIntent(
        val action: String? = null,
        val packageName: String? = null,
        val className: String? = null,
        val dataUri: String? = null,
        val extrasJson: String? = null,
    ) : TaskActionSpec() {
        override val category = TaskCategory.ADVANCED
        override val label = "Launch custom intent"
    }

    @Serializable
    @SerialName("delay")
    data class Delay(val milliseconds: Long) : TaskActionSpec() {
        override val category = TaskCategory.ADVANCED
        override val label get() = "Wait ${milliseconds}ms"
    }

    @Serializable
    @SerialName("run_profile")
    data class RunProfile(val profileId: Long) : TaskActionSpec() {
        override val category = TaskCategory.ADVANCED
        override val label = "Run another profile's tasks"
    }
}

enum class RingerMode { NORMAL, VIBRATE, SILENT }
enum class VolumeStream { MEDIA, RING, ALARM, NOTIFICATION }
