package com.example.ardetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.widget.Toast
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.support.image.TensorImage
import kotlin.math.hypot

class ObjectDetectorHelper(private val context: Context) {

    private var detector: ObjectDetector? = null

    // List to track the objects detected
    data class TrackedObject(var label: String, var rect: RectF)
    val trackedObjects = mutableListOf<TrackedObject>()

    // Configuration of the AI model that we are using
    private val modelName = "model.tflite"
    private val confidenceThreshold = 0.25f // 25% confidence then only we will track the object
    private val trackingDistanceThreshold = 250f // Pixels to snap to existing object

    // Blocklist of the objects that we don't want to track
    private val blockedLabels = setOf(
        "person", "dining table", "bed", "couch", "chair",
        "bench", "toilet", "keyboard"
    )

    init {
        setupDetector()
    }


    // function to setup the AI model
    private fun setupDetector() {
        try {
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(5)
                .setScoreThreshold(confidenceThreshold)
                .build()
            detector = ObjectDetector.createFromFileAndOptions(context, modelName, options)
        } catch (e: Exception) {
            Toast.makeText(context, "AI Init Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // function to process the image and detect the objects
    fun processImage(bitmap: Bitmap, rotation: Int, screenWidth: Int, screenHeight: Int): List<RectF> {
        if (detector == null) return emptyList()

        // Fix the rotation so our model sees it correctly
        val rotatedBitmap = if (rotation != 0) rotateBitmap(bitmap, rotation.toFloat()) else bitmap

        // Run the model
        val tensorImage = TensorImage.fromBitmap(rotatedBitmap)
        val results = detector!!.detect(tensorImage)

        // Scale and filter
        val boxesToDraw = mutableListOf<RectF>()
        val scaleX = screenWidth.toFloat() / rotatedBitmap.width
        val scaleY = screenHeight.toFloat() / rotatedBitmap.height

        for (detected in results) {
            val box = detected.boundingBox
            val label = detected.categories.firstOrNull()?.label ?: "Unknown"

            // Filters
            if (blockedLabels.contains(label)) continue
            if (box.width() < rotatedBitmap.width * 0.1f) continue // minimum size to be considered a valid object


            // Scale to screen size
            val screenRect = RectF(
                box.left * scaleX,
                box.top * scaleY,
                box.right * scaleX,
                box.bottom * scaleY
            )

            // Smart tracking so we don't add the same object multiple times
            updateTracking(label, screenRect)
            boxesToDraw.add(screenRect)
        }
        return boxesToDraw
    }

    private fun updateTracking(label: String, newRect: RectF) {
        var matchFound = false
        val cx = newRect.centerX()
        val cy = newRect.centerY()

        for (tracked in trackedObjects) {
            val distance = hypot(cx - tracked.rect.centerX(), cy - tracked.rect.centerY())
            if (tracked.label == label && distance < trackingDistanceThreshold) {
                tracked.rect = newRect // Update position, don't add new as its the same product
                matchFound = true
                break
            }
        }

        if (!matchFound) {
            trackedObjects.add(TrackedObject(label, newRect))
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun getCount(): Int = trackedObjects.size
}
