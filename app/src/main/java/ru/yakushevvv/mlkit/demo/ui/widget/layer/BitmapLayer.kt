package ru.yakushevvv.mlkit.demo.ui.widget.layer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import ru.yakushevvv.mlkit.demo.ui.widget.AnalysisView

/**
 * Слой для отображения изображения с камеры
 *
 * @param bitmap изображение с камеры, преобразованное в [Bitmap]
 */
class BitmapLayer(private val bitmap: Bitmap) : AnalysisView.Layer {
    override fun onDraw(canvas: Canvas, matrix: Matrix, params: AnalysisView.LayerParams) {
        canvas.drawBitmap(bitmap, matrix, null)
    }
}