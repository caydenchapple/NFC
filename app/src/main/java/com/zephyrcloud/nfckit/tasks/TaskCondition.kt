package com.zephyrcloud.nfckit.tasks

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar

/** Gates whether a [TaskStep] runs -- the "conditional logic" building block for chains. */
@Serializable
sealed class TaskCondition {

    @Serializable
    @SerialName("always")
    data object Always : TaskCondition()

    @Serializable
    @SerialName("time_window")
    data class TimeWindow(val startMinuteOfDay: Int, val endMinuteOfDay: Int) : TaskCondition()

    @Serializable
    @SerialName("day_of_week")
    data class DayOfWeek(val calendarDays: Set<Int>) : TaskCondition()

    @Serializable
    @SerialName("wifi_connected")
    data class WifiConnected(val expected: Boolean) : TaskCondition()

    @Serializable
    @SerialName("bluetooth_enabled")
    data class BluetoothEnabled(val expected: Boolean) : TaskCondition()

    @Serializable
    @SerialName("battery_above")
    data class BatteryAbove(val percent: Int) : TaskCondition()
}

object ConditionEvaluator {

    fun evaluate(condition: TaskCondition, context: Context): Boolean = when (condition) {
        is TaskCondition.Always -> true
        is TaskCondition.TimeWindow -> {
            val now = Calendar.getInstance()
            val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            if (condition.startMinuteOfDay <= condition.endMinuteOfDay) {
                minuteOfDay in condition.startMinuteOfDay..condition.endMinuteOfDay
            } else {
                // Window wraps past midnight, e.g. 22:00 - 06:00.
                minuteOfDay >= condition.startMinuteOfDay || minuteOfDay <= condition.endMinuteOfDay
            }
        }
        is TaskCondition.DayOfWeek ->
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in condition.calendarDays
        is TaskCondition.WifiConnected -> isWifiConnected(context) == condition.expected
        is TaskCondition.BluetoothEnabled -> isBluetoothEnabled(context) == condition.expected
        is TaskCondition.BatteryAbove -> batteryPercent(context) > condition.percent
    }

    private fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isBluetoothEnabled(context: Context): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return adapter.isEnabled
    }

    private fun batteryPercent(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return 100
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
