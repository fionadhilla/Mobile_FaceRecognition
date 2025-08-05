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

/**
 * Menerapkan Non-Maximum Suppression (NMS) untuk menghilangkan bounding box yang tumpang tindih.
 */
fun applyNMS(boxes: List<BoundingBox>, iouThreshold: Float = 0.45f): List<BoundingBox> {
    if (boxes.isEmpty()) return emptyList()

    val sortedBoxes = boxes.sortedByDescending { it.confidence }
    val result = mutableListOf<BoundingBox>()
    val suppressed = BooleanArray(sortedBoxes.size) { false }

    for (i in sortedBoxes.indices) {
        if (suppressed[i]) continue

        val boxA = sortedBoxes[i]
        result.add(boxA)

        for (j in i + 1 until sortedBoxes.size) {
            if (suppressed[j]) continue

            val boxB = sortedBoxes[j]
            val iou = calculateIoU(boxA.rect, boxB.rect)

            if (iou > iouThreshold) {
                suppressed[j] = true
            }
        }
    }
    return result
}

