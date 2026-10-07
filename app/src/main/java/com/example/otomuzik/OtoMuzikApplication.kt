package com.example.otomuzik

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OtoMuzikApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val report = buildString {
                    appendLine("================ RidoPlay CRASH REPORT ================")
                    appendLine("Time: $timestamp")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                    appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Thread: ${thread.name} (id=${thread.id})")
                    appendLine("Exception: ${throwable::class.java.name}: ${throwable.message}")
                    appendLine("Stack Trace:")
                    val sw = java.io.StringWriter()
                    val pw = PrintWriter(sw)
                    throwable.printStackTrace(pw)
                    appendLine(sw.toString())
                    appendLine("======================================================")
                }

                Log.e("OtoMuzikCrash", report)

                // 1. SharedPreferences'a kaydet (Uygulama içi hata gösterimi için)
                getSharedPreferences("oto_muzik_crash", MODE_PRIVATE).edit()
                    .putString("last_crash_report", report)
                    .putLong("last_crash_time", System.currentTimeMillis())
                    .commit()

                // 2. Uygulama özel dizinine yaz (izin gerektirmez)
                getExternalFilesDir(null)?.let { dir ->
                    val file = File(dir, "crash_log.txt")
                    FileWriter(file, true).use { it.write(report + "\n\n") }
                }

                // 3. /sdcard dizinine yaz (Harici depolama erişilebilirse)
                try {
                    val sdCardFile = File("/sdcard/RidoPlay_crash.txt")
                    FileWriter(sdCardFile, true).use { it.write(report + "\n\n") }
                } catch (_: Throwable) {}

            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
