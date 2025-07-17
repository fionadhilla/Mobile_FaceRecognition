package com.example.myapplication4.data.repository

import com.example.myapplication4.data.database.dao.UserDao
import com.example.myapplication4.data.database.dao.PendingSyncDao
import com.example.myapplication4.data.model.User
import com.example.myapplication4.data.model.PendingSyncData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val pendingSyncDao: PendingSyncDao
) : UserRepository {

    override suspend fun saveUserWithFace(user: User): Boolean {
        return try {
            val localId = userDao.insertUser(user).toInt()
            pendingSyncDao.insertPendingSync(PendingSyncData(userLocalId = localId, action = "ADD"))
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getUserById(localId: Int): Flow<User?> {
        return userDao.getUserByLocalId(localId)
    }

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }

    override suspend fun deleteUser(localId: Int): Boolean {
        return try {
            userDao.deleteUserByLocalId(localId)
            pendingSyncDao.insertPendingSync(PendingSyncData(userLocalId = localId, action = "DELETE"))
            true
        } catch (e: Exception) {
            false
        }
    }
}


