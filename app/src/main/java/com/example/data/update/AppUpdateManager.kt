package com.example.data.update

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

data class DownloadedApkResult(
    val cacheFilePath: String,
    val savedInDownloadsName: String?,
    val savedInDownloadsSuccess: Boolean
)

object AppUpdateManager {
    private const val GITHUB_API_BASE = "https://api.github.com/repos"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val UPDATES_CACHE_DIR = "updates"

    private val activeDownloadCall = AtomicReference<Call?>(null)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun getCurrentInstalledTag(): String = VersionComparator.displayTag(BuildConfig.GITHUB_RELEASE_TAG)

    fun getGitHubRepoSlug(): String = BuildConfig.GITHUB_REPO.ifBlank { "bestdem077/MonoDo" }

    fun getLatestReleaseApiUrl(): String = "$GITHUB_API_BASE/${getGitHubRepoSlug()}/releases/latest"

    fun getReleasesPageUrl(): String = "https://github.com/${getGitHubRepoSlug()}/releases"

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun fetchLatestRelease(): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(getLatestReleaseApiUrl())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MonoDo-Android/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        throw IOException("No published GitHub releases found yet.")
                    }
                    if (response.code == 403) {
                        throw IOException("GitHub rate limit reached. Try again in a few minutes.")
                    }
                    throw IOException("GitHub returned HTTP ${response.code}.")
                }

                val bodyString = response.body?.string()
                    ?: throw IOException("Empty response from GitHub.")
                parseReleaseJson(bodyString)
            }
        }
    }

    internal fun parseReleaseJson(jsonString: String): GitHubReleaseInfo {
        val root = JSONObject(jsonString)
        val tagName = root.optString("tag_name", "").trim()
        if (tagName.isBlank()) {
            throw IOException("Latest GitHub release has no version tag.")
        }

        val releaseName = root.optString("name", tagName).ifBlank { tagName }
        val rawBody = root.optString("body", "")
        val publishedAt = root.optString("published_at", "")
        val htmlUrl = root.optString("html_url", getReleasesPageUrl())
        val assets = root.optJSONArray("assets")

        var chosenName = ""
        var chosenUrl = ""
        var chosenSize = 0L

        if (assets != null) {
            // Prefer versioned MonoDo-vX.Y.Z.apk first, then MonoDo-latest.apk, then any .apk
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                val size = asset.optLong("size", 0L)
                if (!name.endsWith(".apk", ignoreCase = true) || url.isBlank()) continue

                val isVersionedPreferred = name.contains(tagName, ignoreCase = true)
                val isLatestFallback = name.equals("MonoDo-latest.apk", ignoreCase = true)
                if (isVersionedPreferred) {
                    chosenName = name
                    chosenUrl = url
                    chosenSize = size
                    break
                } else if (chosenUrl.isBlank() || (isLatestFallback && !chosenName.contains(tagName, ignoreCase = true))) {
                    chosenName = name
                    chosenUrl = url
                    chosenSize = size
                }
            }
        }

        if (chosenUrl.isBlank()) {
            throw IOException("No APK asset attached to release $tagName.")
        }

        val safeFileName = sanitizeApkFileName(
            if (chosenName.equals("MonoDo-latest.apk", ignoreCase = true)) {
                "MonoDo-$tagName.apk"
            } else {
                chosenName
            }
        )

        return GitHubReleaseInfo(
            tagName = tagName,
            releaseName = releaseName,
            releaseNotes = cleanReleaseNotes(rawBody),
            apkDownloadUrl = chosenUrl,
            apkFileName = safeFileName,
            apkSizeBytes = chosenSize,
            publishedAt = publishedAt,
            htmlUrl = htmlUrl
        )
    }

    suspend fun downloadApkWithProgress(
        context: Context,
        release: GitHubReleaseInfo,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<DownloadedApkResult> = withContext(Dispatchers.IO) {
        runCatching {
            val url = release.apkDownloadUrl
            require(
                url.startsWith("https://github.com/") ||
                    url.startsWith("https://objects.githubusercontent.com/") ||
                    url.startsWith("https://release-assets.githubusercontent.com/")
            ) { "Untrusted download host." }

            val updatesDir = File(context.cacheDir, UPDATES_CACHE_DIR).apply { mkdirs() }
            // Clean previous cached APKs so storage stays tiny
            updatesDir.listFiles()?.forEach { old ->
                if (old.name != release.apkFileName) {
                    runCatching { old.delete() }
                }
            }

            val finalCacheFile = File(updatesDir, sanitizeApkFileName(release.apkFileName))
            val tempFile = File(updatesDir, "${finalCacheFile.name}.part")
            if (tempFile.exists()) runCatching { tempFile.delete() }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MonoDo-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val call = httpClient.newCall(request)
            activeDownloadCall.set(call)

            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Download failed (HTTP ${response.code}).")
                    }
                    val body = response.body ?: throw IOException("Empty APK download stream.")
                    val totalBytes = body.contentLength().takeIf { it > 0L } ?: release.apkSizeBytes

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var downloaded = 0L
                            var lastEmitTime = 0L

                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read

                                val now = System.currentTimeMillis()
                                if (now - lastEmitTime >= 120L || (totalBytes > 0 && downloaded >= totalBytes)) {
                                    lastEmitTime = now
                                    val percent = if (totalBytes > 0L) {
                                        ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                    } else {
                                        0
                                    }
                                    onProgress(percent, downloaded, totalBytes)
                                }
                            }
                            output.flush()
                        }
                    }

                    if (!tempFile.exists() || tempFile.length() <= 0L) {
                        throw IOException("Downloaded APK file is empty.")
                    }

                    if (finalCacheFile.exists()) finalCacheFile.delete()
                    if (!tempFile.renameTo(finalCacheFile)) {
                        tempFile.copyTo(finalCacheFile, overwrite = true)
                        tempFile.delete()
                    }
                }
            } finally {
                activeDownloadCall.compareAndSet(call, null)
                if (tempFile.exists()) runCatching { tempFile.delete() }
            }

            // Also save a copy into the public Downloads folder so users can install manually
            // without ever needing to grant MonoDo "Install unknown apps" permission.
            val savedName = finalCacheFile.name
            val savedToDownloads = copyApkToPublicDownloads(context, finalCacheFile, savedName)

            DownloadedApkResult(
                cacheFilePath = finalCacheFile.absolutePath,
                savedInDownloadsName = savedName,
                savedInDownloadsSuccess = savedToDownloads
            )
        }
    }

    fun cancelActiveDownload() {
        activeDownloadCall.getAndSet(null)?.cancel()
    }

    private fun copyApkToPublicDownloads(
        context: Context,
        sourceFile: File,
        displayName: String
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

                // Remove any prior copy of the exact same release APK owned by MonoDo
                runCatching {
                    resolver.delete(
                        collection,
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                        arrayOf(displayName)
                    )
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, APK_MIME_TYPE)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(collection, values) ?: return false
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out, bufferSize = 16 * 1024)
                        }
                    } ?: return false

                    val publishValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    resolver.update(uri, publishValues, null, null)
                    true
                } catch (_: Exception) {
                    runCatching { resolver.delete(uri, null, null) }
                    false
                }
            } else {
                // On API 24-28: try public Downloads if writable, otherwise app-scoped Downloads
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (publicDownloads != null && (publicDownloads.exists() || publicDownloads.mkdirs()) && publicDownloads.canWrite()) {
                    val dest = File(publicDownloads, displayName)
                    sourceFile.copyTo(dest, overwrite = true)
                    true
                } else {
                    val appDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    if (appDownloads != null) {
                        val dest = File(appDownloads, displayName)
                        sourceFile.copyTo(dest, overwrite = true)
                        true
                    } else {
                        false
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun createInstallPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun launchApkInstaller(context: Context, cacheFilePath: String): Boolean {
        return try {
            val apkFile = File(cacheFilePath)
            if (!apkFile.exists() || apkFile.length() <= 0L) return false

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openDownloadsFolder(context: Context): Boolean {
        val intents = listOf(
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.externalstorage.documents/root/primary/Download"),
                    "vnd.android.document/directory"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        for (intent in intents) {
            val started = runCatching { context.startActivity(intent); true }.getOrDefault(false)
            if (started) return true
        }
        return false
    }

    fun openInBrowser(context: Context, url: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun sanitizeApkFileName(raw: String): String {
        val cleaned = raw.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
        val base = cleaned.ifBlank { "MonoDo-update.apk" }
        return if (base.endsWith(".apk", ignoreCase = true)) base else "$base.apk"
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "Unknown size"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes / 1024.0
            String.format(Locale.US, "%.0f KB", kb)
        }
    }

    fun cleanReleaseNotes(rawMarkdown: String): String {
        if (rawMarkdown.isBlank()) {
            return "Bug fixes, performance polish, and latest improvements."
        }
        val lines = rawMarkdown.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                    !line.startsWith("|") &&
                    !line.startsWith("---") &&
                    !line.startsWith("```") &&
                    !line.startsWith("SHA-256", ignoreCase = true) &&
                    !line.startsWith("![") &&
                    !line.contains("releases/download/") &&
                    !line.startsWith("*Maintained", ignoreCase = true)
            }
            .mapNotNull { line ->
                val cleaned = line
                    .removePrefix("### ")
                    .removePrefix("## ")
                    .removePrefix("# ")
                    .removePrefix("> ")
                    .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
                    .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
                    .replace("`", "")
                    .trim()

                when {
                    cleaned.isBlank() -> null
                    cleaned.equals("Build Information", ignoreCase = true) -> null
                    cleaned.equals("Download Assets", ignoreCase = true) -> null
                    cleaned.equals("Integrity Checksum", ignoreCase = true) -> null
                    cleaned.startsWith("Installation Guide", ignoreCase = true) -> null
                    cleaned.startsWith("MonoDo —", ignoreCase = true) -> null
                    cleaned.startsWith("Crafted", ignoreCase = true) -> null
                    cleaned.startsWith("100% offline", ignoreCase = true) -> null
                    else -> cleaned
                }
            }
            .take(6)

        return if (lines.isEmpty()) {
            "Latest refinements and stability upgrades for MonoDo."
        } else {
            lines.joinToString("\n") { line ->
                if (line.startsWith("* ") || line.startsWith("- ")) {
                    "• ${line.substring(2).trim()}"
                } else {
                    line
                }
            }
        }
    }
}
