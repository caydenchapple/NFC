package com.zephyrcloud.nfckit.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/**
 * Executes a [TaskChain] step by step, skipping steps whose [TaskCondition] doesn't hold.
 *
 * Several actions can't be performed directly by a third-party app on modern Android
 * (WiFi/Bluetooth toggling is restricted since API 29/31, brightness needs a user-granted
 * "modify system settings" permission, exact alarms need scheduling permission on API 31+).
 * Where that's the case this executor does the best available thing -- usually opening the
 * relevant system settings screen -- and reports [ActionResult.Failed] with an explanation
 * rather than silently pretending to succeed.
 */
class TaskExecutor(
    private val context: Context,
    private val resolveProfileChain: (suspend (Long) -> TaskChain?)? = null,
) {
    private val NOTIFICATION_CHANNEL_ID = "nfckit_tasks"

    suspend fun run(chain: TaskChain, depth: Int = 0): List<StepResult> {
        if (depth > 5) return listOf(StepResult.Skipped(TaskActionSpec.Delay(0), "Profile chain nesting too deep"))
        val results = mutableListOf<StepResult>()
        for (step in chain.steps) {
            if (step.delayBeforeMs > 0) delay(step.delayBeforeMs)
            if (!ConditionEvaluator.evaluate(step.condition, context)) {
                results += StepResult.Skipped(step.action, "Condition not met")
                continue
            }
            val outcome = if (step.action is TaskActionSpec.RunProfile) {
                runNestedProfile(step.action, depth)
            } else {
                executeSingle(step.action)
            }
            results += StepResult.Ran(step.action, outcome)
        }
        return results
    }

    private suspend fun runNestedProfile(action: TaskActionSpec.RunProfile, depth: Int): ActionResult {
        val resolver = resolveProfileChain ?: return ActionResult.Failed("No profile resolver configured")
        val nested = resolver(action.profileId) ?: return ActionResult.Failed("Profile has no task chain")
        run(nested, depth + 1)
        return ActionResult.Success
    }

    private fun executeSingle(action: TaskActionSpec): ActionResult = try {
        when (action) {
            is TaskActionSpec.WifiToggle -> toggleWifi(action.enable)
            is TaskActionSpec.BluetoothToggle -> toggleBluetooth(action.enable)
            is TaskActionSpec.OpenWifiSettings -> openSettings(Settings.ACTION_WIFI_SETTINGS)
            is TaskActionSpec.OpenBluetoothSettings -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            is TaskActionSpec.OpenAirplaneModeSettings -> openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            is TaskActionSpec.OpenNfcSettings -> openSettings(Settings.ACTION_NFC_SETTINGS)
            is TaskActionSpec.OpenHotspotSettings -> openSettings(Settings.ACTION_WIRELESS_SETTINGS)

            is TaskActionSpec.SetRingerMode -> setRingerMode(action.mode)
            is TaskActionSpec.SetVolume -> setVolume(action.stream, action.percent)
            is TaskActionSpec.PlayPauseMedia -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

            is TaskActionSpec.ToggleFlashlight -> toggleFlashlight(action.enable)
            is TaskActionSpec.SetBrightness -> setBrightness(action.percent)
            is TaskActionSpec.Vibrate -> vibrate(action.milliseconds)
            is TaskActionSpec.ShowToast -> showToast(action.message)
            is TaskActionSpec.ShowNotification -> showNotification(action.title, action.text)
            is TaskActionSpec.OpenDoNotDisturbSettings -> openSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

            is TaskActionSpec.OpenApp -> openApp(action.packageName)
            is TaskActionSpec.OpenUrl -> openUrl(action.url)
            is TaskActionSpec.OpenPlayStoreListing -> openUrl("https://play.google.com/store/apps/details?id=${action.packageName}")

            is TaskActionSpec.DialNumber -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.phoneNumber}")))
            is TaskActionSpec.ComposeSms -> composeSms(action.phoneNumber, action.message)
            is TaskActionSpec.ComposeEmail -> composeEmail(action.address, action.subject, action.body)

            is TaskActionSpec.SetAlarm -> setAlarm(action.hour, action.minute, action.label)
            is TaskActionSpec.StartTimer -> startTimer(action.seconds, action.label)
            is TaskActionSpec.AddCalendarEvent -> addCalendarEvent(action)
            is TaskActionSpec.OpenMapsLocation -> openMaps(action.latitude, action.longitude, action.label)

            is TaskActionSpec.SendBroadcast -> sendBroadcast(action.action)
            is TaskActionSpec.CustomIntent -> launchCustomIntent(action)
            is TaskActionSpec.Delay -> ActionResult.Success // delay already applied via step.delayBeforeMs
            is TaskActionSpec.RunProfile -> ActionResult.Failed("Handled by run()") // unreachable
        }
    } catch (e: Exception) {
        ActionResult.Failed(e.message ?: "Unknown error running ${action.label}")
    }

    // ---- Connectivity ----

    private fun toggleWifi(enable: Boolean): ActionResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openSettings(Settings.Panel.ACTION_WIFI)
            ActionResult.Failed("Android 10+ blocks apps from toggling Wi-Fi directly; opened the quick panel instead.")
        } else {
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enable
            ActionResult.Success
        }
    }

    private fun toggleBluetooth(enable: Boolean): ActionResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return ActionResult.Failed("Missing BLUETOOTH_CONNECT permission")
        }
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return ActionResult.Failed("No Bluetooth adapter")
        @Suppress("DEPRECATION")
        val changed = if (enable) adapter.enable() else adapter.disable()
        return if (changed) ActionResult.Success else ActionResult.Failed("Bluetooth adapter refused the request")
    }

    private fun openSettings(action: String): ActionResult {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return ActionResult.Success
    }

    // ---- Media & sound ----

    private fun audioManager() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun setRingerMode(mode: RingerMode): ActionResult {
        val am = audioManager()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if ((mode == RingerMode.SILENT || mode == RingerMode.VIBRATE) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !notificationManager.isNotificationPolicyAccessGranted
        ) {
            return ActionResult.Failed("Needs Do Not Disturb access to change ringer mode on this Android version")
        }
        am.ringerMode = when (mode) {
            RingerMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
            RingerMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            RingerMode.SILENT -> AudioManager.RINGER_MODE_SILENT
        }
        return ActionResult.Success
    }

    private fun setVolume(stream: VolumeStream, percent: Int): ActionResult {
        val am = audioManager()
        val streamType = when (stream) {
            VolumeStream.MEDIA -> AudioManager.STREAM_MUSIC
            VolumeStream.RING -> AudioManager.STREAM_RING
            VolumeStream.ALARM -> AudioManager.STREAM_ALARM
            VolumeStream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
        }
        val max = am.getStreamMaxVolume(streamType)
        val target = (max * percent.coerceIn(0, 100) / 100.0).toInt()
        am.setStreamVolume(streamType, target, 0)
        return ActionResult.Success
    }

    private fun sendMediaKey(keyCode: Int): ActionResult {
        val am = audioManager()
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return ActionResult.Success
    }

    // ---- Device ----

    private fun toggleFlashlight(enable: Boolean): ActionResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return ActionResult.Failed("No flashlight-capable camera")
        cameraManager.setTorchMode(cameraId, enable)
        return ActionResult.Success
    }

    private fun setBrightness(percent: Int): ActionResult {
        if (!Settings.System.canWrite(context)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return ActionResult.Failed("Needs \"modify system settings\" permission; opened the grant screen.")
        }
        val value = (255 * percent.coerceIn(0, 100) / 100.0).toInt()
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        return ActionResult.Success
    }

    private fun vibrate(millis: Long): ActionResult {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        return ActionResult.Success
    }

    private fun showToast(message: String): ActionResult {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        return ActionResult.Success
    }

    private fun showNotification(title: String, text: String): ActionResult {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, "Task results", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return ActionResult.Failed("Missing POST_NOTIFICATIONS permission")
        }
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
        return ActionResult.Success
    }

    // ---- Apps & web ----

    private fun openApp(packageName: String): ActionResult {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return ActionResult.Failed("App not installed: $packageName")
        return startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun openUrl(url: String): ActionResult {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        return startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---- Communication ----

    private fun composeSms(number: String, message: String?): ActionResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            if (!message.isNullOrBlank()) putExtra("sms_body", message)
        }
        return startActivity(intent)
    }

    private fun composeEmail(address: String, subject: String?, body: String?): ActionResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).apply {
            if (!subject.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
            if (!body.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, body)
        }
        return startActivity(intent)
    }

    // ---- Productivity ----

    private fun setAlarm(hour: Int, minute: Int, label: String?): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
        }
        return startActivity(intent)
    }

    private fun startTimer(seconds: Int, label: String?): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return startActivity(intent)
    }

    private fun addCalendarEvent(action: TaskActionSpec.AddCalendarEvent): ActionResult {
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, action.title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, action.startEpochMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, action.endEpochMillis)
            if (!action.location.isNullOrBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, action.location)
        }
        return startActivity(intent)
    }

    private fun openMaps(lat: Double, lon: Double, label: String?): ActionResult {
        val query = if (label.isNullOrBlank()) "$lat,$lon" else "$lat,$lon(${Uri.encode(label)})"
        return startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$query")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---- Advanced ----

    private fun sendBroadcast(action: String): ActionResult {
        if (action.isBlank()) return ActionResult.Failed("No broadcast action specified")
        context.sendBroadcast(Intent(action))
        return ActionResult.Success
    }

    private fun launchCustomIntent(spec: TaskActionSpec.CustomIntent): ActionResult {
        val intent = Intent(spec.action)
        if (spec.packageName != null && spec.className != null) {
            intent.setClassName(spec.packageName, spec.className)
        } else if (spec.packageName != null) {
            intent.setPackage(spec.packageName)
        }
        spec.dataUri?.let { intent.data = Uri.parse(it) }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startActivity(intent)
    }

    private fun startActivity(intent: Intent): ActionResult {
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ActionResult.Success
        } else {
            ActionResult.Failed("No app can handle this action")
        }
    }
}
