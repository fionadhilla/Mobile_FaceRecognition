package com.example.Mobile_FaceRecognition.presentation.view.visualization

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
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
        setWillNotDraw(false)
    }

    @SuppressLint("MissingSuperCall")
    @Synchronized
    override fun onDraw(canvas: Canvas) {
        for (cb in callbacks) cb(canvas)
        Log.d("DBG_OV","onDraw")
    }
}