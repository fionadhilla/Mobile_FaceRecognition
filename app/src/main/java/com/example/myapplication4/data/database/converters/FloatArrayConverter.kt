package com.example.myapplication4.data.database.converters

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FloatArrayConverter {
    @TypeConverter
    fun fromFloatArray(floatArray: FloatArray?): ByteArray? {
        if (floatArray == null) return null
        val byteBuffer = ByteBuffer.allocate(floatArray.size * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.asFloatBuffer().put(floatArray)
        return byteBuffer.array()
    }

    @TypeConverter
    fun toFloatArray(byteArray: ByteArray?): FloatArray? {
        if (byteArray == null) return null
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.nativeOrder())
        val floatArray = FloatArray(byteArray.size / 4)
        byteBuffer.asFloatBuffer().get(floatArray)
        return floatArray
    }
}