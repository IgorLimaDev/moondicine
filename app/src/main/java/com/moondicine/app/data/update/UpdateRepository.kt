package com.moondicine.app.data.update

import android.content.Context
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
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
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val githubApiUrl = "https://api.github.com/repos/IgorLimaDev/moondicine/releases/latest"
    
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
                return@withContext Result.failure(Exception("Failed to check for updates: HTTP ${response.code}"))
            }
            
            val responseBody = response.body?.string()
            if (responseBody == null) {
                return@withContext Result.failure(Exception("Empty response"))
            }
            val release = gson.fromJson(responseBody, GitHubRelease::class.java)
            
            val latestVersion = release.tagName
            val hasUpdate = VersionUtil.isNewerVersion(currentVersion, latestVersion)
            
            val apkAsset = release.assets.find { it.name.endsWith(".apk") && it.name.contains("release") }
                ?: release.assets.find { it.name.endsWith(".apk") }
            
            val downloadUrl = apkAsset?.downloadUrl
            val releaseNotes = release.body
            
            Result.success(UpdateInfo(
                hasUpdate = hasUpdate,
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                releaseUrl = release.htmlUrl
            ))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to check for updates: ${e.message}"))
        }
    }
    
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            "v1.0.0"
        }
    }
    
    companion object {
        const val GITHUB_OWNER = "OWNER"
        const val GITHUB_REPO = "REPO"
    }
}