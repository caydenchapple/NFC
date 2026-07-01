package com.zephyrcloud.nfckit.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromHistoryAction(action: HistoryAction): String = action.name

    @TypeConverter
    fun toHistoryAction(value: String): HistoryAction = HistoryAction.valueOf(value)
}
