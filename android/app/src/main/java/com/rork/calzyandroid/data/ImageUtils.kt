package com.rork.calzyandroid.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Image helpers. Meal and progress photos persist as downscaled JPEG data URLs
 * for parity with the web and iOS builds.
 */
object ImageUtils {

    private const val PREFIX = "data:image/jpeg;base64,"

    fun bitmapToDataUrl(source: Bitmap, maxDimension: Int = 900, quality: Int = 72): String {
        val scale = min(
            1f,
            maxDimension.toFloat() / maxOf(source.width, source.height).toFloat(),
        )
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            source
        }
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return PREFIX + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun dataUrlToBitmap(dataUrl: String): Bitmap? {
        val comma = dataUrl.indexOf(',')
        if (comma == -1) return null
        return try {
            val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (error: Exception) {
            null
        }
    }

    /** Reads a gallery/photo-picker Uri into a downscaled data URL. */
    fun uriToDataUrl(context: Context, uri: Uri, maxDimension: Int = 900): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (
                maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension
            ) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null
            bitmapToDataUrl(bitmap, maxDimension)
        } catch (error: Exception) {
            null
        }
    }

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
