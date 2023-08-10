package ru.yakushevvv.mlkit.demo.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.view.View
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Виджет для отрисовки в несколько слоев изображения с камеры и масок, полученных от MLKit
 *
 * @param context контекст текущей Activity
 */
class AnalysisView(
    context: Context
) : View(context) {

    private var params = LayerParams()
    private val matrix = Matrix()
    private val lock = ReentrantLock()

    private var viewLayers: List<Layer> = emptyList()
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var needUpdateParams: Boolean = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        lock.withLock {
            updateTransformationIfNeeded()
            viewLayers.forEach { it.onDraw(canvas, matrix, params) }
        }
    }

    /**
     * Получение слоев для отображения, параметров изображения с камеры и перерисовка View
     *
     * @param width ширина изображения с камеры
     * @param height высота изображения с камеры
     */
    fun invalidate(width: Int, height: Int, layers: List<Layer>) {
        lock.withLock {
            imageWidth = width
            imageHeight = height
            viewLayers = layers
            needUpdateParams = true
            postInvalidate()
        }
    }

    private fun updateTransformationIfNeeded() {
        if (!needUpdateParams || imageWidth <= 0 || imageHeight <= 0) {
            return
        }

        val viewAspectRatio = width.toFloat() / imageWidth
        val imageAspectRatio: Float = imageWidth.toFloat() / imageHeight
        var offsetX = 0f
        var offsetY = 0f
        val scale: Float
        if (viewAspectRatio < imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scale = width.toFloat() / imageWidth
            offsetY = -(width.toFloat() / imageAspectRatio - height) / 2
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scale = height.toFloat() / imageHeight
            offsetX = -(height.toFloat() * imageAspectRatio - width) / 2
        }

        with(matrix) {
            reset()
            setScale(scale, scale)
            postTranslate(offsetX, offsetY)
            postScale(-1f, 1f, width / 2f, height / 2f)
        }

        params = LayerParams(
            width = width, height = height,
            scale = scale, offsetX = offsetX, offsetY = offsetY
        )
        needUpdateParams = false
    }

    /**
     * Слои для отображения изображения с камеры и масок полученных от MLKit
     */
    interface Layer {
        /**
         * Отрисовка слоя на [Canvas]
         *
         * @param canvas [Canvas] для отрисовки слоя
         * @param params параметры отображения с учетом исходных размеров изображения с камеры
         */
        fun onDraw(canvas: Canvas, matrix: Matrix, params: LayerParams)
    }

    /**
     * Параметры для трансформации изображения
     *
     * @param width ширина [View]
     * @param height высота [View]
     * @param scale масштабирование с учетом исходного размера изображения
     * @param offsetX сдвиг по оси X с учетом масштабирования
     * @param offsetY сдвиг по оси Y с учетом масштабирования
     */
    data class LayerParams(
        val width: Int = 0,
        val height: Int = 0,
        val scale: Float = 1f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f
    )
}
