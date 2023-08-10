/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.yakushevvv.mlkit.demo.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.Image.Plane
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Получение [Bitmap] на основе YUV-изображения, полученного от камеры для обработки в MLKit
 */
@OptIn(ExperimentalGetImage::class)
fun ImageProxy.toBitmap(): Bitmap {
    val nv21Buffer = yuv420ThreePlanesToNV21(image!!.planes, width, height)
    return getBitmap(nv21Buffer, this)
}


/**
 * Конвертирование буфера с NV21 информаций в [Bitmap].
 */
private fun getBitmap(data: ByteBuffer, imageProxy: ImageProxy): Bitmap {
    data.rewind()
    val imageInBuffer = ByteArray(data.limit())
    data[imageInBuffer, 0, imageInBuffer.size]
    return try {
        ByteArrayOutputStream().use {
            val image = YuvImage(
                imageInBuffer, ImageFormat.NV21, imageProxy.width, imageProxy.height, null
            )
            image.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 70, it)
            BitmapFactory.decodeByteArray(it.toByteArray(), 0, it.size())
        }
    } catch (e: Exception) {
        Log.e("VisionProcessorBase", "Error: " + e.message)
        Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ALPHA_8)
    }.run {
        rotateBitmap(this, imageProxy.imageInfo.rotationDegrees)
    }
}

/**
 * Поворот [Bitmap] с учетом информации об исходном угле поворота
 */
private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotatedBitmap =
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotatedBitmap != bitmap) {
        bitmap.recycle()
    }
    return rotatedBitmap
}

/**
 * Converts YUV_420_888 to NV21 bytebuffer.
 *
 *
 * The NV21 format consists of a single byte array containing the Y, U and V values. For an
 * image of size S, the first S positions of the array contain all the Y values. The remaining
 * positions contain interleaved V and U values. U and V are subsampled by a factor of 2 in both
 * dimensions, so there are S/4 U values and S/4 V values. In summary, the NV21 array will contain
 * S Y values followed by S/4 VU values: YYYYYYYYYYYYYY(...)YVUVUVUVU(...)VU
 *
 *
 * YUV_420_888 is a generic format that can describe any YUV image where U and V are subsampled
 * by a factor of 2 in both dimensions. [Image.getPlanes] returns an array with the Y, U and
 * V planes. The Y plane is guaranteed not to be interleaved, so we can just copy its values into
 * the first part of the NV21 array. The U and V planes may already have the representation in the
 * NV21 format. This happens if the planes share the same buffer, the V buffer is one position
 * before the U buffer and the planes have a pixelStride of 2. If this is case, we can just copy
 * them to the NV21 array.
 */
private fun yuv420ThreePlanesToNV21(
    yuv420888planes: Array<Plane>, width: Int, height: Int
): ByteBuffer {
    val imageSize = width * height
    val out = ByteArray(imageSize + 2 * (imageSize / 4))

    // Fallback to copying the UV values one by one, which is slower but also works.
    // Unpack Y.
    unpackPlane(yuv420888planes[0], width, height, out, 0, 1)
    // Unpack U.
    unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2)
    // Unpack V.
    unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2)

    return ByteBuffer.wrap(out)
}

/**
 * Unpack an image plane into a byte array.
 *
 * The input plane data will be copied in 'out', starting at 'offset' and every pixel will be
 * spaced by 'pixelStride'. Note that there is no row padding on the output.
 */
private fun unpackPlane(
    plane: Plane, width: Int, height: Int, out: ByteArray, offset: Int, pixelStride: Int
) {
    val buffer = plane.buffer
    buffer.rewind()

    // Compute the size of the current plane.
    // We assume that it has the aspect ratio as the original image.
    val numRow = (buffer.limit() + plane.rowStride - 1) / plane.rowStride
    if (numRow == 0) {
        return
    }
    val numCol = width / (height / numRow)

    // Extract the data in the output buffer.
    var outputPos = offset
    var rowStart = 0
    for (row in 0 until numRow) {
        var inputPos = rowStart
        for (col in 0 until numCol) {
            out[outputPos] = buffer[inputPos]
            outputPos += pixelStride
            inputPos += plane.pixelStride
        }
        rowStart += plane.rowStride
    }
}
