package com.example.otomuzik.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.otomuzik.model.PlayerState
import com.example.otomuzik.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

object MusicController {

    private var musicService: MusicService? = null
    private var isBound = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _equalizerState = MutableStateFlow(com.example.otomuzik.model.EqualizerState())
    val equalizerState: StateFlow<com.example.otomuzik.model.EqualizerState> = _equalizerState.asStateFlow()

    /**
     * Aktif çalma sırasındaki şarkılar — PlayerState.currentQueue'dan türetilir.
     * Shuffle açıksa karıştırılmış, kapalıysa tam liste yayınlanır.
     */
    val currentQueue: StateFlow<List<Song>> = _playerState
        .map { it.currentQueue }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Şu an çalan şarkının sıradaki indeksi — PlayerState.currentIndex'ten türetilir.
     */
    val currentQueueIndex: StateFlow<Int> = _playerState
        .map { it.currentIndex }
        .stateIn(scope, SharingStarted.Eagerly, -1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicService.MusicBinder
            musicService = binder?.getService()
            isBound = true

            // Servisin StateFlow'larını UI StateFlow'larına köprüle
            musicService?.let { s ->
                scope.launch {
                    s.playerState.collect { state ->
                        _playerState.value = state
                    }
                }
                scope.launch {
                    s.equalizerState.collect { eqState ->
                        _equalizerState.value = eqState
                    }
                }
                scope.launch {
                    s.sleepTimerLeftMs.collect { ms ->
                        _sleepTimerLeftMs.value = ms
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    val isServiceBound: Boolean
        get() = isBound && musicService != null

    fun initService(context: Context) {
        val appCtx = context.applicationContext
        val serviceIntent = Intent(appCtx, MusicService::class.java)

        // Servisi öncelikli olarak başlat
        try {
            ContextCompat.startForegroundService(appCtx, serviceIntent)
        } catch (e: Exception) {
            // Android 12 kısıtlamalarına karşı yedek
            try {
                appCtx.startService(serviceIntent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        try {
            appCtx.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        musicService?.setQueue(songs, startIndex, autoPlay)
    }

    fun addToQueue(songs: List<Song>) {
        musicService?.addToQueue(songs)
    }

    fun syncQueueWithLibrary(allSongs: List<Song>) {
        musicService?.syncQueueWithLibrary(allSongs)
    }

    fun playSong(song: Song) {
        musicService?.playSong(song)
    }

    fun playSongAt(index: Int) {
        musicService?.playSongAt(index)
    }

    /** [playSongAt] için okunabilir alias — Queue UI katmanından çağrılır. */
    fun playAtIndex(index: Int) = playSongAt(index)

    fun togglePlayPause() {
        musicService?.togglePlayPause()
    }

    fun resumePlayback() {
        musicService?.resumePlayback()
    }

    fun pausePlayback() {
        musicService?.pausePlayback()
    }

    fun stopPlayback() {
        musicService?.stopPlayback()
    }

    fun playNext() {
        musicService?.playNext()
    }

    fun playPrevious() {
        musicService?.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        musicService?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        musicService?.toggleShuffle()
    }

    fun reshuffleUpcoming() {
        musicService?.reshuffleUpcoming()
    }

    fun toggleRepeatMode() {
        musicService?.toggleRepeatMode()
    }

    // --- Ekolayzer (EQ) İşlemleri ---
    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        musicService?.setBandLevel(bandIndex, levelMb)
    }

    fun setPreset(presetName: String) {
        musicService?.setPreset(presetName)
    }

    fun setBassBoost(strength: Short) {
        musicService?.setBassBoost(strength)
    }

    fun toggleEqualizer(enabled: Boolean) {
        musicService?.toggleEqualizer(enabled)
    }
    
    fun setLoudnessEnhancerEnabled(enabled: Boolean) {
        musicService?.setLoudnessEnhancerEnabled(enabled)
    }

    // --- Sleep Timer ---
    private val _sleepTimerLeftMs = MutableStateFlow<Long?>(null)
    val sleepTimerLeftMs: StateFlow<Long?> = _sleepTimerLeftMs.asStateFlow()

    fun setSleepTimer(minutes: Int) {
        musicService?.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        musicService?.cancelSleepTimer()
    }

    fun updateCurrentSongInfo(updatedSong: Song) {
        musicService?.updateCurrentSongInfo(updatedSong)
        val current = _playerState.value.currentSong
        if (current != null) {
            _playerState.value = _playerState.value.copy(
                currentSong = updatedSong,
                currentQueue = _playerState.value.currentQueue.map {
                    if (it.path == current.path || it.id == updatedSong.id) updatedSong else it
                }
            )
        }
    }
}
