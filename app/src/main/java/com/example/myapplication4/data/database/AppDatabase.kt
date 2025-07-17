package com.example.myapplication4.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication4.data.database.dao.UserDao
import com.example.myapplication4.data.database.dao.PendingSyncDao
import com.example.myapplication4.data.model.User
import com.example.myapplication4.data.model.PendingSyncData

@Database(entities = [User::class, PendingSyncData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun pendingSyncDao(): PendingSyncDao
}