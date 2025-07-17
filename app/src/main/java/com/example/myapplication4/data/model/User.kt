package com.example.myapplication4.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    val userId: Int? = null,
    val name: String,
    val email: String,
    val embeddings: ByteArray,
    val role: String = "user"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (localId != other.localId) return false
        if (userId != other.userId) return false
        if (name != other.name) return false
        if (email != other.email) return false
        if (!embeddings.contentEquals(other.embeddings)) return false
        if (role != other.role) return false

        return true
    }

    override fun hashCode(): Int {
        var result = localId
        result = 31 * result + (userId ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + embeddings.contentHashCode()
        result = 31 * result + role.hashCode()
        return result
    }
}