package com.example.myapplication4.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication4.data.model.PendingSyncData
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSync(pendingSync: PendingSyncData)

    @Query("SELECT * FROM pending_sync ORDER BY timestamp ASC")
    fun getAllPendingSyncs(): Flow<List<PendingSyncData>>

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deletePendingSync(id: String)

    @Query("DELETE FROM pending_sync")
    suspend fun clearAllPendingSyncs()
}
