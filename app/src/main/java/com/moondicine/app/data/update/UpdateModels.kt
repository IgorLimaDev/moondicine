package com.moondicine.app.data.update

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubAsset>
)

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("size") val size: Long,
    @SerializedName("content_type") val contentType: String
)

data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String?,
    val downloadUrl: String?,
    val releaseUrl: String
)

object VersionUtil {
    fun compareVersions(current: String, latest: String): Int {
        val currentParts = current.replace("v", "").split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.replace("v", "").split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxSize = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxSize) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (currentPart != latestPart) {
                return currentPart.compareTo(latestPart)
            }
        }
        return 0
    }
    
    fun isNewerVersion(current: String, latest: String): Boolean {
        return compareVersions(current, latest) < 0
    }
}