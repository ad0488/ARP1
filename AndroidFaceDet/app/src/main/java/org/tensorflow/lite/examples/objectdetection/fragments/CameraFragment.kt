/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tensorflow.lite.examples.objectdetection.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import org.tensorflow.lite.examples.objectdetection.*
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.data.Device
import org.tensorflow.lite.examples.objectdetection.data.Face
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt


class CameraFragment :
    Fragment(),
    YoloDetector.FaceDetectorListener,
    YawnClassifier.YawnClassifierListener,
    EyeClassifier.EyeClassifierListener
{

    private val TAG = "ObjectDetection"

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var yoloDetector: YoloDetector
    private lateinit var yawnClassifier: YawnClassifier
    private lateinit var eyeClassifier: EyeClassifier
    private lateinit var mainClassifier: MainClassifier
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var totalInferenceTime: Long = 0

    private lateinit var tinyDB : TinyDB;

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tinyDB = TinyDB(activity)
        clearData()

        yoloDetector = YoloDetector.create(
            context = requireContext(),
            device = Device.GPU,
            faceDetectorListener = this
        )

        yawnClassifier = YawnClassifier.create(
            context = requireContext(),
            device = Device.GPU,
            yawnClassifierListener = this
        )

        eyeClassifier = EyeClassifier.create(
            context = requireContext(),
            device = Device.GPU,
            eyeClassifierListener = this
        )

        mainClassifier = MainClassifier()

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        val finishBtn: Button = view.findViewById(R.id.finish_btn)
        finishBtn.setOnClickListener{
            Log.d(TAG, "Finish button pressed")
            saveData()
            val intent = Intent(activity, MainMenuActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveData(){
        // save data to shared preferences
        tinyDB.putListDouble("fom", mainClassifier.f_fomPredictions)
        tinyDB.putListDouble("perclos", mainClassifier.f_perclosPredictions)
        tinyDB.putListDouble("drowsy", mainClassifier.f_drowsyPredictions)
    }

    private fun clearData(){
        tinyDB.remove("fom")
        tinyDB.remove("perclos")
        tinyDB.remove("drowsy")
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                              image.width,
                              image.height,
                              Bitmap.Config.ARGB_8888
                            )
                        }

                        detectObjects(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees

        // Reset inference time before start
        totalInferenceTime = 0
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        val face = yoloDetector.detect(bitmapBuffer, imageRotation)
        // detect yawning
        yawnClassifier.classify(bitmapBuffer, face)
        // detect eyes closed
        eyeClassifier.classify(bitmapBuffer, face, predictLeftEye = true)
        eyeClassifier.classify(bitmapBuffer, face, predictLeftEye = false)
        mainClassifier.predictFinal()
        onDisplayDrowsiness()
        updateBottomShelfUI()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResultsFace(
        result: Face?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        totalInferenceTime += inferenceTime
        activity?.runOnUiThread {
            // Pass necessary information to OverlayView for drawing on the canvas
            fragmentCameraBinding.overlay.setResults(
                detectionResults = result,
                imageHeight = imageHeight,
                imageWidth = imageWidth
            )

            // Force a redraw
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onErrorFace(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResultsYawn(result: Int, inferenceTime: Long) {
        totalInferenceTime += inferenceTime
        activity?.runOnUiThread {
            view?.findViewById<TextView>(R.id.yawn_class)?.text = result.toString()
        }
        mainClassifier.addPrediction(result, type="yawn")
    }

    override fun onErrorYawn(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResultsEye(result: Int, inferenceTime: Long, predictLeftEye: Boolean) {
        totalInferenceTime += inferenceTime
        var type = "leftEye"
        if (!predictLeftEye){
            type = "rightEye"
            activity?.runOnUiThread {
                view?.findViewById<TextView>(R.id.right_eye_class)?.text = result.toString()
            }
        }
        else {
            activity?.runOnUiThread {
                view?.findViewById<TextView>(R.id.left_eye_class)?.text = result.toString()
            }
        }
        mainClassifier.addPrediction(result, type=type)
    }

    override fun onErrorEye(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onDisplayDrowsiness() {
        activity?.runOnUiThread {
            val dr = mainClassifier.drowsiness_level
            val roundoff = (dr * 10000).roundToInt().toDouble() / 10000
            view?.findViewById<TextView>(R.id.drowsiness_level)?.text = roundoff.toString()
        }
    }

    private fun updateBottomShelfUI(){
        Log.e(TAG, "Total inference time: "+totalInferenceTime.toString() + " ms")
        activity?.runOnUiThread {
            view?.findViewById<TextView>(R.id.inference_time_val)?.text = totalInferenceTime.toString()+" ms"
        }
    }

}

