# AR Detector - Intelligent Inventory Scanner

An Android application designed for real-time product detection and inventory counting.

## Key Features

* **Live Product Detection:** Utilizes the `TensorFlow Lite (EfficientDet-Lite2)` model to identify shelf items with high accuracy, filtering out non-product entities (walls, floors, people).
* **Stabilized Counting (Euclidean Tracking):** Implemented a custom spatial tracker that correlates new detections with existing ones based on screen coordinates. This prevents the "flicker" issue where a single object is counted multiple times due to hand tremors.
* **Rotation-Aware Inference:** Features a custom bitmap transformation pipeline to handle device rotation (Portrait/Landscape) correctly, ensuring the AI model receives the image in the expected orientation.
* **User-Controlled Workflow:** A "Pause/Resume" state machine allows warehouse workers to freeze the count before moving to the next shelf, ensuring data integrity.
* **Smart Filtering:**
    * **Confidence Threshold:** Strict 50% confidence requirement.
    * **Blocklist logic:** Explicitly ignores non-retail classes (e.g., "dining table", "person") to reduce false positives.

## Tech Stack & Architecture

* **Language:** Kotlin
* **Camera:** Android CameraX (ImageAnalysis Use Case)
* **ML Engine:** TensorFlow Lite Task Vision
* **UI/UX:** Programmatic UI (View System) - Chosen for rapid prototyping and zero XML overhead.

### File Structure
* **`MainActivity.kt`**: Manages the UI state, Camera lifecycle, and threading. Acts as the controller.
* **`ObjectDetectorHelper.kt`**: Has the "Business Logic." Handles TFLite inference, bitmap rotation math, and the object tracking algorithm.
* **`OverlayView.kt`**: A custom `View` implementation for high-performance rendering of bounding boxes and status badges.

## Setup Instructions

1.  **Clone the repository.**
2.  **Open in Android Studio** (i did in otter version)
3.  **Sync Gradle**
4.  **Connect Device:** Enable USB Debugging.
5.  **Build & Run.**
