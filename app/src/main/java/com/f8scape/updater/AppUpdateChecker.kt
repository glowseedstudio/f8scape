package com.f8scape.updater

import android.content.Context
import com.f8scape.util.F8Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class AppUpdateChecker(
    private val repoOwner: String = DEFAULT_REPO_OWNER,
    private val repoName: String = DEFAULT_REPO_NAME,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        const val DEFAULT_REPO_OWNER = "glowseedstudio"
        const val DEFAULT_REPO_NAME = "f8scape"
        private const val TAG = "AppUpdateChecker"

        fun getInstalledAppVersion(context: Context): String {
            return runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.PackageInfoFlags.of(0)
                    ).versionName
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "1.0.0"
        }

        /**
         * Compares two SemVer strings (e.g., "1.0.1" vs "1.0.0").
         * Returns true if [remoteVersion] is strictly newer than [localVersion].
         */
        fun isNewerVersion(remoteVersion: String, localVersion: String): Boolean {
            val remoteParts = parseVersionParts(remoteVersion)
            val localParts = parseVersionParts(localVersion)
            val maxLen = maxOf(remoteParts.size, localParts.size)

            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }

        private fun parseVersionParts(version: String): List<Int> {
            val clean = version.trim().removePrefix("v").removePrefix("V")
            val base = clean.substringBefore('-').substringBefore('+')
            return base.split('.').map { it.toIntOrNull() ?: 0 }
        }
    }

    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(ioDispatcher) {
        val endpoint = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        var connection: HttpsURLConnection? = null
        try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "F8scape-Updater/$currentVersion")
            }

            val statusCode = connection.responseCode
            if (statusCode == 403) {
                val rateLimitRemaining = connection.getHeaderField("X-RateLimit-Remaining")
                F8Log.w(TAG, "GitHub API 403 Forbidden. RateLimit-Remaining: $rateLimitRemaining")
                return@withContext UpdateCheckResult.Error("GitHub rate limit reached. Please try again later.")
            }

            if (statusCode !in 200..299) {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() }
                F8Log.w(TAG, "GitHub API returned status $statusCode: $errorMsg")
                return@withContext UpdateCheckResult.Error("GitHub error HTTP $statusCode")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseJson = JSONObject(responseBody)
            val rawTag = releaseJson.optString("tag_name", "").trim()
            val remoteVersion = rawTag.removePrefix("v").removePrefix("V")

            if (remoteVersion.isBlank()) {
                return@withContext UpdateCheckResult.Error("Invalid release data from GitHub")
            }

            if (!isNewerVersion(remoteVersion, currentVersion)) {
                return@withContext UpdateCheckResult.UpToDate(currentVersion)
            }

            val title = releaseJson.optString("name", "Release $rawTag")
            val notes = releaseJson.optString("body", "")
            val publishedAt = releaseJson.optString("published_at", "")

            val assetsArray = releaseJson.optJSONArray("assets")
            if (assetsArray == null || assetsArray.length() == 0) {
                return@withContext UpdateCheckResult.Error("Release has no attached APK files")
            }

            var matchingAssetUrl: String? = null
            var matchingAssetSize: Long = 0L

            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    matchingAssetUrl = asset.optString("browser_download_url")
                    matchingAssetSize = asset.optLong("size", 0L)
                    break
                }
            }

            if (matchingAssetUrl.isNullOrBlank()) {
                return@withContext UpdateCheckResult.Error("No APK download found in release $rawTag")
            }

            val info = AppReleaseInfo(
                versionName = remoteVersion,
                tagName = rawTag,
                releaseTitle = title,
                releaseNotes = notes,
                apkDownloadUrl = matchingAssetUrl,
                apkSizeBytes = matchingAssetSize,
                publishedAt = publishedAt,
            )

            F8Log.i(TAG, "Found new update: ${info.versionName} (${info.apkSizeBytes} bytes)")
            UpdateCheckResult.Available(info)
        } catch (e: Exception) {
            F8Log.e(TAG, "Failed to check for updates: ${e.message}", e)
            UpdateCheckResult.Error(e.message ?: "Network error checking for updates", e)
        } finally {
            connection?.disconnect()
        }
    }
}
