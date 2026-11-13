package com.imn.iman.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.imn.iman.data.model.TasbeehCollection

@Dao
interface TasbeehDao {
    @Query("SELECT * FROM tasbeeh_collections ORDER BY id ASC")
    fun getAll(): Flow<List<TasbeehCollection>>

    @Insert
    suspend fun insert(collection: TasbeehCollection)

    @Delete
    suspend fun delete(collection: TasbeehCollection)

    @Query("SELECT COUNT(*) FROM tasbeeh_collections")
    suspend fun count(): Int

    @Query("DELETE FROM tasbeeh_collections WHERE title LIKE 'Dummy Card %'")
    suspend fun deleteDummyCards()
}
