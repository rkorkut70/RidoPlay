package com.example.otomuzik.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.otomuzik.MainActivity
import com.example.otomuzik.R
import com.example.otomuzik.data.MusicRepository
import com.example.otomuzik.service.MusicService

/**
 * Tablet veya araç multimedya sistemi açıldığında / kontak (ACC) açıldığında RidoPlay'i otomatik başlatan alıcı.
 * Desteklenen platformlar:
 * - Standart Android (Boot / QuickBoot / Power / UserPresent)
 * - K2501 / NWD / Allwinner (Reis Elektronik, Navigold, For-X)
 * - FYT / UIS7862 / Joying / Teyes / Mekede
 * - Microntek / PX3 / PX5 / PX6 / Dasaita
 * - Autochips / XYAuto / MTK8227L
 * - Topway / TS7 / TS10 / TS18
 * - USB Güç Bağlantısı (USB Attached)
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val CHANNEL_ID = "ridoplay_autostart_channel"
        private const val NOTIFICATION_ID = 9991

        // Çoklu sinyaller (ACC + USB + USER_PRESENT vb.) art arda geldiğinde gereksiz tekrarları önleme
        private var lastTriggerTimeMs: Long = 0L
        private const val DEBOUNCE_DELAY_MS = 3500L
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        Log.d(TAG, "Otomotiv / Sistem Başlatma Alıcısı Tetiklendi: $action")

        val now = System.currentTimeMillis()
        if (now - lastTriggerTimeMs < DEBOUNCE_DELAY_MS) {
            Log.d(TAG, "Sinyal çok hızlı tekrarlandı, debounce devrede: $action")
            return
        }

        try {
            val repository = MusicRepository(context)
            if (!repository.getStartOnBoot()) {
                Log.d(TAG, "Cihaz açılışında / kontak açılışında başlatma ayarı kapalı.")
                return
            }

            lastTriggerTimeMs = now
            Log.d(TAG, "Kontak / Açılış tespit edildi ($action), RidoPlay uyandırılıyor...")

            // 1. Ekranı veya CPU'yu kısa süre uyanık tut
            wakeUpCpuBriefly(context)

            // 2. MusicService'i Öncelikli Servis (Foreground) olarak uyandır
            try {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    if (repository.getAutoPlayOnStart()) {
                        setAction(MusicService.ACTION_PLAY)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "MusicService başarıyla uyandırıldı.")
            } catch (e: Exception) {
                Log.w(TAG, "MusicService başlatılırken uyarı: ${e.message}")
            }

            // 3. MainActivity'yi öne getirmeyi dene
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }

            try {
                context.startActivity(launchIntent)
                Log.d(TAG, "MainActivity doğrudan başlatıldı.")
            } catch (e: Exception) {
                Log.w(TAG, "Doğrudan Activity başlatılamadı (Android 10+ arka plan kısıtı): ${e.message}")
                // 4. Android 10+ kısıtında bildirim ve Full-Screen Intent ile öne getir
                triggerFullScreenLaunch(context, launchIntent)
            }

        } catch (e: Exception) {
            Log.e(TAG, "BootReceiver çalışırken beklenmedik hata: ${e.message}", e)
        }
    }

    private fun wakeUpCpuBriefly(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RidoPlay:BootReceiverWakeLock"
            )
            wakeLock?.acquire(5000L) // En fazla 5 saniye
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock alınamadı: ${e.message}")
        }
    }

    private fun triggerFullScreenLaunch(context: Context, launchIntent: Intent) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "RidoPlay Otomatik Başlatma",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Araç açıldığında RidoPlay'i ekrana getirme bildirimi"
                    setSound(null, null)
                    enableVibration(false)
                }
                nm.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle("RidoPlay Başlatılıyor...")
                .setContentText("Müzik çalar açılıyor")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()

            nm.notify(NOTIFICATION_ID, notification)

            // 4 saniye sonra bildirimi otomatik temizle
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    nm.cancel(NOTIFICATION_ID)
                } catch (e: Exception) {}
            }, 4000L)

        } catch (e: Exception) {
            Log.e(TAG, "FullScreen notification hatası: ${e.message}")
        }
    }
}
