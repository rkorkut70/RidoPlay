package com.example.otomuzik.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.otomuzik.MainActivity
import com.example.otomuzik.data.MusicRepository

/**
 * Tablet veya araç multimedya sistemi açıldığında RidoPlay'i otomatik başlatan alıcı.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        Log.d(TAG, "BootReceiver tetiklendi: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            try {
                val repository = MusicRepository(context)
                if (repository.getStartOnBoot()) {
                    Log.d(TAG, "Cihaz açılışında başlatma aktif, MainActivity açılıyor...")
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(launchIntent)
                } else {
                    Log.d(TAG, "Cihaz açılışında başlatma ayarı kapalı.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "BootReceiver MainActivity başlatırken hata oluştu: ${e.message}", e)
            }
        }
    }
}
