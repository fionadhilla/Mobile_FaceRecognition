package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun saveUserWithFace(user: User): Boolean
    fun getUserById(localId: Int): Flow<User?>
    fun getAllUsers(): Flow<List<User>>
    suspend fun deleteUser(localId: Int): Boolean
}