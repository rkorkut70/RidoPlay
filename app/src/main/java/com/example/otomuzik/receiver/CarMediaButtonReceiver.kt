package com.example.otomuzik.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.otomuzik.service.MusicController
import com.example.otomuzik.service.MusicService

/**
 * Araç Direksiyon Kumandası (SWC - Steering Wheel Controls) ve Donanım Medya Tuşları Alıcısı.
 * 
 * Standart Android medya butonlarının (KeyEvent) yanı sıra Çin/Aftermarket araç multimedya ünitelerinde
 * (Allwinner, Rockchip, Microntek/MTC, FYT, UIS7862 vb.) kullanılan özel CAN-bus komutlarını da yakalar.
 */
class CarMediaButtonReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CarMediaButtonReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        Log.d(TAG, "Gelen eylem (Action): $action")

        when (action) {
            // 1. Standart Android Donanım / Direksiyon Medya Tuşu
            Intent.ACTION_MEDIA_BUTTON -> {
                val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }

                if (keyEvent != null) {
                    Log.d(TAG, "KeyEvent alındı: keyCode=${keyEvent.keyCode}, action=${keyEvent.action}")
                    if (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                        handleKeyCode(context, keyEvent.keyCode)
                    }
                }
            }

            // 2. AOSP ve Çin Araç Multimedya Sistemleri Komutları (Allwinner, Rockchip, MTK vb.)
            "com.android.music.musicservicecommand" -> {
                val command = intent.getStringExtra("command")?.lowercase()
                Log.d(TAG, "Araç servis komutu alındı: $command")
                when (command) {
                    "next" -> sendServiceAction(context, MusicService.ACTION_NEXT)
                    "previous", "prev" -> sendServiceAction(context, MusicService.ACTION_PREV)
                    "togglepause", "playpause" -> sendServiceAction(context, MusicService.ACTION_TOGGLE_PAUSE)
                    "pause" -> sendServiceAction(context, MusicService.ACTION_PAUSE)
                    "play" -> sendServiceAction(context, MusicService.ACTION_PLAY)
                    "stop" -> sendServiceAction(context, MusicService.ACTION_STOP)
                }
            }

            // 3. Microntek / MTC Car Head Units CAN-bus Tuşları
            "com.microntek.irkeyDown" -> {
                val keyCode = intent.getIntExtra("keyCode", -1)
                Log.d(TAG, "Microntek tuşu: $keyCode")
                handleMicrontekKey(context, keyCode)
            }

            // 4. FYT / UIS7862 / Joying / Teyes Car Units
            "com.fyt.system.action.KEY" -> {
                val keyCode = intent.getIntExtra("keyCode", -1)
                Log.d(TAG, "FYT tuşu: $keyCode")
                if (keyCode != -1) {
                    handleKeyCode(context, keyCode)
                }
            }

            // 5. Kulaklık veya araç ses bağlantısı kesildiğinde müziği duraklat
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                sendServiceAction(context, MusicService.ACTION_PAUSE)
            }
        }
    }

    private fun handleKeyCode(context: Context, keyCode: Int) {
        when (keyCode) {
            // Sonraki Şarkı (Direksiyon kumandasında en sık gönderilen tuş kodları)
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_NAVIGATE_NEXT,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                sendServiceAction(context, MusicService.ACTION_NEXT)
            }

            // Önceki Şarkı
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                sendServiceAction(context, MusicService.ACTION_PREV)
            }

            // Oynat / Duraklat
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                sendServiceAction(context, MusicService.ACTION_TOGGLE_PAUSE)
            }

            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                sendServiceAction(context, MusicService.ACTION_PLAY)
            }

            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                sendServiceAction(context, MusicService.ACTION_PAUSE)
            }

            KeyEvent.KEYCODE_MEDIA_STOP -> {
                sendServiceAction(context, MusicService.ACTION_STOP)
            }
        }
    }

    private fun handleMicrontekKey(context: Context, keyCode: Int) {
        when (keyCode) {
            12, 23 -> sendServiceAction(context, MusicService.ACTION_NEXT)
            11, 24 -> sendServiceAction(context, MusicService.ACTION_PREV)
            10 -> sendServiceAction(context, MusicService.ACTION_TOGGLE_PAUSE)
        }
    }

    private fun sendServiceAction(context: Context, action: String) {
        if (MusicController.isServiceBound) {
            when (action) {
                MusicService.ACTION_NEXT -> MusicController.playNext()
                MusicService.ACTION_PREV -> MusicController.playPrevious()
                MusicService.ACTION_TOGGLE_PAUSE -> MusicController.togglePlayPause()
                MusicService.ACTION_PLAY -> MusicController.resumePlayback()
                MusicService.ACTION_PAUSE -> MusicController.pausePlayback()
                MusicService.ACTION_STOP -> MusicController.stopPlayback()
            }
        } else {
            try {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    this.action = action
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                try {
                    context.startService(Intent(context, MusicService::class.java).apply {
                        this.action = action
                    })
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }
}
