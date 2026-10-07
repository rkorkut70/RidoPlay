package com.example.otomuzik.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.KeyEvent
import com.example.otomuzik.MainActivity
import com.example.otomuzik.R
import com.example.otomuzik.data.MusicRepository
import com.example.otomuzik.model.EqBand
import com.example.otomuzik.model.EqualizerState
import com.example.otomuzik.model.PlayerState
import com.example.otomuzik.model.RepeatMode
import com.example.otomuzik.model.Song
import com.example.otomuzik.receiver.CarMediaButtonReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaBrowserCompat
import android.os.Bundle

import androidx.core.app.NotificationCompat

class MusicService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val CHANNEL_ID = "oto_muzik_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.otomuzik.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.otomuzik.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.otomuzik.ACTION_NEXT"
        const val ACTION_PREV = "com.example.otomuzik.ACTION_PREV"
        const val ACTION_STOP = "com.example.otomuzik.ACTION_STOP"
        const val ACTION_TOGGLE_PAUSE = "com.example.otomuzik.ACTION_TOGGLE_PAUSE"
    }

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null

    // --- Crossfade (Şarkılar Arası Yumuşak Geçiş) ---
    /** Crossfade sırasında yeni şarkıyı çalan ikinci player */
    private var crossfadePlayer: MediaPlayer? = null
    /** Crossfade animasyonu devam ediyor mu? */
    private var isCrossfading = false
    /** Crossfade animasyon coroutine job'ı (iptal edilebilir) */
    private var crossfadeJob: Job? = null
    private var mediaSession: MediaSession? = null
    private lateinit var audioManager: AudioManager
    private lateinit var repository: MusicRepository

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()
    
    private var sleepTimerJob: Job? = null
    private val _sleepTimerLeftMs = MutableStateFlow<Long?>(null)
    val sleepTimerLeftMs: StateFlow<Long?> = _sleepTimerLeftMs.asStateFlow()

    private var audioFocusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var progressTrackerJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var fullQueue = listOf<Song>()
    private var activeQueue = listOf<Song>()
    private var currentIndex = -1

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private val carBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                "com.android.music.musicservicecommand" -> {
                    when (intent.getStringExtra("command")?.lowercase()) {
                        "next" -> playNext()
                        "previous", "prev" -> playPrevious()
                        "togglepause", "playpause" -> togglePlayPause()
                        "pause" -> pausePlayback()
                        "play" -> resumePlayback()
                        "stop" -> stopPlayback()
                    }
                }
                "com.microntek.irkeyDown" -> {
                    when (intent.getIntExtra("keyCode", -1)) {
                        12, 23 -> playNext()
                        11, 24 -> playPrevious()
                        10 -> togglePlayPause()
                    }
                }
                "com.fyt.system.action.KEY" -> {
                    val kc = intent.getIntExtra("keyCode", -1)
                    if (kc != -1) {
                        handleMediaKeyCode(kc)
                    }
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    pausePlayback()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            repository = MusicRepository(applicationContext)
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            createNotificationChannel()
            startStandbyForeground()
            setupMediaSession()

            // Çin ve AOSP Araç Multimedya Yayınlarını (CAN-bus) Dinle
            val filter = IntentFilter().apply {
                addAction("com.android.music.musicservicecommand")
                addAction("com.microntek.irkeyDown")
                addAction("com.fyt.system.action.KEY")
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(carBroadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(carBroadcastReceiver, filter)
            }

            // Eski / Çin araç ROM'ları için AudioManager medya butonu kaydı
            try {
                @Suppress("DEPRECATION")
                audioManager.registerMediaButtonEventReceiver(
                    ComponentName(packageName, CarMediaButtonReceiver::class.java.name)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Son kaydedilen ayarları yükle
            val lastShuffle = repository.getLastShuffle()
            val lastRepeat = try {
                RepeatMode.valueOf(repository.getLastRepeat())
            } catch (e: Exception) {
                RepeatMode.OFF
            }

            _playerState.update {
                it.copy(isShuffle = lastShuffle, repeatMode = lastRepeat)
            }

            val eqEnabled = repository.getEqEnabled()
            val eqPreset = repository.getEqPreset()
            val bassBoostStr = repository.getEqBassBoost()
            val loudnessEnhancerEnabled = repository.getEqLoudnessEnhancerEnabled()
            _equalizerState.update {
                it.copy(
                    isEnabled = eqEnabled,
                    currentPreset = eqPreset,
                    bassBoostStrength = bassBoostStr,
                    isLoudnessEnhancerEnabled = loudnessEnhancerEnabled
                )
            }
        } catch (t: Throwable) {
            android.util.Log.e("MusicService", "Error during onCreate: ${t.message}", t)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (_playerState.value.currentSong == null) {
            startStandbyForeground()
        }
        if (Intent.ACTION_MEDIA_BUTTON == intent?.action) {
            val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }
            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                handleMediaKeyCode(keyEvent.keyCode)
            }
        }
        when (intent?.action) {
            ACTION_PLAY -> resumePlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrevious()
            ACTION_STOP -> stopPlayback()
            ACTION_TOGGLE_PAUSE -> togglePlayPause()
        }
        return START_NOT_STICKY
    }

    private fun handleMediaKeyCode(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_NAVIGATE_NEXT,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                playNext()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                playPrevious()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                togglePlayPause()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                resumePlayback()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                pausePlayback()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_STOP -> {
                stopPlayback()
                return true
            }
        }
        return false
    }

    private fun setupMediaSession() {
        try {
            val session = MediaSession(this, "OtoMuzikSession").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        resumePlayback()
                    }

                    override fun onPause() {
                        pausePlayback()
                    }

                    override fun onSkipToNext() {
                        playNext()
                    }

                    override fun onSkipToPrevious() {
                        playPrevious()
                    }

                    override fun onFastForward() {
                        playNext()
                    }

                    override fun onRewind() {
                        playPrevious()
                    }

                    override fun onSeekTo(pos: Long) {
                        seekTo(pos)
                    }

                    override fun onStop() {
                        stopPlayback()
                    }

                    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                        val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                        }
                        if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                            val handled = handleMediaKeyCode(keyEvent.keyCode)
                            if (handled) return true
                        }
                        return super.onMediaButtonEvent(mediaButtonIntent)
                    }
                })

                // ÖNEMLİ: Araç CAN-bus ve direksiyon tuşlarının bu oturuma yönlendirilmesi için flag'ler
                @Suppress("DEPRECATION")
                setFlags(
                    MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
                )

                try {
                    val mbrIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null, applicationContext, CarMediaButtonReceiver::class.java)
                    val mbrPending = PendingIntent.getBroadcast(
                        applicationContext,
                        0,
                        mbrIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setMediaButtonReceiver(mbrPending)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }

                isActive = true
            }
            mediaSession = session
            
            try {
                sessionToken = android.support.v4.media.session.MediaSessionCompat.Token.fromToken(session.sessionToken)
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            // Başlangıçta PlaybackState'i hemen yayınla (Direksiyon tuşları hazır olsun)
            updateMediaSessionState(PlaybackState.STATE_NONE, 0L)
        } catch (t: Throwable) {
            android.util.Log.e("MusicService", "Error setting up MediaSession: ${t.message}", t)
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return
        fullQueue = songs
        currentIndex = startIndex.coerceIn(0, songs.size - 1)

        if (_playerState.value.isShuffle) {
            buildShuffledQueue(fullQueue[currentIndex])
        } else {
            activeQueue = fullQueue
        }

        _playerState.update {
            it.copy(
                currentQueue = activeQueue,
                currentIndex = currentIndex,
                currentSong = activeQueue.getOrNull(currentIndex)
            )
        }

        if (autoPlay) {
            playSongAt(currentIndex)
        }
    }

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        if (activeQueue.isEmpty()) {
            setQueue(songs, 0, autoPlay = true)
            return
        }
        val updated = activeQueue.toMutableList().apply { addAll(songs) }
        activeQueue = updated
        fullQueue = fullQueue.toMutableList().apply { addAll(songs) }
        _playerState.update {
            it.copy(currentQueue = activeQueue)
        }
    }

    /**
     * Kütüphane yeniden tarandığında (rescan) aktif çalma sırasını günceller:
     * - Silinen müzikleri sıradan temizler.
     * - Yeni eklenen müzikleri sıraya ekler.
     * - Şarkı metaverilerini tazeler.
     * - Aktif çalan şarkının indeksini korur.
     */
    fun syncQueueWithLibrary(allSongs: List<Song>) {
        if (allSongs.isEmpty()) {
            fullQueue = emptyList()
            activeQueue = emptyList()
            currentIndex = -1
            _playerState.update {
                it.copy(
                    currentQueue = emptyList(),
                    currentIndex = -1,
                    currentSong = null,
                    isPlaying = false
                )
            }
            return
        }

        val allSongsMap = allSongs.associateBy { it.path }
        val currentPlayingSong = _playerState.value.currentSong

        // 1. Silinen dosyaları sıradan çıkar, mevcut olanların metaverilerini güncelle
        val updatedFullQueue = fullQueue.mapNotNull { song ->
            allSongsMap[song.path] ?: if (java.io.File(song.path).exists()) song else null
        }.toMutableList()

        // 2. Eğer kullanıcı tüm kütüphaneyi çalıyorsa veya sıra boşsa yeni eklenen şarkıları da sıraya dahil et
        val existingPaths = updatedFullQueue.map { it.path }.toSet()
        val newlyAddedSongs = allSongs.filter { it.path !in existingPaths }

        // Eğer mevcut sıra tüm şarkıları temsil ediyorsa (veya sıra boşsa), yeni eklenenleri de ekle
        if (fullQueue.isEmpty() || fullQueue.size >= (allSongs.size - newlyAddedSongs.size - 2)) {
            updatedFullQueue.addAll(newlyAddedSongs)
        }

        fullQueue = updatedFullQueue

        if (_playerState.value.isShuffle) {
            val curSong = currentPlayingSong?.let { allSongsMap[it.path] } ?: updatedFullQueue.firstOrNull()
            if (curSong != null) {
                buildShuffledQueue(curSong)
            } else {
                activeQueue = updatedFullQueue
            }
        } else {
            activeQueue = updatedFullQueue
        }

        // Yeni index'i belirle
        val newIndex = if (currentPlayingSong != null) {
            val idx = activeQueue.indexOfFirst { it.path == currentPlayingSong.path }
            if (idx != -1) idx else 0.coerceAtMost(activeQueue.size - 1)
        } else {
            0.coerceAtMost(activeQueue.size - 1)
        }

        currentIndex = newIndex
        val newCurrentSong = activeQueue.getOrNull(newIndex)

        _playerState.update {
            it.copy(
                currentQueue = activeQueue,
                currentIndex = currentIndex,
                currentSong = newCurrentSong
            )
        }
    }

    fun playSong(song: Song) {
        val idx = activeQueue.indexOfFirst { it.path == song.path || (it.id != 0L && it.id == song.id) }
        if (idx != -1) {
            playSongAt(idx)
        } else {
            // Tekil şarkı veya yeni liste başlatma
            setQueue(listOf(song), 0, true)
        }
    }

    fun playSongAt(index: Int) {
        if (index !in activeQueue.indices) return
        currentIndex = index
        val song = activeQueue[index]

        if (!requestAudioFocus()) {
            return
        }

        // Halihazırda bir şarkı çalıyorsa crossfade ile geçiş yap
        val currentlyPlaying = mediaPlayer?.isPlaying == true
        if (currentlyPlaying && !isCrossfading) {
            startCrossfade(song)
            return
        }

        // Crossfade zaten devam ediyorsa veya çalmıyorsa → normal yükleme
        releaseMediaPlayer()

        try {
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (song.path.isNotEmpty() && File(song.path).exists()) {
                    setDataSource(song.path)
                } else {
                    setDataSource(applicationContext, Uri.parse(song.uriString))
                }

                prepare()
                start()
                setupAudioEffects(audioSessionId)

                setOnCompletionListener {
                    handleTrackCompletion()
                }

                setOnErrorListener { _, _, _ ->
                    playNext()
                    true
                }
            }

            val duration = mediaPlayer?.duration?.toLong() ?: song.durationMs

            _playerState.update {
                it.copy(
                    currentSong = song,
                    isPlaying = true,
                    currentPositionMs = 0L,
                    durationMs = duration,
                    currentIndex = currentIndex
                )
            }

            startForegroundServiceWithNotification(song, true)
            updateMediaSessionState(PlaybackState.STATE_PLAYING, 0L)
            updateMediaSessionMetadata(song, duration)
            startProgressTracker()

            // Son çalınan şarkıyı kaydet (araç kontağı kapandığında son durumu bilmek için)
            repository.saveLastPlaybackState(
                song.path,
                0L,
                _playerState.value.isShuffle,
                _playerState.value.repeatMode.name
            )
            
            serviceScope.launch(Dispatchers.IO) {
                repository.incrementPlayCount(song.path)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            _playerState.update { it.copy(isPlaying = false) }
        }
    }


    fun resumePlayback() {
        val player = mediaPlayer
        if (player != null && !player.isPlaying) {
            if (requestAudioFocus()) {
                player.start()
                _playerState.update { it.copy(isPlaying = true) }
                _playerState.value.currentSong?.let {
                    startForegroundServiceWithNotification(it, true)
                    updateMediaSessionState(PlaybackState.STATE_PLAYING, player.currentPosition.toLong())
                }
                startProgressTracker()
            }
        } else if (player == null && activeQueue.isNotEmpty()) {
            playSongAt(currentIndex.coerceAtLeast(0))
        } else if (activeQueue.isEmpty()) {
            serviceScope.launch(Dispatchers.IO) {
                val songs = repository.getAllSongs()
                if (songs.isNotEmpty()) {
                    val lastPath = repository.getLastPlayedPath()
                    val lastIdx = if (lastPath != null) {
                        songs.indexOfFirst { it.path == lastPath }.takeIf { it != -1 } ?: 0
                    } else 0
                    withContext(Dispatchers.Main) {
                        setQueue(songs, lastIdx, true)
                    }
                }
            }
        }
    }

    fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                val currentPos = it.currentPosition.toLong()
                _playerState.update { state ->
                    state.copy(isPlaying = false, currentPositionMs = currentPos)
                }
                stopProgressTracker()
                _playerState.value.currentSong?.let { song ->
                    startForegroundServiceWithNotification(song, false)
                    updateMediaSessionState(PlaybackState.STATE_PAUSED, currentPos)
                    repository.saveLastPlaybackState(
                        song.path,
                        currentPos,
                        _playerState.value.isShuffle,
                        _playerState.value.repeatMode.name
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun playNext() {
        if (activeQueue.isEmpty()) {
            serviceScope.launch(Dispatchers.IO) {
                val songs = repository.getAllSongs()
                if (songs.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        setQueue(songs, 0, true)
                    }
                }
            }
            return
        }
        if (currentIndex < activeQueue.size - 1) {
            playSongAt(currentIndex + 1)
        } else if (_playerState.value.repeatMode == RepeatMode.ALL) {
            playSongAt(0)
        } else {
            // Listenin sonu
            pausePlayback()
            seekTo(0)
        }
    }

    fun playPrevious() {
        if (activeQueue.isEmpty()) {
            serviceScope.launch(Dispatchers.IO) {
                val songs = repository.getAllSongs()
                if (songs.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        setQueue(songs, 0, true)
                    }
                }
            }
            return
        }
        val currentPos = mediaPlayer?.currentPosition ?: 0
        if (currentPos > 3000) {
            // Şarkı 3 saniyeden uzun çalıyorsa başa sar
            seekTo(0)
        } else {
            if (currentIndex > 0) {
                playSongAt(currentIndex - 1)
            } else if (_playerState.value.repeatMode == RepeatMode.ALL) {
                playSongAt(activeQueue.size - 1)
            } else {
                seekTo(0)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, _playerState.value.durationMs)
        mediaPlayer?.seekTo(target.toInt())
        _playerState.update { it.copy(currentPositionMs = target) }
        updateMediaSessionState(
            if (_playerState.value.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
            target
        )
    }

    fun toggleShuffle() {
        val newShuffle = !_playerState.value.isShuffle
        val currentSong = _playerState.value.currentSong

        if (newShuffle) {
            buildShuffledQueue(currentSong)
        } else {
            activeQueue = fullQueue
            currentIndex = if (currentSong != null) {
                activeQueue.indexOfFirst { it.path == currentSong.path }.coerceAtLeast(0)
            } else 0
        }

        _playerState.update {
            it.copy(
                isShuffle = newShuffle,
                currentQueue = activeQueue,
                currentIndex = currentIndex
            )
        }

        repository.saveLastPlaybackState(
            currentSong?.path ?: "",
            _playerState.value.currentPositionMs,
            newShuffle,
            _playerState.value.repeatMode.name
        )
    }

    fun toggleRepeatMode() {
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        _playerState.update { it.copy(repeatMode = nextMode) }

        repository.saveLastPlaybackState(
            _playerState.value.currentSong?.path ?: "",
            _playerState.value.currentPositionMs,
            _playerState.value.isShuffle,
            nextMode.name
        )
    }

    fun updateCurrentSongInfo(updatedSong: Song) {
        val current = _playerState.value.currentSong
        if (current != null) {
            _playerState.update { state ->
                val updatedQueue = state.currentQueue.map {
                    if (it.path == current.path || it.id == updatedSong.id) updatedSong else it
                }
                state.copy(currentSong = updatedSong, currentQueue = updatedQueue)
            }
            startForegroundServiceWithNotification(updatedSong, _playerState.value.isPlaying)
        }
    }

    private fun buildShuffledQueue(currentSong: Song?) {
        val list = fullQueue.toMutableList()
        if (currentSong != null) {
            list.remove(currentSong)
            list.shuffle()
            list.add(0, currentSong)
            activeQueue = list
            currentIndex = 0
        } else {
            list.shuffle()
            activeQueue = list
            currentIndex = 0
        }
    }

    fun reshuffleUpcoming() {
        if (activeQueue.isEmpty() || currentIndex !in activeQueue.indices) return
        val currentSong = activeQueue[currentIndex]
        val previousSongs = if (currentIndex > 0) activeQueue.take(currentIndex) else emptyList()
        val remaining = fullQueue.toMutableList().apply {
            remove(currentSong)
            removeAll(previousSongs)
        }
        remaining.shuffle()
        activeQueue = previousSongs + listOf(currentSong) + remaining
        _playerState.update {
            it.copy(
                isShuffle = true,
                currentQueue = activeQueue,
                currentIndex = currentIndex
            )
        }
    }

    private fun handleTrackCompletion() {
        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                _playerState.update { it.copy(isPlaying = true) }
            }
            RepeatMode.ALL -> {
                if (currentIndex < activeQueue.size - 1) {
                    playSongAt(currentIndex + 1)
                } else {
                    playSongAt(0)
                }
            }
            RepeatMode.OFF -> {
                if (currentIndex < activeQueue.size - 1) {
                    playSongAt(currentIndex + 1)
                } else {
                    pausePlayback()
                    seekTo(0)
                }
            }
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackerJob = serviceScope.launch {
            // Her 10 saniyede bir otomatik konum kayıt için sayaç (40 x 250ms = 10s)
            var autoSaveCounter = 0
            while (isActive) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val pos = player.currentPosition.toLong()
                    _playerState.update { it.copy(currentPositionMs = pos) }

                    // Her 40 turda bir (≈10 saniye) çalma konumunu kalıcı olarak kaydet
                    autoSaveCounter++
                    if (autoSaveCounter >= 40) {
                        autoSaveCounter = 0
                        saveCurrentPosition()
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    // --- Audio Focus (Navigasyon yönlendirmelerinde sesi kısma ve Bluetooth aramalarında duraklatma) ---
    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()

            audioFocusRequest = request
            val res = audioManager.requestAudioFocus(request)
            return res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val res = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            return res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            // Navigasyon konuşurken müziğin sesini kıs (Ducking)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            // Kısa süreli odak kaybı (örn: gelen telefon çağrısı) -> duraklat
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_playerState.value.isPlaying) {
                    resumeOnFocusGain = true
                    mediaPlayer?.pause()
                    _playerState.update { it.copy(isPlaying = false) }
                }
            }
            // Kalıcı odak kaybı (başka bir müzik uygulaması açıldı) -> tamamen durdur
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                pausePlayback()
            }
            // Odak geri geldi -> sesi tam seviyeye yükselt, duraklatılmışsa devam et
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    resumePlayback()
                }
            }
        }
    }

    // --- MediaSession Senkronizasyonu (Direksiyon Tuşları & Araç Paneli Desteği) ---
    private fun updateMediaSessionState(state: Int, position: Long) {
        try {
            val actions = PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP or
                    PlaybackState.ACTION_FAST_FORWARD or
                    PlaybackState.ACTION_REWIND

            val stateBuilder = PlaybackState.Builder()
                .setActions(actions)
                .setState(state, position, 1.0f)

            mediaSession?.setPlaybackState(stateBuilder.build())
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun updateMediaSessionMetadata(song: Song, duration: Long) {
        try {
            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .build()

            mediaSession?.setMetadata(metadata)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    // --- Foreground Service Bildirimi (Android Go 12 LMK Koruması) ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startStandbyForeground() {
        try {
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RidoPlay")
                .setContentText("Müzik çalar hazır")
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(openPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)

            val notification = builder.build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun startForegroundServiceWithNotification(song: Song, isPlaying: Boolean) {
        try {
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val prevIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PREV }
            val prevPending = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE)

            val playPauseIntent = Intent(this, MusicService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            }
            val playPausePending = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)

            val nextIntent = Intent(this, MusicService::class.java).apply { action = ACTION_NEXT }
            val nextPending = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE)

            val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSubText(song.album)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(openPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_skip_previous, "Önceki", prevPending)
                .addAction(playPauseIcon, if (isPlaying) "Duraklat" else "Oynat", playPausePending)
                .addAction(R.drawable.ic_skip_next, "Sonraki", nextPending)
                .setOngoing(isPlaying)

            try {
                val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                sessionToken?.let { token ->
                    mediaStyle.setMediaSession(token)
                }
                builder.setStyle(mediaStyle)
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            val notification = builder.build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    // --- Ekolayzer (EQ) ve Bas Güçlendirme (BassBoost) ---
    private fun setupAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            releaseAudioEffects()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }

            equalizer?.let { eq ->
                val numBands = eq.numberOfBands
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                val bandList = mutableListOf<EqBand>()

                val savedBands = repository.getEqBandLevels()

                for (i in 0 until numBands) {
                    val idx = i.toShort()
                    val freq = eq.getCenterFreq(idx) / 1000 // Hz
                    val savedLvl = savedBands[idx]
                    val level = if (savedLvl != null) {
                        try {
                            eq.setBandLevel(idx, savedLvl)
                            savedLvl
                        } catch (e: Exception) {
                            eq.getBandLevel(idx)
                        }
                    } else {
                        eq.getBandLevel(idx)
                    }
                    bandList.add(EqBand(idx, freq, level, minLevel, maxLevel))
                }

                val numPresets = eq.numberOfPresets
                val presetList = mutableListOf<String>()
                for (p in 0 until numPresets) {
                    presetList.add(eq.getPresetName(p.toShort()))
                }
                if (!presetList.contains("Özel")) presetList.add("Özel")

                _equalizerState.update {
                    it.copy(
                        bands = bandList,
                        presets = presetList,
                        currentPreset = repository.getEqPreset()
                    )
                }
            }

            try {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    if (strengthSupported) {
                        setStrength(_equalizerState.value.bassBoostStrength)
                        enabled = true
                    }
                }
                _equalizerState.update {
                    it.copy(isBassBoostSupported = bassBoost?.strengthSupported == true)
                }
            } catch (e: Exception) {
                bassBoost = null
                _equalizerState.update { it.copy(isBassBoostSupported = false) }
            }

            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(500)
                    enabled = _equalizerState.value.isLoudnessEnhancerEnabled
                }
            } catch (e: Exception) {
                loudnessEnhancer = null
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseAudioEffects() {
        try { equalizer?.release() } catch (e: Exception) {}
        try { bassBoost?.release() } catch (e: Exception) {}
        try { loudnessEnhancer?.release() } catch (e: Exception) {}
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
    }

    fun setLoudnessEnhancerEnabled(enabled: Boolean) {
        loudnessEnhancer?.enabled = enabled
        _equalizerState.update { it.copy(isLoudnessEnhancerEnabled = enabled) }
        repository.setEqLoudnessEnhancerEnabled(enabled)
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        equalizer?.let { eq ->
            try {
                eq.setBandLevel(bandIndex, levelMb)
                _equalizerState.update { state ->
                    val updated = state.bands.map {
                        if (it.index == bandIndex) it.copy(levelMb = levelMb) else it
                    }
                    state.copy(bands = updated, currentPreset = "Özel")
                }
                persistEq()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setPreset(presetName: String) {
        equalizer?.let { eq ->
            try {
                val numPresets = eq.numberOfPresets
                var foundIdx: Short? = null
                for (i in 0 until numPresets) {
                    if (eq.getPresetName(i.toShort()).equals(presetName, ignoreCase = true)) {
                        foundIdx = i.toShort()
                        break
                    }
                }
                if (foundIdx != null) {
                    eq.usePreset(foundIdx)
                    val updatedBands = _equalizerState.value.bands.map {
                        it.copy(levelMb = eq.getBandLevel(it.index))
                    }
                    _equalizerState.update {
                        it.copy(bands = updatedBands, currentPreset = presetName)
                    }
                    persistEq()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setBassBoost(strength: Short) {
        bassBoost?.let { bb ->
            try {
                if (bb.strengthSupported) {
                    bb.setStrength(strength)
                    _equalizerState.update { it.copy(bassBoostStrength = strength) }
                    persistEq()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        equalizer?.enabled = enabled
        _equalizerState.update { it.copy(isEnabled = enabled) }
        persistEq()
    }

    private fun persistEq() {
        val st = _equalizerState.value
        val bandMap = st.bands.associate { it.index to it.levelMb }
        repository.saveEqSettings(st.isEnabled, st.currentPreset, st.bassBoostStrength, bandMap)
    }

    /**
     * Mevcut çalma konumunu ve şarkı yolunu kalıcı olarak kaydeder.
     * Uygulama kapanışı veya periyodik otomatik kayıt sırasında çağrılır.
     * Tüm hatalar sessizce yutulur — kapanış akışını engellememek için.
     */
    private fun saveCurrentPosition() {
        try {
            val state = _playerState.value
            val song = state.currentSong ?: return
            val posMs = mediaPlayer?.currentPosition?.toLong() ?: state.currentPositionMs
            repository.saveLastPlaybackState(
                songPath  = song.path,
                positionMs = posMs,
                isShuffle  = state.isShuffle,
                repeatMode = state.repeatMode.name
            )
        } catch (e: Exception) {
            // Sessizce geç — kapanış sırasında hata olabilir
        }
    }

    /**
     * Mevcut şarkıyı fade-out yaparken yeni şarkıyı fade-in ile başlatır.
     *
     * Süre: 15 adım × 100ms = 1500ms
     *
     * Geçiş tamamlandıktan sonra:
     *  - Eski MediaPlayer durdurulup release edilir.
     *  - [mediaPlayer] yeni player'a güncellenir.
     *  - EQ ve BassBoost yeni player'ın audioSessionId'sine bağlanır.
     *
     * Herhangi bir hata durumunda crossfade iptal edilerek normal [playSongAt] çağrısına düşülür.
     *
     * @param song Oynatılacak yeni şarkı
     */
    private fun startCrossfade(song: Song) {
        // Önceki bir crossfade varsa iptal et
        crossfadeJob?.cancel()

        val crossfadeDuration = repository.getCrossfadeDuration()
        if (crossfadeDuration <= 0) {
            isCrossfading = false
            releaseMediaPlayer()
            try {
                // crossfadePlayer veya mediaPlayer serbest birakildi ama normal calmaya geciyoruz
                val actualIndex = activeQueue.indexOfFirst { it.path == song.path || (it.id != 0L && it.id == song.id) }
                val idx = if (actualIndex != -1) actualIndex else currentIndex
                
                // Dikkat: playSongAt(idx) icinde releaseMediaPlayer() bir daha cagriliyor, ama sorun degil.
                // startCrossfade icinden normal oynatmaya donmek icin yeni MediaPlayer baslatiyoruz:
                // Ama playSongAt'in ilk basinda eger oynuyorsa tekrar crossfade deniyor, loop olabilir.
                // Bu yuzden playSongAt yerine direkt oynamasini saglamak icin, playSongAt'in basinda isCrossfading kontrolu var.
            } catch(e: Exception) {}
            playSongAt(currentIndex)
            return
        }

        val fadeSteps = (crossfadeDuration / 100).coerceAtLeast(1)
        val stepMs = 100L
        val oldPlayer = mediaPlayer ?: run {
            // Eski player yoksa normal geçiş yap
            isCrossfading = false
            playSongAt(currentIndex)
            return
        }

        try {
            val newPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (song.path.isNotEmpty() && File(song.path).exists()) {
                    setDataSource(song.path)
                } else {
                    setDataSource(applicationContext, Uri.parse(song.uriString))
                }
                setVolume(0f, 0f) // Fade-in için sessiz başla
                prepare()
                
                // Ses efektlerini hemen başlat (1.5 sn sonraki duraklamayı önlemek için)
                setupAudioEffects(this.audioSessionId)
                
                start()
            }

            crossfadePlayer = newPlayer
            isCrossfading = true

            // UI'ı hemen yeni şarkı bilgileriyle güncelle
            val duration = newPlayer.duration.toLong().takeIf { it > 0 } ?: song.durationMs
            _playerState.update {
                it.copy(
                    currentSong = song,
                    isPlaying = true,
                    currentPositionMs = 0L,
                    durationMs = duration,
                    currentIndex = currentIndex
                )
            }
            startForegroundServiceWithNotification(song, true)
            updateMediaSessionState(PlaybackState.STATE_PLAYING, 0L)
            updateMediaSessionMetadata(song, duration)
            startProgressTracker()
            repository.saveLastPlaybackState(
                song.path,
                0L,
                _playerState.value.isShuffle,
                _playerState.value.repeatMode.name
            )
            
            serviceScope.launch(Dispatchers.IO) {
                repository.incrementPlayCount(song.path)
            }

            // Fade coroutine
            crossfadeJob = serviceScope.launch {
                for (step in 1..fadeSteps) {
                    val newVol = step.toFloat() / fadeSteps
                    val oldVol = 1f - newVol
                    try {
                        oldPlayer.setVolume(oldVol, oldVol)
                        newPlayer.setVolume(newVol, newVol)
                    } catch (e: Exception) {
                        break // Player release edilmiş olabilir — çık
                    }
                    delay(stepMs)
                }

                // --- Geçiş tamamlandı ---
                // Eski player'ı durdur ve serbest bırak
                try {
                    if (oldPlayer.isPlaying) oldPlayer.stop()
                    oldPlayer.release()
                } catch (e: Exception) {
                    // Sessizce geç
                }

                // Yeni player artık ana player
                newPlayer.setVolume(1f, 1f)
                newPlayer.setOnCompletionListener { handleTrackCompletion() }
                newPlayer.setOnErrorListener { _, _, _ -> playNext(); true }
                mediaPlayer = newPlayer
                crossfadePlayer = null
                isCrossfading = false
            }

        } catch (e: Exception) {
            // Crossfade başarısız → temiz geçiş yap
            e.printStackTrace()
            crossfadePlayer = null
            isCrossfading = false
            releaseMediaPlayer()
            playSongAt(currentIndex)
        }
    }

    private fun releaseMediaPlayer() {
        stopProgressTracker()
        releaseAudioEffects()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null

        crossfadePlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        crossfadePlayer = null
        isCrossfading = false
        crossfadeJob?.cancel()
    }

    fun stopPlayback() {
        releaseMediaPlayer()
        _playerState.update { it.copy(isPlaying = false) }
        updateMediaSessionState(PlaybackState.STATE_STOPPED, 0L)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val totalMs = minutes * 60 * 1000L
        _sleepTimerLeftMs.value = totalMs
        
        sleepTimerJob = serviceScope.launch {
            var timeLeft = totalMs
            while (timeLeft > 0) {
                delay(1000)
                timeLeft -= 1000
                _sleepTimerLeftMs.value = timeLeft
            }
            pausePlayback()
            _sleepTimerLeftMs.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerLeftMs.value = null
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("root_id", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == "root_id") {
            result.detach()
            serviceScope.launch(Dispatchers.IO) {
                val songs = repository.getAllSongs()
                val mediaItems = songs.map { song ->
                    val description = android.support.v4.media.MediaDescriptionCompat.Builder()
                        .setMediaId(song.path)
                        .setTitle(song.title)
                        .setSubtitle(song.artist)
                        .setIconUri(Uri.parse(song.uriString))
                        .build()
                    MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                }
                withContext(Dispatchers.Main) {
                    result.sendResult(mediaItems)
                }
            }
        } else {
            result.sendResult(emptyList())
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (repository.getStopOnTaskRemoved()) {
            stopPlayback()
        }
    }

    override fun onDestroy() {
        cancelSleepTimer()
        // Servis kapanmadan önce mevcut çalma konumunu kaydet
        saveCurrentPosition()
        super.onDestroy()
        serviceJob.cancel()
        releaseMediaPlayer()
        try {
            mediaSession?.release()
        } catch (t: Throwable) {}
        try {
            unregisterReceiver(carBroadcastReceiver)
        } catch (e: Exception) {}
        try {
            @Suppress("DEPRECATION")
            audioManager.unregisterMediaButtonEventReceiver(
                ComponentName(packageName, CarMediaButtonReceiver::class.java.name)
            )
        } catch (e: Exception) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }
}
