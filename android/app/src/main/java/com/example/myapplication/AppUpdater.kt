package com.example.myapplication



import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdater(private val context: Context) {

    // REPLACE THESE WITH YOUR DETAILS
    // This calculates the URL automatically
    private val UPDATE_URL = "https://api.github.com/repos/chelipika/eschoolloggerappupdate/releases/latest"

    suspend fun checkForUpdate(currentVersionName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(UPDATE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "MyApp") // GitHub requires this

                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)

                // Get the tag (e.g. "v1.0.0")
                val latestTag = json.getString("tag_name")

                // Clean up the "v" (e.g. "v1.0.0" -> "1.0.0")
                val cleanLatest = latestTag.replace("[^0-9.]".toRegex(), "")
                val cleanCurrent = currentVersionName.replace("[^0-9.]".toRegex(), "")

                // Compare versions safely
                if (isNewerVersion(cleanCurrent, cleanLatest)) {
                    val assets = json.getJSONArray("assets")
                    if (assets.length() > 0) {
                        return@withContext assets.getJSONObject(0).getString("browser_download_url")
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace() // CHECK YOUR LOGCAT IF THIS HAPPENS
                null
            }
        }
    }

    // Helper function to compare "1.0" vs "1.0.0" or "0.1" vs "1.0"
    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".")
        val latestParts = latest.split(".")
        val length = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until length) {
            val c = if (i < currentParts.size) currentParts[i].toIntOrNull() ?: 0 else 0
            val l = if (i < latestParts.size) latestParts[i].toIntOrNull() ?: 0 else 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun downloadApk(apkUrl: String, onProgress: (Float) -> Unit): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(apkUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength
                val file = File(context.getExternalFilesDir(null), "update.apk")
                if (file.exists()) {
                    file.delete()
                }
                connection.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(4 * 1024)
                        var bytesCopied = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } >= 0) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            // Calculate progress
                            if (fileLength > 0) {
                                onProgress(bytesCopied.toFloat() / fileLength.toFloat())
                            }
                        }
                    }
                }
                if (file.length() < 1000 * 1024) {
                    Log.e("AppUpdater", "File too small! Download failed.")
                    file.delete()
                    return@withContext null
                }
                file

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // 3. Install the APK
    fun installApk(file: File) {
        if (!file.exists()) {
            Log.e("AppUpdater", "File not found!")
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")

            // THESE FLAGS ARE CRITICAL:
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(intent)
    }
}