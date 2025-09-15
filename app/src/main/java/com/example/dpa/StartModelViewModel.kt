package com.example.dpa

import android.app.Application
import android.content.Context
import android.graphics.RectF
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

typealias FaceLandmarkerResultBundle = com.example.dpa.FaceLandmarkerHelper.ResultBundle

class StartModelViewModel(
    application: Application,
    private val repository: DrowsinessRepository
) : AndroidViewModel(application) {

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    private val _earState = MutableStateFlow(0.0f)
    val earState: StateFlow<Float> = _earState.asStateFlow()

    private val _faceBox = mutableStateOf<RectF?>(null)
    val faceBox: State<RectF?> = _faceBox

    private val _imageWidth = mutableStateOf(0f)
    val imageWidth: State<Float> = _imageWidth

    private val _imageHeight = mutableStateOf(0f)
    val imageHeight: State<Float> = _imageHeight

    private val _isWarningActive = MutableStateFlow(false)
    val isWarningActive: StateFlow<Boolean> = _isWarningActive.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var isMediaPlayerInitialized = false

    private val vibrator: Vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private val drowsyCheck = DrowsyCheck()
    private var eyeClosureStartTime: Long = 0L

    private val settingsDataStore = SettingsDataStore(application.applicationContext)
    private val currentSettings: Flow<AppSettings> = settingsDataStore.appSettingsFlow

    private var faceLandmarkerHelper: FaceLandmarkerHelper? = null

    init {
        // init 블록에서는 faceLandmarkerHelper 생성을 제거합니다.
        // 설정 값 변경 감지 로직만 남겨둡니다.
        viewModelScope.launch {
            currentSettings.collect { settings ->
                Log.d("StartModelViewModel", "AppSettings changed: $settings")
                if (mediaPlayer != null && isMediaPlayerInitialized) {
                    mediaPlayer?.setVolume(settings.volumeLevel, settings.volumeLevel)
                }
            }
        }
    }

    private val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { newSurfaceRequest ->
            _surfaceRequest.update { newSurfaceRequest }
        }
    }

    private val imageAnalysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { imageAnalysis ->
            imageAnalysis.setAnalyzer( // setAnalyzer 호출 시작
                Executors.newSingleThreadExecutor(),
                ImageAnalysis.Analyzer { imageProxy ->
                    try {
                        faceLandmarkerHelper?.detectLiveStream(imageProxy, false)
                    } finally {
                        imageProxy.close()
                    }
                } // Analyzer 람다 블록 끝
            ) // 여기에 setAnalyzer에 대한 닫는 괄호 ')'를 추가합니다.
        }


    suspend fun bindToCamera(lifecycleOwner: LifecycleOwner) {
        if (faceLandmarkerHelper == null) {
            setupFaceLandmarkerHelper()
        }

        val appContext = getApplication<Application>().applicationContext
        val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
        try {
            processCameraProvider.unbindAll()
            processCameraProvider.bindToLifecycle(
                lifecycleOwner, DEFAULT_FRONT_CAMERA, cameraPreviewUseCase, imageAnalysisUseCase
            )
            awaitCancellation()
        } catch (exc: Exception) {
            Log.e("StartModelViewModel", "Use case binding failed", exc)
        }
    }

    private fun setupFaceLandmarkerHelper() {
        faceLandmarkerHelper = FaceLandmarkerHelper(
            context = getApplication<Application>().applicationContext,
            faceLandmarkerHelperListener = object : FaceLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String, errorCode: Int) {
                    Log.e("StartModelViewModel", "FaceLandmarker Error: $error")
                    stopAllWarnings()
                }

                override fun onResults(resultBundle: FaceLandmarkerResultBundle) {
                    val ear = drowsyCheck.drowsyCheck(resultBundle)
                    _earState.update { ear.toFloat() }

                    viewModelScope.launch {
                        repository.saveEarValue(ear.toFloat())
                    }

                    viewModelScope.launch {
                        val settings: AppSettings = currentSettings.first()
                        val earThreshold = settings.earThreshold
                        val eyeClosureDurationThreshold = settings.drowsinessDurationThreshold

                        if (ear.toFloat() < earThreshold) {
                            onEyesClosed(eyeClosureDurationThreshold)
                        } else {
                            onEyesOpened()
                        }
                        updateFaceBox(resultBundle)
                    }
                }

                override fun onEmpty() {
                    _earState.update { 0.0f }
                    stopAllWarnings()
                    _faceBox.value = null
                }
            }
        )
    }

    private fun updateFaceBox(resultBundle: FaceLandmarkerResultBundle) {
        val landmarks = resultBundle.result.faceLandmarks().firstOrNull()
        if (landmarks != null) {
            val boundingBox = getBoundingBoxFromLandmarks(landmarks)
            val screenBox = RectF(
                boundingBox.left * resultBundle.inputImageWidth,
                boundingBox.top * resultBundle.inputImageHeight,
                boundingBox.right * resultBundle.inputImageWidth,
                boundingBox.bottom * resultBundle.inputImageHeight
            )
            _faceBox.value = screenBox
            _imageWidth.value = resultBundle.inputImageWidth.toFloat()
            _imageHeight.value = resultBundle.inputImageHeight.toFloat()
        } else {
            _faceBox.value = null
        }
    }

    private fun onEyesClosed(durationThreshold: Long) {
        if (eyeClosureStartTime == 0L) {
            eyeClosureStartTime = System.currentTimeMillis()
        }
        val currentTime = System.currentTimeMillis()
        if (currentTime - eyeClosureStartTime >= durationThreshold) {
            triggerWarning()
        }
    }

    private fun onEyesOpened() {
        eyeClosureStartTime = 0L
        stopAllWarnings()
    }

    private fun triggerWarning() {
        if (_isWarningActive.value) return
        _isWarningActive.value = true
        viewModelScope.launch {
            repository.incrementWarningCountForToday()
        }
        viewModelScope.launch(Dispatchers.Default) {
            val settings: AppSettings = currentSettings.first()
            if (settings.isSoundEnabled) {
                playWarningSound(settings.volumeLevel)
            }
            if (settings.isVibrationEnabled) {
                vibrateWarningPattern(settings.vibrationStrength)
            }
        }
    }

    private fun stopAllWarnings() {
        if (!_isWarningActive.value && mediaPlayer?.isPlaying == false) return
        _isWarningActive.value = false
        stopWarningSound()
        vibrator.cancel()
    }

    private fun playWarningSound(volume: Float) {
        stopWarningSound()
        try {
            mediaPlayer = MediaPlayer.create(getApplication<Application>().applicationContext, R.raw.alarm_sound)?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            isMediaPlayerInitialized = true
        } catch (e: Exception) {
            Log.e("StartModelViewModel", "Error creating and starting MediaPlayer", e)
        }
    }

    private fun stopWarningSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        isMediaPlayerInitialized = false
    }

    private fun vibrateWarningPattern(strength: Int) {
        val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
        val amplitudes = intArrayOf(0, 150, 0, 200, 0, strength.coerceIn(1, 255))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseResources()
        Log.d("StartModelViewModel", "onCleared called, resources released.")
    }

    fun releaseResources() {
        faceLandmarkerHelper?.clearFaceLandmarker()
        faceLandmarkerHelper = null
        stopAllWarnings()
        _surfaceRequest.value = null
        Log.d("StartModelViewModel", "All resources released.")
    }
}

fun getBoundingBoxFromLandmarks(landmarks: List<NormalizedLandmark>): RectF {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    for (point in landmarks) {
        if (point.x() < minX) minX = point.x()
        if (point.x() > maxX) maxX = point.x()
        if (point.y() < minY) minY = point.y()
        if (point.y() > maxY) maxY = point.y()
    }
    return RectF(minX, minY, maxX, maxY)
}