package ru.yakushevvv.mlkit.demo.ui.widget.layer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.google.mlkit.vision.facemesh.FaceMesh
import ru.yakushevvv.mlkit.demo.ui.widget.AnalysisView
import kotlin.math.max
import kotlin.math.min

/**
 * Слой для отображения границы распознанных на изображении лиц
 *
 * @param faceMeshes список с распознанными лицами
 */
class FaceDetectorLayer(private val faceMeshes: List<FaceMesh>) : AnalysisView.Layer {

    override fun onDraw(canvas: Canvas, matrix: Matrix, params: AnalysisView.LayerParams) {
        faceMeshes.forEach { faceMesh ->
            val faceRect = faceMesh.boundingBox
            val left = min(faceRect.left, faceRect.right)
            val right = max(faceRect.left, faceRect.right)

            val rect = Rect(
                translateX(left, params), translateY(faceRect.top, params),
                translateX(right, params), translateY(faceRect.bottom, params)
            )
            canvas.drawRect(rect, PAINT)
        }
    }

    private fun translateX(value: Int, params: AnalysisView.LayerParams): Int =
        params.width - (value * params.scale + params.offsetX).toInt()

    private fun translateY(value: Int, params: AnalysisView.LayerParams): Int =
        (value * params.scale + params.offsetY).toInt()

    companion object {
        private val PAINT = Paint().apply {
            color = Color.argb(255, 255, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
    }
}