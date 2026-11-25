package com.example.data.update

data class GitHubReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val publishedAt: String,
    val htmlUrl: String
)

sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data class UpToDate(
        val currentVersion: String,
        val checkedAtMillis: Long
    ) : AppUpdateState

    data class UpdateAvailable(
        val release: GitHubReleaseInfo,
        val currentVersion: String
    ) : AppUpdateState

    data class Downloading(
        val release: GitHubReleaseInfo,
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : AppUpdateState

    data class ReadyToInstall(
        val release: GitHubReleaseInfo,
        val cacheFilePath: String,
        val savedInDownloadsName: String?,
        val savedInDownloadsSuccess: Boolean
    ) : AppUpdateState

    data class Error(
        val message: String,
        val release: GitHubReleaseInfo? = null
    ) : AppUpdateState
}

object VersionComparator {
    /**
     * Parses semantic/build version strings like "v2.0.1.7", "2.0.1", "v2.1.0-beta"
     * into numeric segments (e.g. [2, 0, 1, 7]).
     */
    fun parseNumericParts(version: String): List<Int> {
        val cleaned = version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        if (cleaned.isBlank()) return emptyList()
        return cleaned.split(".")
            .mapNotNull { part -> part.filter { it.isDigit() }.toIntOrNull() }
    }

    /**
     * Returns true strictly when [remoteTag] represents a newer version than [installedTag].
     * Examples:
     * - "v2.0.1.8" vs "v2.0.1.7" -> true
     * - "v2.0.1.7" vs "v2.0.1"   -> true
     * - "v2.1.0"   vs "v2.0.1.9" -> true
     * - "v2.0.1.7" vs "v2.0.1.7" -> false
     * - "v2.0.1.6" vs "v2.0.1.7" -> false
     */
    fun isRemoteNewer(remoteTag: String, installedTag: String): Boolean {
        val remote = parseNumericParts(remoteTag)
        val local = parseNumericParts(installedTag)
        if (remote.isEmpty() || local.isEmpty()) {
            val r = remoteTag.trim().removePrefix("v").removePrefix("V")
            val l = installedTag.trim().removePrefix("v").removePrefix("V")
            return r.isNotBlank() && r != l
        }
        val maxLen = maxOf(remote.size, local.size)
        for (i in 0 until maxLen) {
            val r = remote.getOrElse(i) { 0 }
            val l = local.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    fun displayTag(rawVersion: String): String {
        val trimmed = rawVersion.trim()
        return if (trimmed.startsWith("v", ignoreCase = true)) {
            "v${trimmed.substring(1)}"
        } else {
            "v$trimmed"
        }
    }
}
