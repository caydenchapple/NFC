package com.zephyrcloud.nfckit.tasks

/** Descriptor used to render the "add a task" picker grouped by category. */
data class TaskTemplate(
    val title: String,
    val category: TaskCategory,
    val description: String,
    val default: TaskActionSpec,
)

object TaskCatalog {
    val templates: List<TaskTemplate> = listOf(
        TaskTemplate("Turn Wi-Fi on", TaskCategory.CONNECTIVITY, "Enables Wi-Fi where the OS still allows it; opens the panel otherwise.", TaskActionSpec.WifiToggle(true)),
        TaskTemplate("Turn Wi-Fi off", TaskCategory.CONNECTIVITY, "Disables Wi-Fi where the OS still allows it; opens the panel otherwise.", TaskActionSpec.WifiToggle(false)),
        TaskTemplate("Turn Bluetooth on", TaskCategory.CONNECTIVITY, "Enables Bluetooth (needs BLUETOOTH_CONNECT on Android 12+).", TaskActionSpec.BluetoothToggle(true)),
        TaskTemplate("Turn Bluetooth off", TaskCategory.CONNECTIVITY, "Disables Bluetooth (needs BLUETOOTH_CONNECT on Android 12+).", TaskActionSpec.BluetoothToggle(false)),
        TaskTemplate("Open Wi-Fi settings", TaskCategory.CONNECTIVITY, "Jumps to the system Wi-Fi panel.", TaskActionSpec.OpenWifiSettings),
        TaskTemplate("Open Bluetooth settings", TaskCategory.CONNECTIVITY, "Jumps to the system Bluetooth panel.", TaskActionSpec.OpenBluetoothSettings),
        TaskTemplate("Open airplane mode settings", TaskCategory.CONNECTIVITY, "Airplane mode can't be toggled directly by apps since Android 4.2.", TaskActionSpec.OpenAirplaneModeSettings),
        TaskTemplate("Open NFC settings", TaskCategory.CONNECTIVITY, "Jumps to the system NFC panel.", TaskActionSpec.OpenNfcSettings),
        TaskTemplate("Open hotspot settings", TaskCategory.CONNECTIVITY, "Jumps to the tethering panel.", TaskActionSpec.OpenHotspotSettings),

        TaskTemplate("Set ringer mode", TaskCategory.MEDIA_SOUND, "Normal, vibrate, or silent.", TaskActionSpec.SetRingerMode(RingerMode.NORMAL)),
        TaskTemplate("Set volume", TaskCategory.MEDIA_SOUND, "Media, ring, alarm, or notification stream.", TaskActionSpec.SetVolume(VolumeStream.MEDIA, 50)),
        TaskTemplate("Play/pause media", TaskCategory.MEDIA_SOUND, "Sends a media-button event to the active player.", TaskActionSpec.PlayPauseMedia),

        TaskTemplate("Toggle flashlight", TaskCategory.DEVICE, "Uses the rear camera's torch.", TaskActionSpec.ToggleFlashlight(true)),
        TaskTemplate("Set brightness", TaskCategory.DEVICE, "Requires the \"modify system settings\" permission.", TaskActionSpec.SetBrightness(50)),
        TaskTemplate("Vibrate", TaskCategory.DEVICE, "One-shot vibration.", TaskActionSpec.Vibrate(200)),
        TaskTemplate("Show message", TaskCategory.DEVICE, "Quick on-screen toast.", TaskActionSpec.ShowToast("Tag scanned")),
        TaskTemplate("Show notification", TaskCategory.DEVICE, "Posts a notification.", TaskActionSpec.ShowNotification("NFC Kit", "Task ran")),
        TaskTemplate("Open Do Not Disturb settings", TaskCategory.DEVICE, "Needs notification policy access, granted manually per-app.", TaskActionSpec.OpenDoNotDisturbSettings),

        TaskTemplate("Open app", TaskCategory.APPS_WEB, "Launches an installed app by package name.", TaskActionSpec.OpenApp("")),
        TaskTemplate("Open website", TaskCategory.APPS_WEB, "Opens a URL in the browser.", TaskActionSpec.OpenUrl("https://")),
        TaskTemplate("Open Play Store listing", TaskCategory.APPS_WEB, "Deep-links to an app's store page.", TaskActionSpec.OpenPlayStoreListing("")),

        TaskTemplate("Dial number", TaskCategory.COMMUNICATION, "Opens the dialer pre-filled.", TaskActionSpec.DialNumber("")),
        TaskTemplate("Compose text message", TaskCategory.COMMUNICATION, "Opens the SMS app pre-filled; sending needs manual confirmation.", TaskActionSpec.ComposeSms("")),
        TaskTemplate("Compose email", TaskCategory.COMMUNICATION, "Opens the email app pre-filled.", TaskActionSpec.ComposeEmail("")),

        TaskTemplate("Set alarm", TaskCategory.PRODUCTIVITY, "Adds an alarm via the default clock app.", TaskActionSpec.SetAlarm(7, 0)),
        TaskTemplate("Start timer", TaskCategory.PRODUCTIVITY, "Starts a countdown via the default clock app.", TaskActionSpec.StartTimer(300)),
        TaskTemplate("Add calendar event", TaskCategory.PRODUCTIVITY, "Opens a pre-filled calendar event.", TaskActionSpec.AddCalendarEvent("", 0, 0)),
        TaskTemplate("Open location in Maps", TaskCategory.PRODUCTIVITY, "Opens coordinates in the default maps app.", TaskActionSpec.OpenMapsLocation(0.0, 0.0)),

        TaskTemplate("Send broadcast", TaskCategory.ADVANCED, "Fires a system/app broadcast intent -- for power users.", TaskActionSpec.SendBroadcast("")),
        TaskTemplate("Launch custom intent", TaskCategory.ADVANCED, "Full control over action/package/class/data/extras.", TaskActionSpec.CustomIntent()),
        TaskTemplate("Wait", TaskCategory.ADVANCED, "Pause before the next step in the chain.", TaskActionSpec.Delay(1000)),
        TaskTemplate("Run another profile's tasks", TaskCategory.ADVANCED, "Chains a saved profile's task list.", TaskActionSpec.RunProfile(0)),
    )

    fun byCategory(): Map<TaskCategory, List<TaskTemplate>> = templates.groupBy { it.category }
}
