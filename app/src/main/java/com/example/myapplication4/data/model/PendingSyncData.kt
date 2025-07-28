package com.example.myapplication4.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pending_sync")
data class PendingSyncData(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userLocalId: Int,
    val action: String,
    val timestamp: Long = System.currentTimeMillis()
)
