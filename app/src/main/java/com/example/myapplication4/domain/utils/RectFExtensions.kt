package com.example.myapplication4.domain.utils

import android.graphics.RectF

fun RectF.flipHorizontal(imageWidth: Float): RectF {
    val flippedLeft = imageWidth - this.right
    val flippedRight = imageWidth - this.left
    return RectF(flippedLeft, this.top, flippedRight, this.bottom)
}