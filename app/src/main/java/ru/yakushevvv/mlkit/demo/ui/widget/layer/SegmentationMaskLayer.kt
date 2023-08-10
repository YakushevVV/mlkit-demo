package ru.yakushevvv.mlkit.demo.ui.widget.layer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.annotation.ColorInt
import com.google.mlkit.vision.segmentation.SegmentationMask
import ru.yakushevvv.mlkit.demo.ui.widget.AnalysisView
import java.nio.ByteBuffer

/**
 * Слой для отображения маски, выделяющей людей на изображении с камеры
 *
 * Маска поставляется для свернутого изображения 256х256. Поэтому получаем в конструктор
 * размеры исходного изображения для масштабирования
 *
 * @param mask маска для выделения людей
 */
class SegmentationMaskLayer(
    private val mask: SegmentationMask
) : AnalysisView.Layer {

    override fun onDraw(canvas: Canvas, matrix: Matrix, params: AnalysisView.LayerParams) {
        val bitmap = Bitmap.createBitmap(
            maskColorsFromByteBuffer(mask.buffer),
            mask.width,
            mask.height,
            Bitmap.Config.ARGB_8888
        )
        canvas.drawBitmap(bitmap, matrix, null)

        bitmap.recycle()
        mask.buffer.rewind()
    }

    @ColorInt
    private fun maskColorsFromByteBuffer(byteBuffer: ByteBuffer): IntArray {
        @ColorInt val colors = IntArray(mask.width * mask.height)
        for (i in 0 until mask.width * mask.height) {
            val backgroundLikelihood = 1 - byteBuffer.float
            if (backgroundLikelihood > 0.9) {
                colors[i] = Color.argb(200, 0, 255, 0)
            } else if (backgroundLikelihood > 0.2) {
                // Linear interpolation to make sure when backgroundLikelihood is 0.2,
                // the alpha is 0 and when backgroundLikelihood is 0.9, the alpha is 128.
                // +0.5 to round the float value to the nearest int.
                val alpha = (182.9 * backgroundLikelihood - 36.6 + 0.5).toInt()
                colors[i] = Color.argb(alpha, 0, 255, 0)
            }
        }
        return colors
    }
}