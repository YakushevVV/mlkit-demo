package ru.yakushevvv.mlkit.demo.ui.analyzer

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.segmentation.Segmenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ru.yakushevvv.mlkit.demo.ui.util.get
import ru.yakushevvv.mlkit.demo.ui.widget.AnalysisView
import ru.yakushevvv.mlkit.demo.ui.widget.layer.FaceDetectorLayer
import ru.yakushevvv.mlkit.demo.ui.widget.layer.SegmentationMaskLayer

/**
 * Анализатор изображения для MLKit, содержащий в себе [Segmenter] для выделения
 * людей на изображении и [FaceMeshDetector] для выделения их лиц
 *
 * @param view представление, на котором будет отображаться изображение с камеры и распознанные образы
 * @param segmenter настроенный анализатор для поиска людей на фото
 * @param faceDetector настроенный анализатор для поиска лиц на фото
 */
class CameraAnalyzer(
    private val view: AnalysisView,
    private val segmenter: Segmenter,
    private val faceDetector: FaceMeshDetector
) : ImageAnalysis.Analyzer {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        scope.launch {
            // получаем `InputImage` с учетом текущего поворота изображения
            val inputImage =
                InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

            // Выполняем обработку изображений для поиска людей и лиц.
            // Используется собственный extension-метод `async()` для
            // синхронного получения результата.
            val faces = async { faceDetector.process(inputImage).get() }
            val selfieMask = async { segmenter.process(inputImage).get() }

            // формируем слои от нижнего к верхнему: изображение с камеры,
            // маска выделяющая людей и прямоугольники с границами лиц
            val layers = listOf(
                SegmentationMaskLayer(selfieMask.await()),
                FaceDetectorLayer(faces.await())
            )
            // отправляем параметры изображения и слои для отрисовки
            val imageInfo = imageProxy.imageInfo
            if (imageInfo.rotationDegrees == 0 || imageInfo.rotationDegrees == 180) {
                view.invalidate(imageProxy.width, imageProxy.height, layers)
            } else {
                view.invalidate(imageProxy.height, imageProxy.width, layers)
            }

            // освобождаем изображение, чтобы анализатор мог получить следующее
            imageProxy.close()
        }
    }
}