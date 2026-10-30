package com.f8scape.updater

data class AppReleaseInfo(
    val versionName: String,
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val publishedAt: String,
)

sealed interface UpdateCheckResult {
    data object Checking : UpdateCheckResult
    data class Available(val releaseInfo: AppReleaseInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String, val throwable: Throwable? = null) : UpdateCheckResult
}

data class DownloadProgress(
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val percent: Float = 0f,
    val isCompleted: Boolean = false,
    val error: String? = null,
)
