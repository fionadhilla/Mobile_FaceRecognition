package com.example.myapplication4.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication4.data.database.dao.UserDao
import com.example.myapplication4.data.database.dao.PendingSyncDao
import com.example.myapplication4.data.model.User
import com.example.myapplication4.data.model.PendingSyncData
import androidx.room.TypeConverters
import com.example.myapplication4.data.database.converters.FloatArrayConverter

@Database(entities = [User::class, PendingSyncData::class], version = 1, exportSchema = false)
@TypeConverters(FloatArrayConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun pendingSyncDao(): PendingSyncDao
}