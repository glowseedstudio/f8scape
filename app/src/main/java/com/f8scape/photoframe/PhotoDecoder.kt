package com.f8scape.photoframe

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import com.f8scape.util.F8Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Memory-efficient, production-grade image decoder tailored for ambient display sessions
 * and the Galaxy Z Fold 8 landscape cover screen (~2400 x 1100 target).
 *
 * Automatically resolves EXIF orientation, applies downsampling (inSampleSize) to prevent
 * memory spikes, and gracefully skips corrupt or deleted files.
 */
object PhotoDecoder {

    private const val TAG = "PhotoDecoder"

    // Default max dimensions targeting the Galaxy Z Fold 8 cover screen in landscape
    private const val DEFAULT_TARGET_WIDTH = 2400
    private const val DEFAULT_TARGET_HEIGHT = 1200
    private const val THUMBNAIL_TARGET_SIZE = 280

    /**
     * Decodes a full-size display bitmap with subsampling and correct orientation.
     */
    suspend fun decodePhoto(
        context: Context,
        uri: Uri,
        targetWidth: Int = DEFAULT_TARGET_WIDTH,
        targetHeight: Int = DEFAULT_TARGET_HEIGHT
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 1. First pass: Query raw image dimensions without allocating memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            openInputStream(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: run {
                F8Log.w(TAG, "Cannot open input stream for URI: $uri")
                return@withContext null
            }

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) {
                F8Log.w(TAG, "Invalid dimensions for photo: ${rawWidth}x${rawHeight}")
                return@withContext null
            }

            // 2. Compute power-of-2 inSampleSize
            options.inSampleSize = calculateInSampleSize(rawWidth, rawHeight, targetWidth, targetHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            // 3. Second pass: Decode downsampled bitmap
            val decodedBitmap = openInputStream(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext null

            // 4. Resolve EXIF orientation and rotate if needed
            val rotationDegrees = getExifRotation(context, uri)
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(
                    decodedBitmap,
                    0,
                    0,
                    decodedBitmap.width,
                    decodedBitmap.height,
                    matrix,
                    true
                )
                if (rotatedBitmap != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                rotatedBitmap
            } else {
                decodedBitmap
            }
        } catch (e: SecurityException) {
            F8Log.e(TAG, "Permission denied for photo URI: $uri", e)
            null
        } catch (e: Exception) {
            F8Log.e(TAG, "Failed to decode photo from URI: $uri", e)
            null
        }
    }

    /**
     * Decodes a lightweight thumbnail for the collection manager grid.
     */
    suspend fun decodeThumbnail(
        context: Context,
        uri: Uri,
        targetSize: Int = THUMBNAIL_TARGET_SIZE
    ): Bitmap? = decodePhoto(context, uri, targetSize, targetSize)

    /**
     * Calculates the largest power-of-2 inSampleSize value that is a power of 2 and keeps both
     * height and width larger than the requested target height and width.
     */
    fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Reads the EXIF orientation tag from an image input stream and returns rotation degrees.
     */
    private fun getExifRotation(context: Context, uri: Uri): Int {
        return try {
            openInputStream(context, uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Robust stream opener that handles standard ContentResolver streams, ParcelFileDescriptors,
     * Photopicker URI-to-MediaStore translation, and direct filesystem paths.
     */
    fun openInputStream(context: Context, uri: Uri): InputStream? {
        val resolver = context.contentResolver

        // 1. Try direct ContentResolver stream
        try {
            val stream = resolver.openInputStream(uri)
            if (stream != null) return stream
        } catch (_: Exception) {}

        // 2. Try ParcelFileDescriptor
        try {
            val pfd = resolver.openFileDescriptor(uri, "r")
            if (pfd != null) return FileInputStream(pfd.fileDescriptor)
        } catch (_: Exception) {}

        // 3. If Photo Picker URI (content://media/picker/.../media/<id>)
        val uriStr = uri.toString()
        if (uriStr.contains("photopicker") || uri.authority?.contains("photopicker") == true) {
            val mediaId = uri.lastPathSegment?.toLongOrNull()
            if (mediaId != null) {
                val mediaStoreUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
                // Try MediaStore content URI
                try {
                    val stream = resolver.openInputStream(mediaStoreUri)
                    if (stream != null) return stream
                } catch (_: Exception) {}

                // Try resolving filesystem path from MediaStore
                try {
                    val projection = arrayOf(MediaStore.Images.Media.DATA)
                    resolver.query(mediaStoreUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                            if (dataCol != -1) {
                                val filePath = cursor.getString(dataCol)
                                if (!filePath.isNullOrBlank()) {
                                    val file = File(filePath)
                                    if (file.exists() && file.canRead()) {
                                        return FileInputStream(file)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 4. If direct file path or file URI
        try {
            val path = if (uri.scheme == "file") uri.path else if (uri.path?.startsWith("/") == true) uri.path else null
            if (path != null) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    return FileInputStream(file)
                }
            }
        } catch (_: Exception) {}

        return null
    }
}
