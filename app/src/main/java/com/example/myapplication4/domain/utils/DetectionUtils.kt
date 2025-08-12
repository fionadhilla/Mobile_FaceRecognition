package com.example.myapplication4.domain.utils

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

// Data class untuk menyimpan hasil deteksi.
data class BoundingBox(
    val rect: RectF,
    val confidence: Float,
    val classId: Int
)

/**
 * Menghitung IoU (Intersection over Union) antara dua bounding box.
 */
private fun calculateIoU(rectA: RectF, rectB: RectF): Float {
    val intersection = RectF(
        max(rectA.left, rectB.left),
        max(rectA.top, rectB.top),
        min(rectA.right, rectB.right),
        min(rectA.bottom, rectB.bottom)
    )

    if (intersection.width() <= 0 || intersection.height() <= 0) return 0f

    val intersectionArea = intersection.width() * intersection.height()
    val areaA = rectA.width() * rectA.height()
    val areaB = rectB.width() * rectB.height()

    return intersectionArea / (areaA + areaB - intersectionArea)
}

fun applyNMS(boxes: List<BoundingBox>, iouThreshold: Float): List<BoundingBox> {
    val sortedBoxes = boxes.sortedByDescending { it.confidence }.toMutableList()
    val nmsBoxes = mutableListOf<BoundingBox>()

    while (sortedBoxes.isNotEmpty()) {
        val first = sortedBoxes.removeAt(0)
        nmsBoxes.add(first)

        val iterator = sortedBoxes.iterator()
        while (iterator.hasNext()) {
            val nextBox = iterator.next()
            if (calculateIoU(first.rect, nextBox.rect) >= iouThreshold) {
                iterator.remove()
            }
        }
    }
    return nmsBoxes
}

