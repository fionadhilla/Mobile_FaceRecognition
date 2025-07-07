package com.example.Mobile_FaceRecognition.presentation.view.visualization

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.LinkedList

/** A simple View providing a render callback to other classes. */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val callbacks = LinkedList<(Canvas) -> Unit>()

    fun addCallback(callback: (Canvas) -> Unit) {
        callbacks.add(callback)
    }

    @SuppressLint("MissingSuperCall")
    @Synchronized
    override fun draw(canvas: Canvas) {
        for (callback in callbacks) {
            callback(canvas)
        }
    }

    /** Interface defining the callback for client classes. */
    fun interface DrawCallback {
        fun drawCallback(canvas: Canvas)
    }
}