package com.imn.iman.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.imn.iman.data.model.TasbeehCollection

@Database(entities = [TasbeehCollection::class], version = 1, exportSchema = false)
@TypeConverters(DhikrItemListConverter::class)
abstract class TasbeehDatabase : RoomDatabase() {
    abstract fun tasbeehDao(): TasbeehDao

    companion object {
        @Volatile private var INSTANCE: TasbeehDatabase? = null

        fun getInstance(context: Context): TasbeehDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TasbeehDatabase::class.java,
                    "tasbeeh.db",
                ).build().also { INSTANCE = it }
            }
    }
}
