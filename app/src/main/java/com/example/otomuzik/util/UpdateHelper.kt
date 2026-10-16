package com.example.otomuzik.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val changelog: String,
    val playStoreUrl: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean
)

object UpdateHelper {

    private const val PRIMARY_URL = "https://raw.githubusercontent.com/rkorkut70/RidoPlay/main/version.json"
    private const val FALLBACK_URL = "https://rkorkut70.github.io/RidoPlay/version.json"
    private const val TIMEOUT_MS = 6000

    suspend fun checkForUpdates(
        currentVersionCode: Int,
        currentVersionName: String
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        val jsonString = fetchJson(PRIMARY_URL) ?: fetchJson(FALLBACK_URL)
        if (jsonString.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Güncelleme sunucusuna bağlanılamadı. Lütfen internet bağlantınızı kontrol edin."))
        }

        try {
            val json = JSONObject(jsonString)
            val latestVersionCode = json.optInt("versionCode", currentVersionCode)
            val latestVersionName = json.optString("versionName", currentVersionName)
            val changelog = json.optString("changelog", "Yeni sürüm geliştirmeleri ve performans iyileştirmeleri.")
            val playStoreUrl = json.optString("playStoreUrl", "https://play.google.com/store/apps/details?id=com.ridoplay.music")
            val downloadUrl = json.optString("downloadUrl", "https://github.com/rkorkut70/RidoPlay/releases/latest")

            val isUpdateAvailable = latestVersionCode > currentVersionCode

            val info = UpdateInfo(
                latestVersionCode = latestVersionCode,
                latestVersionName = latestVersionName,
                changelog = changelog,
                playStoreUrl = playStoreUrl,
                downloadUrl = downloadUrl,
                isUpdateAvailable = isUpdateAvailable
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchJson(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                setRequestProperty("User-Agent", "RidoPlay-Android")
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                setPackage("com.android.vending")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {}
        }
    }

    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
