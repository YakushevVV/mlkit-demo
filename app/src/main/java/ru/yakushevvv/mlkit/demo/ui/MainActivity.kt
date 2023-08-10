package ru.yakushevvv.mlkit.demo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
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
            Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_LONG).show()
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

    // Сценарий для анализа изображений с камеры. Говорим, что будем работать только
    // с последним использованным изображением и не будем хранить пул изображений.
    // Также для меньших затрат на масштабирование, выбираем размеры изображения в
    // пропорции 16:9. Получаемые изображения просим в YUV-формате.
    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SelfieSegmentationTheme {
                CameraView(imageAnalysis, selfieSegmenter, faceDetector)
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
 * Создаем [провайдер][ProcessCameraProvider] для работы с камерой и передаем
 * в него [сценарий анализа изображений][imageAnalysis]. В качестве виджета используем
 * нашу собственную реализацию - [AnalysisView]. Данный виджет призван в один момент
 * отображать и изображение с камеры, и слои, полученные от MLKit. Если не отображать
 * одновременно, то при движении распознанные слои будут отставать от изображения.
 *
 * @param imageAnalysis сценарий для анализа изображений с камеры
 * @param segmenter объект, для распознавания людей на изображении
 * @param faceDetector объект, для распознавания лиц на изображении
 */
@Composable
fun CameraView(
    imageAnalysis: ImageAnalysis,
    segmenter: Segmenter,
    faceDetector: FaceMeshDetector
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisView = remember { AnalysisView(context) }
    val previewView = remember { PreviewView(context) }

    // Передаем в сценарий анализа изображений наш анализатор, который будет
    // передавать изображение в модели для поиска лиц и людей на изображении.
    // Потом на основе ответа от моделей формировать слои и передавать их в AnalysisView.
    imageAnalysis.setAnalyzer(
        ContextCompat.getMainExecutor(context),
        CameraAnalyzer(
            analysisView,
            segmenter,
            faceDetector
        )
    )

    val preview = Preview.Builder().build()

    // Создаем провайдер камеры, который настраиваем на фронтальную камеру и наш
    // сценарий для обработки изображений.
    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    LaunchedEffect(cameraSelector) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        AndroidView({ analysisView }, modifier = Modifier.fillMaxSize())
    }
}
