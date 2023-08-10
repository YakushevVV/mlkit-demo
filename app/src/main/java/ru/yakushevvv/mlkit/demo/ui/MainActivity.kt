package ru.yakushevvv.mlkit.demo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import ru.yakushevvv.mlkit.demo.R
import ru.yakushevvv.mlkit.demo.ui.analyzer.CameraAnalyzer
import ru.yakushevvv.mlkit.demo.ui.theme.SelfieSegmentationTheme
import ru.yakushevvv.mlkit.demo.ui.widget.AnalysisView

/**
 * Activity для демонстрации работы MLKit
 */
class MainActivity : ComponentActivity() {

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Без разрешения на работу с камерой продолжать нет смысла. Поэтому ругаемся
            // и снова просим разрешение.
            Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_LONG)
                .show()
            requestPermission()
        }
    }

    // Объект для анализа изображения с камеры и поиска на нем людей.
    // Настроен на поиск в потоковом режиме и возвращаем маску в исходном размере изображения
    private val selfieSegmenter = Segmentation.getClient(
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .build()
    )

    // Объект для поиска лиц на изображении. Из функционала используем только
    // поиск прямоугольника для выделения лица.
    private val faceDetector = FaceMeshDetection.getClient(
        FaceMeshDetectorOptions.Builder()
            .setUseCase(FaceMeshDetectorOptions.BOUNDING_BOX_ONLY)
            .build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SelfieSegmentationTheme {
                CameraView(selfieSegmenter, faceDetector)
            }
        }
        requestPermission()
    }

    private fun requestPermission() {
        val cameraGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!cameraGranted) {
            permissionRequest.launch(Manifest.permission.CAMERA)
        }
    }
}

/**
 * Получаем виджет для отображения.
 *
 * Создаем [контроллер камеры][LifecycleCameraController] для работы с камерой и настраиваем
 * его для работы со сценарием анализа изображений. В качестве виджета для отображения
 * слоев распознавания образов используем нашу собственную реализацию - [AnalysisView].
 *
 * @param segmenter объект, для распознавания людей на изображении
 * @param faceDetector объект, для распознавания лиц на изображении
 */
@Composable
fun CameraView(
    segmenter: Segmenter,
    faceDetector: FaceMeshDetector
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisView = remember { AnalysisView(context) }
    val previewView = remember { PreviewView(context) }
    val cameraAnalyzer = CameraAnalyzer(analysisView, segmenter, faceDetector)

    // Создаем провайдер камеры, который настраиваем на фронтальную камеру и наш
    // сценарий для обработки изображений.
    val cameraController = LifecycleCameraController(context).also {
        it.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        it.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        it.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context), cameraAnalyzer)
        it.bindToLifecycle(lifecycleOwner)
    }

    previewView.controller = cameraController

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        AndroidView(factory = { analysisView }, modifier = Modifier.fillMaxSize())
    }
}
