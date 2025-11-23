package com.example.ardetector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Architecture components
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detectorHelper: ObjectDetectorHelper

    // UI components
    private lateinit var overlayView: OverlayView
    private lateinit var previewView: PreviewView
    private lateinit var countValue: TextView
    private lateinit var statusBadge: TextView
    private lateinit var pauseButton: TextView
    private lateinit var startScreen: FrameLayout
    private lateinit var hudContainer: FrameLayout

    // App state
    private var isPaused = false
    private var isScanningActive = false

    // function to create the interface
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize logic
        detectorHelper = ObjectDetectorHelper(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Build the user interface
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        // 1st layer is camera preview
        previewView = PreviewView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        root.addView(previewView)

        // 2nd layer is the boxes we draw
        overlayView = OverlayView(this, null).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.TRANSPARENT)
        }
        root.addView(overlayView)

        // 3rd layer is Heads up display (Hidden initially)
        hudContainer = createHud()
        hudContainer.visibility = View.GONE
        root.addView(hudContainer)

        // 4th layer is Start screen (Visible initially)
        startScreen = createStartScreen()
        root.addView(startScreen)

        setContentView(root)
    }

    // Camera pipeline
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Image analysis
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy -> processFrame(imageProxy) }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processFrame(imageProxy: ImageProxy) {
        // if user has clicked pause, then dont process the frame
        if (!isScanningActive || isPaused) {
            imageProxy.close()
            return
        }

        // Convert image to Bitmap
        val bitmap = imageProxy.toBitmap() ?: run {
            imageProxy.close()
            return
        }

        // Get rotation degrees from the phone
        val rotation = imageProxy.imageInfo.rotationDegrees

        val boxes = detectorHelper.processImage(bitmap, rotation, previewView.width, previewView.height)
        val count = detectorHelper.getCount()

        // UI Update
        runOnUiThread {
            overlayView.setResults(boxes)
            countValue.text = "$count"
        }

        imageProxy.close()
    }

    // Constructing the UI of the app
    private fun createStartScreen(): FrameLayout {
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#111111"))
            isClickable = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val title = TextView(this).apply {
            text = "AR Product Detection"
            textSize = 32f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        content.addView(title)

        val subtitle = TextView(this).apply {
            text = "Intelligent Inventory Scanner"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 100)
        }
        content.addView(subtitle)

        // Start button
        val startBtn = Button(this).apply {
            text = "START SESSION"
            textSize = 18f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#00FF00"))
                cornerRadius = 100f
            }
            layoutParams = LinearLayout.LayoutParams(600, 150)

            setOnClickListener {
                checkPermissionsAndStart()
            }
        }
        content.addView(startBtn)
        container.addView(content)
        return container
    }

    // function to create the Heads Up Display
    private fun createHud(): FrameLayout {
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        // Top status badge
        statusBadge = TextView(this).apply {
            text = "● LIVE"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#80000000"))
                cornerRadius = 50f
            }
            setPadding(30, 15, 30, 15)
            layoutParams = FrameLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = 80
            }
        }
        container.addView(statusBadge)

        // Bottom control panel
        val bottomPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE222222"))
                cornerRadii = floatArrayOf(60f,60f,60f,60f,0f,0f,0f,0f)
            }
            setPadding(60, 60, 60, 60)
            layoutParams = FrameLayout.LayoutParams(-1, 350).apply {
                gravity = Gravity.BOTTOM
            }
        }

        // Counter at the left side
        val counterLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val label = TextView(this).apply {
            text = "Total Items"
            textSize = 14f
            setTextColor(Color.GRAY)
        }
        countValue = TextView(this).apply {
            text = "0"
            textSize = 48f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        counterLayout.addView(label)
        counterLayout.addView(countValue)
        bottomPanel.addView(counterLayout)

        // Pause/Resume button at the right side
        pauseButton = TextView(this).apply {
            text = "⏸"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                shape = GradientDrawable.OVAL
            }
            layoutParams = LinearLayout.LayoutParams(140, 140)
            setOnClickListener { togglePause() }
        }
        bottomPanel.addView(pauseButton)
        container.addView(bottomPanel)

        return container
    }

    // State management
    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScanner()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScanner() {
        // Animation: Fade out Start Screen, Fade in HUD
        startScreen.animate().alpha(0f).setDuration(400).withEndAction {
            startScreen.visibility = View.GONE
            hudContainer.alpha = 0f
            hudContainer.visibility = View.VISIBLE
            hudContainer.animate().alpha(1f).duration = 400

            isScanningActive = true
            startCamera()
        }
    }

    private fun togglePause() {
        isPaused = !isPaused
        if (isPaused) {
            pauseButton.text = "▶"
            pauseButton.background = GradientDrawable().apply { setColor(Color.GREEN); shape = GradientDrawable.OVAL }
            statusBadge.text = "⏸ PAUSED"
            statusBadge.setTextColor(Color.YELLOW)
            overlayView.setResults(emptyList())
        } else {
            pauseButton.text = "⏸"
            pauseButton.background = GradientDrawable().apply { setColor(Color.WHITE); shape = GradientDrawable.OVAL }
            statusBadge.text = "● LIVE"
            statusBadge.setTextColor(Color.WHITE)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchScanner() else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }
}