package com.kaizen.khushu.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.kaizen.khushu.BuildConfig
import com.kaizen.khushu.R
import com.kaizen.khushu.i18n.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String,
    @SerialName("body") val body: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

data class UpdateInfo(
    val latestVersionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
)

sealed interface UpdateCheckResult {
    data object Checking : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult
    data class Error(val message: String, val isRetryable: Boolean = false) : UpdateCheckResult
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    data class Ready(val apkFile: File) : DownloadState
    data class Failed(val message: String) : DownloadState
}

object AppUpdateRepository {
    private const val GITHUB_API_URL = "https://api.github.com/repos/farhun/iman/releases/latest"
    private const val GITHUB_ACCEPT_HEADER = "application/vnd.github.v3+json"

    // Must match the authority declared in AndroidManifest.xml for the FileProvider.
    // Using applicationId so it resolves correctly across application variants.
    private val PROVIDER_AUTHORITY: String by lazy { "${BuildConfig.APPLICATION_ID}.update_provider" }
    private const val USER_AGENT = "Iman/${BuildConfig.VERSION_NAME} (Android; +https://github.com/farhun/iman)"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Whether this build supports in-app updates. */
    val isUpdateableBuild: Boolean get() = true

    /**
     * Checks for an update on GitHub. Returns [UpdateCheckResult.Error] immediately
     * if called on a non-updateable build.
     */
    suspend fun checkForUpdate(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val localizedContext = LocaleHelper.wrap(context)
        if (!isUpdateableBuild) {
            return@withContext UpdateCheckResult.Error(
                localizedContext.getString(R.string.settings_update_unavailable_build),
                isRetryable = false
            )
        }
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", GITHUB_ACCEPT_HEADER)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val msg = when (response.code) {
                    403 -> localizedContext.getString(R.string.settings_update_rate_limited)
                    404 -> localizedContext.getString(R.string.settings_update_endpoint_not_found)
                    in 500..599 -> localizedContext.getString(R.string.settings_update_server_error, response.code)
                    else -> localizedContext.getString(R.string.settings_update_api_error, response.code)
                }
                return@withContext UpdateCheckResult.Error(msg, isRetryable = true)
            }

            val body = response.body?.string() ?: return@withContext UpdateCheckResult.Error(
                localizedContext.getString(R.string.settings_update_empty_response),
                isRetryable = true
            )
            val release = json.decodeFromString<GitHubRelease>(body)

            // Prerelease policy: skip prerelease releases on stable channel.
            if (release.prerelease) {
                return@withContext UpdateCheckResult.Error(
                    localizedContext.getString(R.string.settings_update_prerelease),
                    isRetryable = false
                )
            }

            val tagVersion = release.tagName.removePrefix("v")
            val currentVersionName = BuildConfig.VERSION_NAME
                .substringBefore("+") // strip any +gitHash suffix for semantic compare

            if (compareVersions(tagVersion, currentVersionName) < 0) {
                return@withContext UpdateCheckResult.Error(
                    localizedContext.getString(
                        R.string.settings_update_installed_newer,
                        currentVersionName,
                        tagVersion
                    ),
                    isRetryable = false
                )
            }
            if (compareVersions(tagVersion, currentVersionName) == 0) {
                return@withContext UpdateCheckResult.UpToDate
            }

            // Prefer the universal APK, then fall back to an ABI-specific APK.
            val apkAsset = release.assets.find {
                it.name.endsWith(".apk", ignoreCase = true) &&
                        it.name.contains("universal", ignoreCase = true)
            } ?: release.assets.find {
                it.name.endsWith(".apk", ignoreCase = true)
            }

            if (apkAsset == null) {
                val assetNames = release.assets.joinToString(", ") { it.name }
                return@withContext UpdateCheckResult.Error(
                    localizedContext.getString(R.string.settings_update_no_apk, assetNames),
                    isRetryable = false
                )
            }

            UpdateCheckResult.UpdateAvailable(
                UpdateInfo(
                    latestVersionName = tagVersion,
                    releaseNotes = release.body ?: "",
                    downloadUrl = apkAsset.browserDownloadUrl,
                )
            )
        } catch (e: java.net.SocketTimeoutException) {
            UpdateCheckResult.Error(
                localizedContext.getString(R.string.settings_update_timeout),
                isRetryable = true
            )
        } catch (e: java.net.UnknownHostException) {
            UpdateCheckResult.Error(
                localizedContext.getString(R.string.settings_update_no_internet),
                isRetryable = true
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                e.message ?: localizedContext.getString(R.string.settings_update_unknown_error),
                isRetryable = true
            )
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
    ): DownloadState = withContext(Dispatchers.IO) {
        val localizedContext = LocaleHelper.wrap(context)
        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext DownloadState.Failed(
                                    localizedContext.getString(R.string.settings_update_download_failed_code, response.code)
                                )
            }

            val body = response.body ?: return@withContext DownloadState.Failed(
                localizedContext.getString(R.string.settings_update_empty_body)
            )
            val contentLength = body.contentLength()
            val fileName = "iman-update-${BuildConfig.VERSION_NAME}.apk"
            val file = File(context.cacheDir, fileName)

            FileOutputStream(file).use { output ->
                val inputStream = body.byteStream()
                val buffer = ByteArray(8192)
                var bytesRead = 0L
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesRead += read
                    if (contentLength > 0) {
                        onProgress(bytesRead.toFloat() / contentLength)
                    }
                }
            }

            DownloadState.Ready(file)
        } catch (e: Exception) {
            DownloadState.Failed(e.message ?: localizedContext.getString(R.string.settings_update_download_failed))
        }
    }

    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Returns > 0 if v1 is newer, < 0 if older, 0 if equal. */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}
