package com.moondicine.app.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val githubApiUrl = "https://api.github.com/repos/IgorLimaDev/moondicine/releases/latest"

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    sealed class DownloadState {
        data object Idle : DownloadState()
        data class Downloading(val progress: Int = 0) : DownloadState()
        data class Downloaded(val file: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
        data object Installing : DownloadState()
    }

    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion()
            val request = Request.Builder()
                .url(githubApiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Moondicine-App")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))
            val release = gson.fromJson(responseBody, GitHubRelease::class.java)
            val latestVersion = release.tagName
            val hasUpdate = VersionUtil.isNewerVersion(currentVersion, latestVersion)
            val apkAsset = release.assets.find { it.name.endsWith(".apk") && it.name.contains("release") }
                ?: release.assets.find { it.name.endsWith(".apk") }
            Result.success(UpdateInfo(
                hasUpdate = hasUpdate,
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                releaseNotes = release.body,
                downloadUrl = apkAsset?.downloadUrl,
                releaseUrl = release.htmlUrl
            ))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to check for updates: ${e.message}"))
        }
    }

    suspend fun downloadApk(url: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Downloading(0)
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Moondicine-App")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _downloadState.value = DownloadState.Error("Falha no download: HTTP ${response.code}")
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
            val body = response.body ?: run {
                _downloadState.value = DownloadState.Error("Resposta vazia")
                return@withContext Result.failure(Exception("Empty body"))
            }
            val contentLength = body.contentLength()
            val apkDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(apkDir, "moondicine-update.apk")
            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            _downloadState.value = DownloadState.Downloading(((totalRead * 100) / contentLength).toInt())
                        }
                    }
                }
            }
            _downloadState.value = DownloadState.Downloaded(apkFile)
            Result.success(apkFile)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error("Falha no download: ${e.message}")
            Result.failure(e)
        }
    }

    fun installApk(file: File) {
        _downloadState.value = DownloadState.Installing
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            "v1.0.0"
        }
    }
}