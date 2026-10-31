package com.f8scape.photoframe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.f8scape.util.F8Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Handles Storage Access Framework (SAF) folder enumeration, Photo Picker URI resolution,
 * and persistable read permissions.
 */
class PhotoFrameSourceRepository(private val context: Context) {

    companion object {
        private const val TAG = "PhotoSourceRepo"

        private val SUPPORTED_IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "heic", "heif"
        )
    }

    /**
     * Resolves the list of available image URIs for the active source type.
     * Automatically filters out unreadable or revoked URIs so dead files are omitted.
     */
    suspend fun resolveActiveUris(
        sourceType: String,
        selectedUrisSerialized: String,
        folderUriString: String
    ): List<Uri> = withContext(Dispatchers.IO) {
        if (sourceType == "FOLDER") {
            if (folderUriString.isBlank()) {
                emptyList()
            } else {
                try {
                    val folderUri = Uri.parse(folderUriString)
                    enumerateFolderImages(folderUri)
                } catch (e: Exception) {
                    F8Log.e(TAG, "Failed to enumerate folder images from $folderUriString", e)
                    emptyList()
                }
            }
        } else {
            // Selected Photos - filter out unreadable / revoked URIs
            selectedUrisSerialized
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { uriStr ->
                    try {
                        Uri.parse(uriStr)
                    } catch (_: Exception) {
                        null
                    }
                }
                .filter { uri ->
                    PhotoDecoder.canOpenUri(context, uri)
                }
        }
    }

    /**
     * Enumerates image documents within an ACTION_OPEN_DOCUMENT_TREE directory.
     */
    private fun enumerateFolderImages(treeUri: Uri): List<Uri> {
        val results = mutableListOf<Uri>()
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

                if (idCol != -1) {
                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idCol) ?: continue
                        val mime = if (mimeCol != -1) cursor.getString(mimeCol) else null
                        val name = if (nameCol != -1) cursor.getString(nameCol) else null

                        if (isSupportedImage(mime, name)) {
                            val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            results.add(documentUri)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            F8Log.e(TAG, "Error querying tree documents for $treeUri", e)
        }
        return results
    }

    /**
     * Checks if a document represents a supported image format.
     */
    private fun isSupportedImage(mimeType: String?, displayName: String?): Boolean {
        if (mimeType?.startsWith("image/", ignoreCase = true) == true) {
            return true
        }
        if (displayName != null) {
            val extension = displayName.substringAfterLast('.', "").lowercase(Locale.getDefault())
            if (extension in SUPPORTED_IMAGE_EXTENSIONS) {
                return true
            }
        }
        return false
    }

    /**
     * Safely takes persistable URI permission if offered by the provider.
     */
    fun takePersistablePermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            F8Log.w(TAG, "Persistable permission could not be taken for URI: $uri (handled gracefully)")
        } catch (e: Exception) {
            F8Log.w(TAG, "Unexpected error taking permission for URI: $uri: ${e.message}")
        }
    }

    /**
     * Safely releases a persistable URI permission when a photo or folder is removed.
     */
    fun releasePersistablePermission(uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Ignore if grant was already cleared or per-session
        }
    }

    /**
     * Resolves human-readable folder name for the UI.
     */
    fun getFolderDisplayName(folderUriString: String): String {
        if (folderUriString.isBlank()) return "None"
        return try {
            val uri = Uri.parse(folderUriString)
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val leaf = docId.substringAfterLast(':', docId)
            leaf.substringAfterLast('/', leaf).ifBlank { "Selected Folder" }
        } catch (_: Exception) {
            "Selected Folder"
        }
    }

    /**
     * Copies a selected photo directly into app internal storage.
     * This guarantees 100% reliable, permanent, offline file access across reboots,
     * without being affected by Activity lifecycle or ContentProvider permission limits.
     */
    suspend fun importPhotoToInternal(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            // If already an imported local file, verify and return
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null && File(path).exists()) {
                    return@withContext uri
                }
            }

            val storageDir = File(context.filesDir, "photo_frame_photos").apply {
                if (!exists()) mkdirs()
            }

            // Determine appropriate file extension from mime type
            val mimeType = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
            val ext = when {
                mimeType?.contains("png", ignoreCase = true) == true -> "png"
                mimeType?.contains("webp", ignoreCase = true) == true -> "webp"
                mimeType?.contains("heic", ignoreCase = true) == true -> "heic"
                else -> "jpg"
            }

            val targetFile = File(storageDir, "photo_${System.currentTimeMillis()}_${(1000..9999).random()}.$ext")
            val inputStream = PhotoDecoder.openInputStream(context, uri) ?: run {
                F8Log.w(TAG, "Cannot open stream to import photo from URI: $uri")
                return@withContext null
            }

            inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                F8Log.i(TAG, "Imported photo successfully: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                Uri.fromFile(targetFile)
            } else {
                targetFile.delete()
                null
            }
        } catch (e: Exception) {
            F8Log.e(TAG, "Failed to import photo from $uri", e)
            null
        }
    }

    /**
     * Deletes an imported photo file from internal storage.
     */
    fun deleteInternalPhoto(uri: Uri) {
        try {
            val filename = uri.lastPathSegment
            if (filename != null && (uri.scheme == "file" || uri.toString().contains("photo_frame_photos"))) {
                val internalDir = File(context.filesDir, "photo_frame_photos")
                File(internalDir, filename).delete()
            } else if (uri.scheme == "file") {
                val path = uri.path
                if (path != null && path.contains("photo_frame_photos")) {
                    File(path).delete()
                }
            }
        } catch (e: Exception) {
            F8Log.w(TAG, "Failed to delete internal photo $uri: ${e.message}")
        }
    }

    /**
     * Clears all imported photos from internal storage.
     */
    fun clearAllInternalPhotos() {
        try {
            val storageDir = File(context.filesDir, "photo_frame_photos")
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            F8Log.w(TAG, "Failed to clear internal photos: ${e.message}")
        }
    }
}
