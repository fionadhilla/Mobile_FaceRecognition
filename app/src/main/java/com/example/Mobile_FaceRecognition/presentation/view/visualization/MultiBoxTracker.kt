package com.example.Mobile_FaceRecognition.presentation.view.visualization

import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import android.util.TypedValue
import com.example.Mobile_FaceRecognition.domain.model.FaceRecognition
import com.example.Mobile_FaceRecognition.utils.ImageUtils
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

/** A tracker that handles non‑max suppression and matches existing objects to new detections. */
class MultiBoxTracker(context: Context) {

    // ───── internal data ─────
    private val screenRects = LinkedList<Pair<Float, RectF>>()
    private val trackedObjects = LinkedList<TrackedRecognition>()
    private val boxPaint = Paint()
    private val textSizePx: Float
    private val borderedText: BorderedText

    // konfigurasi frame
    private var frameToCanvasMatrix: Matrix? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var sensorOrientation = 0
    private var isConfigured = false

    init {
        // warna kotak
        boxPaint.color = Color.RED
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 10f
        boxPaint.strokeCap = Paint.Cap.ROUND
        boxPaint.strokeJoin = Paint.Join.ROUND
        boxPaint.strokeMiter = 100f

        textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.resources.displayMetrics
        )
        borderedText = BorderedText(textSizePx)
    }

    // ══════════════════════════ public api ══════════════════════════════

    @Synchronized
    fun setFrameConfiguration(width: Int, height: Int, sensorOrientation: Int) {
        frameWidth = width
        frameHeight = height
        this.sensorOrientation = sensorOrientation
        isConfigured = true
    }

    /** hapus semua tracking (dipanggil saat switch kamera) */
    @Synchronized
    fun clear() {
        screenRects.clear()
        trackedObjects.clear()
    }

    @Synchronized
    fun trackResults(results: List<FaceRecognition>, timestamp: Long) {
        if (!isConfigured) {
            Log.w(TAG, "Tracker not configured yet, skip trackResults.")
            Log.d("DBG_TR","track size=${results.size}")
            return
        }
        processResults(results)
    }

    @Synchronized
    fun draw(canvas: Canvas) {
        if (!isConfigured) {
            Log.w(TAG, "Tracker not configured yet, skip draw.")
            return
        }
        rebuildMatrixIfNeeded(canvas) // Membangun ulang matriks transformasi
        val matrix = frameToCanvasMatrix ?: return

        for (rec in trackedObjects) {
            val trackedPos = RectF(rec.location)
            matrix.mapRect(trackedPos) // Mengaplikasikan transformasi ke bounding box
            Log.d("DBG_POS", "rect=$trackedPos  canvas=${canvas.width}x${canvas.height}")

            boxPaint.color = rec.color
            val corner = min(trackedPos.width(), trackedPos.height()) / 8f
            canvas.drawRoundRect(trackedPos, corner, corner, boxPaint)

            val label = if (!TextUtils.isEmpty(rec.title)) {
                String.format("%s %.2f", rec.title, rec.detectionConfidence)
            } else {
                String.format("%.2f", rec.detectionConfidence)
            }
            borderedText.drawText(
                canvas, trackedPos.left + corner, trackedPos.top, label, boxPaint
            )
        }
        Log.d("DBG_TR","draw size=${trackedObjects.size}")
    }

    private fun rebuildMatrixIfNeeded(canvas: Canvas) {

        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            canvas.width,
            canvas.height,
            sensorOrientation,
            /*maintainAspect=*/ true
        )
    }

    private fun processResults(results: List<FaceRecognition>) {
        Log.d("DBG", "processResults size=${results.size}")
        val rectsToTrack = LinkedList<Pair<Float, FaceRecognition>>()

        screenRects.clear()

        for (result in results) {
            val loc = result.location ?: continue
            rectsToTrack.add(Pair(result.distance ?: 0f, result))
        }

        trackedObjects.clear()
        if (rectsToTrack.isEmpty()) return

        for (cand in rectsToTrack) {
            val tr = TrackedRecognition().apply {
                detectionConfidence = cand.first
                location = RectF(cand.second.location) // Location already in frame coordinates
                title = cand.second.title
                color = COLORS[trackedObjects.size % COLORS.size]
            }
            trackedObjects.add(tr)
            if (trackedObjects.size >= COLORS.size) break
        }
    }

    private class TrackedRecognition {
        var location: RectF = RectF()
        var detectionConfidence = 0f
        var color = 0
        var title: String? = null
    }

    companion object {
        private const val TAG = "MultiBoxTracker"
        private const val TEXT_SIZE_DIP = 18f
        private const val MIN_SIZE = 16f
        private val COLORS = intArrayOf(
            Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA,
            Color.WHITE, Color.parseColor("#55FF55"), Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"), Color.parseColor("#AAAAFF"), Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"), Color.parseColor("#AA33AA"), Color.parseColor("#0D0068")
        )
    }
}