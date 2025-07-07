package com.example.Mobile_FaceRecognition.domain.model

import android.graphics.Bitmap
import android.graphics.RectF

data class FaceRecognition(
    val id: String? = null,
    val title: String,
    val distance: Float? = null,
    var location: RectF? = null,
    var embedding: FloatArray? = null,
    var crop: Bitmap? = null
) {
    override fun toString(): String {
        var resultString = ""
        if (id != null) {
            resultString += "[$id] "
        }

        if (title != null) {
            resultString += "$title "
        }

        if (distance != null) {
            resultString += String.format("(%.1f%%) ", distance * 100.0f)
        }

        if (location != null) {
            resultString += "$location "
        }

        return resultString.trim()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceRecognition

        if (id != other.id) return false
        if (title != other.title) return false
        if (distance != other.distance) return false
        if (location != other.location) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (crop != other.crop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + title.hashCode()
        result = 31 * result + (distance?.hashCode() ?: 0)
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + (crop?.hashCode() ?: 0)
        return result
    }
}
