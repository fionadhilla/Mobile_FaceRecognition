package com.example.myapplication4.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication4.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE localId = :localId")
    fun getUserByLocalId(localId: Int): Flow<User?>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("DELETE FROM users WHERE localId = :localId")
    suspend fun deleteUserByLocalId(localId: Int)

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserByBackendId(userId: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): Flow<User?>
}