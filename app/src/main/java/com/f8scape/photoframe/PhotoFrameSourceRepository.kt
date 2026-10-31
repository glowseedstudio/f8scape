package com.f8scape.photoframe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.f8scape.util.F8Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            // Selected Photos
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
}
