package com.imn.iman.data.local

import androidx.room.TypeConverter
import com.imn.iman.data.model.DhikrItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DhikrItemListConverter {
    @TypeConverter
    fun fromJson(value: String): List<DhikrItem> = Json.decodeFromString(value)

    @TypeConverter
    fun toJson(items: List<DhikrItem>): String = Json.encodeToString(items)
}
