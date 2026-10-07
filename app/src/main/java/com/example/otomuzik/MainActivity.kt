package com.example.otomuzik

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.otomuzik.service.MusicController
import com.example.otomuzik.theme.CarBgDark
import com.example.otomuzik.theme.OtoMuzikTheme
import com.example.otomuzik.ui.components.PermissionScreen
import com.example.otomuzik.ui.main.MainScreen

class MainActivity : ComponentActivity() {

    private var hasStoragePermission by mutableStateOf(false)
    private var showSplash by mutableStateOf(true)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.any {
            (it.key == Manifest.permission.READ_EXTERNAL_STORAGE ||
             it.key == "android.permission.READ_MEDIA_AUDIO") && it.value
        }
        hasStoragePermission = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Açılışta ve Compose yüklenirken oluşabilecek mikro parlamaları tamamen engelle
        window.setBackgroundDrawableResource(R.color.car_bg_dark)

        // Otomatik klavye açılmasını engelle
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        // Donanım ses tuşlarının doğrudan medya sesini kontrol etmesini sağla
        try {
            volumeControlStream = AudioManager.STREAM_MUSIC
        } catch (t: Throwable) {}

        // Arka plan müzik servisini başlat
        try {
            MusicController.initService(this)
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "MusicController.initService failed: ${t.message}", t)
        }

        checkStoragePermission()

        setContent {
            val viewModel: com.example.otomuzik.ui.main.MainScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themePalette by viewModel.themePalette.collectAsStateWithLifecycle()
            OtoMuzikTheme(
                themeMode = themeMode,
                themePalette = themePalette
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        com.example.otomuzik.ui.components.CarSplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    } else if (hasStoragePermission) {
                        LaunchedEffect(hasStoragePermission) {
                            if (viewModel.allSongs.value.isEmpty()) {
                                viewModel.refreshAll()
                            }
                        }
                        MainScreen(
                            hasPermission = true,
                            onRequestPermission = { requestRequiredPermissions() },
                            viewModel = viewModel
                        )
                    } else {
                        PermissionScreen(
                            onRequestPermission = { requestRequiredPermissions() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
        checkStoragePermission()
    }

    // --- Araç Direksiyon Kumandası & Donanım Medya Tuşları (Ön Plan Yakalama) ---
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (event.keyCode) {
                // Direksiyon "Sonraki Şarkı" veya "Kanal İleri" Tuşu
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
                KeyEvent.KEYCODE_CHANNEL_UP,
                KeyEvent.KEYCODE_NAVIGATE_NEXT,
                KeyEvent.KEYCODE_PAGE_UP,
                KeyEvent.KEYCODE_BUTTON_R1 -> {
                    MusicController.playNext()
                    return true
                }

                // Direksiyon "Önceki Şarkı" veya "Kanal Geri" Tuşu
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
                KeyEvent.KEYCODE_CHANNEL_DOWN,
                KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
                KeyEvent.KEYCODE_PAGE_DOWN,
                KeyEvent.KEYCODE_BUTTON_L1 -> {
                    MusicController.playPrevious()
                    return true
                }

                // Direksiyon "Oynat / Duraklat" Tuşu
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    MusicController.togglePlayPause()
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    MusicController.resumePlayback()
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    MusicController.pausePlayback()
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    MusicController.stopPlayback()
                    return true
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            // İlgili tuşların UP eylemini de tüketerek sistemin varsayılan bip sesini/eylemini engelle
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
                KeyEvent.KEYCODE_CHANNEL_UP,
                KeyEvent.KEYCODE_NAVIGATE_NEXT,
                KeyEvent.KEYCODE_PAGE_UP,
                KeyEvent.KEYCODE_BUTTON_R1,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
                KeyEvent.KEYCODE_CHANNEL_DOWN,
                KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
                KeyEvent.KEYCODE_PAGE_DOWN,
                KeyEvent.KEYCODE_BUTTON_L1,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_STOP -> return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_NAVIGATE_NEXT,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                MusicController.playNext()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                MusicController.playPrevious()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                MusicController.togglePlayPause()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                MusicController.resumePlayback()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                MusicController.pausePlayback()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_STOP -> {
                MusicController.stopPlayback()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkStoragePermission() {
        try {
            val readGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_AUDIO") == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            
            val manageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    android.os.Environment.isExternalStorageManager()
                } catch (t: Throwable) {
                    false
                }
            } else {
                true
            }
            
            // Eğer normal okuma izni VEYA tüm dosyalara erişim izni verildiyse uygulamayı aç
            hasStoragePermission = readGranted || manageGranted
        } catch (t: Throwable) {
            hasStoragePermission = true
        }
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (!android.os.Environment.isExternalStorageManager()) {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = android.net.Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            startActivity(intent)
                        } catch (e2: Exception) {
                            android.util.Log.w("MainActivity", "All files access settings not found on this ROM: ${e2.message}")
                        }
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.w("MainActivity", "Error checking isExternalStorageManager: ${t.message}")
            }
        }

        try {
            val permissions = mutableListOf<String>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add("android.permission.READ_MEDIA_AUDIO")
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            permissionLauncher.launch(permissions.toTypedArray())
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Failed to launch permission request: ${t.message}", t)
        }
    }
}
