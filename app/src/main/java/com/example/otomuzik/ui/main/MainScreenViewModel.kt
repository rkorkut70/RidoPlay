package com.example.otomuzik.ui.main
import com.example.otomuzik.theme.*

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.otomuzik.data.MusicRepository
import com.example.otomuzik.model.FolderItem
import com.example.otomuzik.model.LyricsData
import com.example.otomuzik.model.PlayerState
import com.example.otomuzik.model.Playlist
import com.example.otomuzik.model.Song
import com.example.otomuzik.service.MusicController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.otomuzik.model.LyricsSource
import com.example.otomuzik.util.TurkishStringFixer
import java.io.File
import java.util.ArrayDeque

enum class TabType {
    HOME, FOLDERS, ALL_SONGS, PLAYLISTS, FAVORITES, QUEUE
}

@OptIn(FlowPreview::class)
class MainScreenViewModel(Application: Application) : AndroidViewModel(Application) {

    private val repository = MusicRepository(Application.applicationContext)

    val playerState: StateFlow<PlayerState> = MusicController.playerState

    private val _activeTab = MutableStateFlow(TabType.HOME)
    val activeTab: StateFlow<TabType> = _activeTab.asStateFlow()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * arama filtresi — 400ms debounce ile çalışır.
     *
     * Neden debounce?
     *   - Kullanıcı "şarkı" yazarken ş→şa→şar→şark→şarkı gibi 5 ara sorgu oluşur.
     *   - Binlerce şarkılı kütüphanede her tuşta filtreleme ana thread'i yorar.
     *   - 400ms: kullanıcı yazmayı bitirdiğinde tetiklenir, yazarken UI akıcı kalır.
     *
     * searchQuery (ham) anlık güncellenir → TextField'da karakter hemen görünür.
     * Filtreleme sadece debounced akış üzerinde çalışır → Liste 400ms sonra güncellenir.
     */
    val filteredSongs: StateFlow<List<Song>> = combine(
        _allSongs,
        _searchQuery.debounce(400L)   // ← 400ms bekle, sonra filtrele
    ) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            val qNorm = TurkishStringFixer.normalizeForSearch(query)
            val locale = java.util.Locale("tr", "TR")
            val qRaw = query.trim().lowercase(locale)

            songs.filter { song ->
                val titleNorm = TurkishStringFixer.normalizeForSearch(song.title)
                val artistNorm = TurkishStringFixer.normalizeForSearch(song.artist)
                val albumNorm = TurkishStringFixer.normalizeForSearch(song.album)
                val fileNorm = TurkishStringFixer.normalizeForSearch(File(song.path).nameWithoutExtension)
                val folderNorm = TurkishStringFixer.normalizeForSearch(File(song.path).parentFile?.name)

                // 1. Türkçe karakter duyarsız eşleşme (Araç klavyesinde s/ş, i/ı, c/ç, g/ğ, o/ö, u/ü eşleştirmesi)
                titleNorm.contains(qNorm)
                    || artistNorm.contains(qNorm)
                    || albumNorm.contains(qNorm)
                    || fileNorm.contains(qNorm)
                    || folderNorm.contains(qNorm)
                    // 2. Standart ID3 alanları eşleşmesi
                    || song.genre.lowercase(locale).contains(qRaw)
                    || song.year.contains(qRaw)
                    || song.format.lowercase(locale).contains(qRaw)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageRoots = MutableStateFlow<List<FolderItem>>(emptyList())
    val storageRoots: StateFlow<List<FolderItem>> = _storageRoots.asStateFlow()

    private val _currentFolderPath = MutableStateFlow<String?>(null)
    val currentFolderPath: StateFlow<String?> = _currentFolderPath.asStateFlow()

    private val _currentFolderItems = MutableStateFlow<List<FolderItem>>(emptyList())
    val currentFolderItems: StateFlow<List<FolderItem>> = _currentFolderItems.asStateFlow()

    private val _favoritePaths = MutableStateFlow<Set<String>>(emptySet())
    val favoritePaths: StateFlow<Set<String>> = _favoritePaths.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    fun openSettingsDialog() {
        onUserNavigated()
        _showSettingsDialog.value = true
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()

    fun openAboutDialog() {
        onUserNavigated()
        _showAboutDialog.value = true
    }

    fun closeAboutDialog() {
        _showAboutDialog.value = false
    }

    val favoriteSongs: StateFlow<List<Song>> = combine(_allSongs, _favoritePaths) { songs, favs ->
        songs.filter { favs.contains(it.path) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ÇÇalma Listeileri (Playlists) Durumu ---
    private val _Playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val Playlists: StateFlow<List<Playlist>> = _Playlists.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<Song>> = _selectedPlaylistSongs.asStateFlow()

    // --- Basılı Tutma (Context Menü) ve İletişim Pencereileri ---
    private val _folderContextItem = MutableStateFlow<FolderItem?>(null)
    val folderContextItem: StateFlow<FolderItem?> = _folderContextItem.asStateFlow()

    private val _songContextItem = MutableStateFlow<Song?>(null)
    val songContextItem: StateFlow<Song?> = _songContextItem.asStateFlow()

    private val _songsToaddToPlaylist = MutableStateFlow<List<Song>?>(null)
    val songsToaddToPlaylist: StateFlow<List<Song>?> = _songsToaddToPlaylist.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _batchId3FolderItem = MutableStateFlow<FolderItem?>(null)
    val batchId3FolderItem: StateFlow<FolderItem?> = _batchId3FolderItem.asStateFlow()

    fun openBatchId3Dialog(folder: FolderItem) {
        _batchId3FolderItem.value = folder
    }

    fun closeBatchId3Dialog() {
        _batchId3FolderItem.value = null
    }

    private val _showM3uImportDialog = MutableStateFlow(false)
    val showM3uImportDialog: StateFlow<Boolean> = _showM3uImportDialog.asStateFlow()

    private val _showDuplicateFinderDialog = MutableStateFlow(false)
    val showDuplicateFinderDialog: StateFlow<Boolean> = _showDuplicateFinderDialog.asStateFlow()

    // Kütüphane Tarama İlerleme Durumu
    private val _isLibraryScanning = MutableStateFlow(false)
    val isLibraryScanning: StateFlow<Boolean> = _isLibraryScanning.asStateFlow()

    private val _libraryScanStatus = MutableStateFlow("")
    val libraryScanStatus: StateFlow<String> = _libraryScanStatus.asStateFlow()

    private val _libraryScanCount = MutableStateFlow(0)
    val libraryScanCount: StateFlow<Int> = _libraryScanCount.asStateFlow()

    private val _libraryScanCompletedMessage = MutableStateFlow<String?>(null)
    val libraryScanCompletedMessage: StateFlow<String?> = _libraryScanCompletedMessage.asStateFlow()

    fun openDuplicateFinderDialog() {
        _showDuplicateFinderDialog.value = true
    }

    fun closeDuplicateFinderDialog() {
        _showDuplicateFinderDialog.value = false
    }

    private val _availableM3uFiles = MutableStateFlow<List<File>>(emptyList())
    val availableM3uFiles: StateFlow<List<File>> = _availableM3uFiles.asStateFlow()

    fun openM3uImportDialog() {
        scanM3uFiles()
        _showM3uImportDialog.value = true
    }

    fun closeM3uImportDialog() {
        _showM3uImportDialog.value = false
    }

    fun scanM3uFiles() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val files = com.example.otomuzik.util.PlaylistM3uHelper.findAvailableM3uFiles()
            _availableM3uFiles.value = files
        }
    }

    // Ekolayzer (EQ) Durumu
    val equalizerState: StateFlow<com.example.otomuzik.model.EqualizerState> = MusicController.equalizerState

    private val _showEqualizerDialog = MutableStateFlow(false)
    val showEqualizerDialog: StateFlow<Boolean> = _showEqualizerDialog.asStateFlow()

    // Oynatma Hızı
    val playbackSpeed: StateFlow<Float> = MusicController.playbackSpeed
    fun setPlaybackSpeed(speed: Float) = MusicController.setPlaybackSpeed(speed)

    // albüm Kapak Resmi
    private val _currentSongartwork = MutableStateFlow<android.graphics.Bitmap?>(null)
    val currentSongartwork: StateFlow<android.graphics.Bitmap?> = _currentSongartwork.asStateFlow()

    // Tema ve Ayarlar (init bloğundan ve _accentColor'dan önce başlatılmalıdır)
    private val _themeMode = MutableStateFlow(repository.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _themePalette = MutableStateFlow(repository.getThemePalette())
    val themePalette: StateFlow<String> = _themePalette.asStateFlow()

    // Arayüz Modu: "AUTO" (Otomatik Algıla), "CAR" (Araç / Tablet), "PHONE" (Cep Telefonu)
    private val _uiMode = MutableStateFlow(repository.getUiMode())
    val uiMode: StateFlow<String> = _uiMode.asStateFlow()

    private val _isUiModeConfigured = MutableStateFlow(repository.getIsUiModeConfigured())
    val isUiModeConfigured: StateFlow<Boolean> = _isUiModeConfigured.asStateFlow()

    private val _showModeSelectDialog = MutableStateFlow(!repository.getIsUiModeConfigured())
    val showModeSelectDialog: StateFlow<Boolean> = _showModeSelectDialog.asStateFlow()

    private val _autoOpenQueueOnStart = MutableStateFlow(repository.getAutoOpenQueueOnStart())
    val autoOpenQueueOnStart: StateFlow<Boolean> = _autoOpenQueueOnStart.asStateFlow()

    // Taranacak Kütüphane Klasörleri
    private val _includedFolders = MutableStateFlow(repository.getIncludedFolders())
    val includedFolders: StateFlow<Set<String>> = _includedFolders.asStateFlow()

    private val _showFolderSelectionDialog = MutableStateFlow(false)
    val showFolderSelectionDialog: StateFlow<Boolean> = _showFolderSelectionDialog.asStateFlow()

    fun openFolderSelectionDialog() { _showFolderSelectionDialog.value = true }
    fun closeFolderSelectionDialog() { _showFolderSelectionDialog.value = false }

    fun toggleFolderIncluded(path: String, isIncluded: Boolean) {
        val current = _includedFolders.value.toMutableSet()
        if (isIncluded) {
            current.add(path)
        } else {
            current.remove(path)
        }
        repository.setIncludedFolders(current)
        _includedFolders.value = current
        rescanLibrary()
    }

    fun addCustomIncludedFolder(path: String) {
        repository.addIncludedFolder(path)
        _includedFolders.value = repository.getIncludedFolders()
        rescanLibrary()
    }

    fun removeIncludedFolder(path: String) {
        repository.removeIncludedFolder(path)
        _includedFolders.value = repository.getIncludedFolders()
        rescanLibrary()
    }

    fun getPalettePrimaryColor(palette: String): androidx.compose.ui.graphics.Color {
        return when (palette) {
            "SPORT_RED" -> androidx.compose.ui.graphics.Color(0xFFD50000)
            "GREY_BROWN" -> androidx.compose.ui.graphics.Color(0xFF8D6E63)
            "DEEP_BLUE" -> androidx.compose.ui.graphics.Color(0xFF3F51B5)
            else -> androidx.compose.ui.graphics.Color(0xFF00E5FF)
        }
    }

    // albüm kapağından çıkarılan dinamik aksan rengi
    // Kapak yoksa temanın ana rengi kullanılır
    private val _accentColor = MutableStateFlow(getPalettePrimaryColor(repository.getThemePalette()))
    val accentColor: StateFlow<androidx.compose.ui.graphics.Color> = _accentColor.asStateFlow()

    // Detaylı ID3 Künyesi
    private val _currentSongId3 = MutableStateFlow<com.example.otomuzik.util.Id3Metadata?>(null)
    val currentSongId3: StateFlow<com.example.otomuzik.util.Id3Metadata?> = _currentSongId3.asStateFlow()

    // ID3 Detayları Penceresi
    private val _showId3Dialog = MutableStateFlow(false)
    val showId3Dialog: StateFlow<Boolean> = _showId3Dialog.asStateFlow()

    // Şarkı Sözleri (Lyrics)
    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics: StateFlow<String?> = _currentLyrics.asStateFlow()

    private val _lyricsData = MutableStateFlow<LyricsData?>(null)
    val lyricsData: StateFlow<LyricsData?> = _lyricsData.asStateFlow()

    private val _isSearchingLyrics = MutableStateFlow(false)
    val isSearchingLyrics: StateFlow<Boolean> = _isSearchingLyrics.asStateFlow()

    private val _lyricsStatusMessage = MutableStateFlow<String?>(null)
    val lyricsStatusMessage: StateFlow<String?> = _lyricsStatusMessage.asStateFlow()

    private val _showLyricsDialog = MutableStateFlow(false)
    val showLyricsDialog: StateFlow<Boolean> = _showLyricsDialog.asStateFlow()

    private val _showCoverLyrics = MutableStateFlow(repository.getShowCoverLyrics())
    val showCoverLyrics: StateFlow<Boolean> = _showCoverLyrics.asStateFlow()

    private val _lyricsFontSize = MutableStateFlow(repository.getLyricsFontSize())
    val lyricsFontSize: StateFlow<String> = _lyricsFontSize.asStateFlow()

    private val _autoSaveId3Lyrics = MutableStateFlow(repository.getAutoSaveId3Lyrics())
    val autoSaveId3Lyrics: StateFlow<Boolean> = _autoSaveId3Lyrics.asStateFlow()

    // aktif çÇalma sırası — MusicController üzerinden
    val currentQueue: StateFlow<List<Song>> = MusicController.currentQueue
    val currentQueueIndex: StateFlow<Int> = MusicController.currentQueueIndex

    // Queue Dialog & Panel
    private val _showQueueDialog = MutableStateFlow(false)
    val showQueueDialog: StateFlow<Boolean> = _showQueueDialog.asStateFlow()

    private val _isQueueVisible = MutableStateFlow(false)
    val isQueueVisible: StateFlow<Boolean> = _isQueueVisible.asStateFlow()

    // Sleep Timer
    val sleepTimerLeftMs: StateFlow<Long?> = MusicController.sleepTimerLeftMs
    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    // Gelişmiş Ayarlar (init bloğundan ve refreshAll()'dan önce başlatılmalıdır)
    private val _autoPlayOnBluetooth = MutableStateFlow(repository.getAutoPlayOnBluetooth())
    val autoPlayOnBluetooth: StateFlow<Boolean> = _autoPlayOnBluetooth.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(repository.getKeepScreenOn())
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _crossfadeDuration = MutableStateFlow(repository.getCrossfadeDuration())
    val crossfadeDuration: StateFlow<Int> = _crossfadeDuration.asStateFlow()

    private val _autoPlayOnStart = MutableStateFlow(repository.getAutoPlayOnStart())
    val autoPlayOnStart: StateFlow<Boolean> = _autoPlayOnStart.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(repository.getGaplessPlayback())
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _defaultStartTab = MutableStateFlow(repository.getDefaultStartTab())
    val defaultStartTab: StateFlow<String> = _defaultStartTab.asStateFlow()

    private val _showSmartPlaylists = MutableStateFlow(repository.getShowSmartPlaylists())
    val showSmartPlaylists: StateFlow<Boolean> = _showSmartPlaylists.asStateFlow()

    private val _showBlurredBackground = MutableStateFlow(repository.getShowBlurredBackground())
    val showBlurredBackground: StateFlow<Boolean> = _showBlurredBackground.asStateFlow()

    private val _showWaveform = MutableStateFlow(repository.getShowWaveform())
    val showWaveform: StateFlow<Boolean> = _showWaveform.asStateFlow()

    private val _isalbumartCropped = MutableStateFlow(repository.getIsAlbumArtCropped())
    val isalbumartCropped: StateFlow<Boolean> = _isalbumartCropped.asStateFlow()

    private val _stopOnTaskRemoved = MutableStateFlow(repository.getStopOnTaskRemoved())
    val stopOnTaskRemoved: StateFlow<Boolean> = _stopOnTaskRemoved.asStateFlow()

    private val _startOnBoot = MutableStateFlow(repository.getStartOnBoot())
    val startOnBoot: StateFlow<Boolean> = _startOnBoot.asStateFlow()

    private val _hiddenSongsCount = MutableStateFlow(repository.getHiddenSongsCount())
    val hiddenSongsCount: StateFlow<Int> = _hiddenSongsCount.asStateFlow()


    private val _swipeSensitivity = MutableStateFlow(repository.getSwipeSensitivity())
    val swipeSensitivity: StateFlow<String> = _swipeSensitivity.asStateFlow()

    fun setSwipeSensitivity(sensitivity: String) {
        _swipeSensitivity.value = sensitivity
        repository.setSwipeSensitivity(sensitivity)
    }

    // Kütüphane İstatistikleri (Ayarlar ekranı için)
    val libraryStats: StateFlow<com.example.otomuzik.model.LibraryStats> = combine(_allSongs, _Playlists) { songs, playlists ->
        if (songs.isEmpty()) {
            com.example.otomuzik.model.LibraryStats(totalPlaylists = playlists.size)
        } else {
            val folders = songs.mapNotNull {
                try { java.io.File(it.path).parent } catch (e: Exception) { null }
            }.distinct().size

            val artists = songs.map { it.artist.trim() }
                .filter { it.isNotEmpty() && !it.equals("<unknown>", true) && !it.equals("unknown", true) && !it.equals("bilinmeyen sanatçı", true) }
                .distinct().size

            val albums = songs.map { it.album.trim() }
                .filter { it.isNotEmpty() && !it.equals("<unknown>", true) && !it.equals("unknown", true) && !it.equals("bilinmeyen albüm", true) }
                .distinct().size

            val totalDurationMs = songs.sumOf { it.durationMs }
            val hours = totalDurationMs / 3600000L
            val minutes = (totalDurationMs % 3600000L) / 60000L
            val durationStr = if (hours > 0) "${hours} sa ${minutes} dk" else "${minutes} dk"

            val totalBytes = songs.sumOf { 
                try { java.io.File(it.path).length() } catch (e: Exception) { 0L }
            }
            val sizeMb = totalBytes.toDouble() / (1024.0 * 1024.0)
            val sizeStr = if (sizeMb >= 1024.0) {
                String.format(java.util.Locale.US, "%.1f GB", sizeMb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.0f MB", sizeMb)
            }

            com.example.otomuzik.model.LibraryStats(
                totalSongs = songs.size,
                totalFolders = folders,
                uniqueArtists = artists,
                uniqueAlbums = albums,
                totalPlaylists = playlists.size,
                totalDurationFormatted = durationStr,
                totalSizeFormatted = sizeStr
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.otomuzik.model.LibraryStats())

    private var autoOpenQueueJob: kotlinx.coroutines.Job? = null
    private var hasUserInteractedWithApp: Boolean = false

    fun scheduleAutoOpenQueue() {
        if (!_autoOpenQueueOnStart.value) return
        autoOpenQueueJob?.cancel()
        autoOpenQueueJob = viewModelScope.launch {
            kotlinx.coroutines.delay(4000) // 4 saniye sonra
            if (!hasUserInteractedWithApp && !_isQueueVisible.value && _selectedPlaylist.value == null && !_showSettingsDialog.value && !_showEqualizerDialog.value) {
                if (MusicController.playerState.value.currentSong != null || MusicController.currentQueue.value.isNotEmpty()) {
                    _isQueueVisible.value = true
                }
            }
        }
    }

    fun onUserNavigated() {
        hasUserInteractedWithApp = true
        autoOpenQueueJob?.cancel()
    }

    init {
        val defaultTabStr = repository.getDefaultStartTab()
        _activeTab.value = try {
            TabType.valueOf(defaultTabStr)
        } catch (e: Exception) {
            TabType.HOME
        }
        
        _favoritePaths.value = repository.getFavoritePaths()
        refreshAll()
        loadPlaylists()

        // Çalan şarkı DEĞİŞTİĞİNDE albüm kapağını, ID3 etiketini ve şarkı sözlerini yükle
        var lastLoadedSongPath: String? = null
        var wasPlaying: Boolean = false
        viewModelScope.launch {
            playerState.collect { state ->
                val song = state.currentSong
                val currentPath = song?.path
                val isPlaying = state.isPlaying

                if (isPlaying && !wasPlaying) {
                    scheduleAutoOpenQueue()
                }
                wasPlaying = isPlaying

                // Eğer çalan şarkı değişmediyse (yalnızca çalma konumu/saniyesi aktıysa veya duraklatıldıysa)
                // metadata, kapak ve şarkı sözlerini TEKRAR YÜKLEME (ekrandaki sözleri sıfırlama)
                if (currentPath == lastLoadedSongPath) {
                    return@collect
                }
                lastLoadedSongPath = currentPath

                if (song != null) {
                    val artwork = com.example.otomuzik.util.AlbumArtHelper.getArtwork(
                        context = getApplication<android.app.Application>(),
                        path = song.path,
                        albumId = song.albumId,
                        uriString = song.uriString
                    )
                    _currentSongartwork.value = artwork

                    // Kapaktan baskın rengi çıkar (DYNAMIC ise) veya seçilen paleti kullan
                    if (_themePalette.value == "DYNAMIC") {
                        _accentColor.value = com.example.otomuzik.theme.DynamicColorManager
                            .extractDominantColor(artwork)
                    } else {
                        _accentColor.value = getPalettePrimaryColor(_themePalette.value)
                    }

                    _currentSongId3.value = com.example.otomuzik.util.Id3Helper.parseSong(
                        context = getApplication<android.app.Application>(),
                        filePath = song.path,
                        uriString = song.uriString
                )

                    // Şarkı sözlerini yerelden yükle
                    val lyrics = com.example.otomuzik.util.LyricsHelper.getLyricsForSong(getApplication<android.app.Application>(), song.path)
                    _lyricsData.value = lyrics
                    _currentLyrics.value = lyrics?.rawLyrics
                    _lyricsStatusMessage.value = null

                    // Eğer şarkı sözü penceresi veya kapakta sözler açıksa ve yerelde söz yoksa, yeni şarkı için internetten otomatik ara
                    if (lyrics == null && (_showLyricsDialog.value || _showCoverLyrics.value)) {
                        searchLyricsOnline()
                    }
                } else {
                    _currentSongartwork.value = null
                    _currentSongId3.value = null
                    _lyricsData.value = null
                    _currentLyrics.value = null
                    _lyricsStatusMessage.value = null
                    _accentColor.value = getPalettePrimaryColor(_themePalette.value)
                }
            }
        }
    }

    fun openEqualizerDialog() { _showEqualizerDialog.value = true }
    fun closeEqualizerDialog() { _showEqualizerDialog.value = false }
    fun setEqBandLevel(bandIndex: Short, levelMb: Short) = MusicController.setBandLevel(bandIndex, levelMb)
    fun setEqPreset(presetName: String) = MusicController.setPreset(presetName)
    fun setEqBassBoost(strength: Short) = MusicController.setBassBoost(strength)
    fun toggleEqualizer(enabled: Boolean) = MusicController.toggleEqualizer(enabled)

    fun openId3Dialog() { _showId3Dialog.value = true }
    fun closeId3Dialog() { _showId3Dialog.value = false }

    fun saveId3Tags(
        song: Song,
        newFileName: String,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: String,
        artworkBytes: ByteArray? = null,
        lyrics: String? = null,
        deleteArtwork: Boolean = false
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val app = getApplication<android.app.Application>()
            var effectivePath = song.path
            var effectiveUriString = song.uriString

            // 1. Dosya adı değiştirme mantığı
            val originalFile = java.io.File(song.path)
            val extension = originalFile.extension.ifEmpty { "mp3" }
            var cleanName = newFileName.trim()
            if (cleanName.endsWith(".$extension", ignoreCase = true)) {
                cleanName = cleanName.substringBeforeLast(".$extension").trim()
            }
            // FAT / Linux dosya sistemleri için geçersiz karakterleri temizle
            cleanName = cleanName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()

            if (cleanName.isNotBlank() && cleanName != originalFile.nameWithoutExtension) {
                val parentDir = originalFile.parentFile ?: java.io.File(song.folderPath)
                val targetFile = java.io.File(parentDir, "$cleanName.$extension")

                if (targetFile.exists() && targetFile.absolutePath != originalFile.absolutePath) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(app, "⚠️ '$cleanName.$extension' isimli dosya zaten var!", android.widget.Toast.LENGTH_LONG).show()
                    }
                } else {
                    val renamed = originalFile.renameTo(targetFile)
                    if (renamed) {
                        effectivePath = targetFile.absolutePath
                        effectiveUriString = android.net.Uri.fromFile(targetFile).toString()

                        // Varsa şarkıyla aynı isimdeki .lrc ve .txt söz dosyalarını da yeniden adlandır
                        try {
                            val oldLrc = java.io.File(parentDir, "${originalFile.nameWithoutExtension}.lrc")
                            if (oldLrc.exists()) {
                                oldLrc.renameTo(java.io.File(parentDir, "$cleanName.lrc"))
                            }
                            val oldTxt = java.io.File(parentDir, "${originalFile.nameWithoutExtension}.txt")
                            if (oldTxt.exists()) {
                                oldTxt.renameTo(java.io.File(parentDir, "$cleanName.txt"))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        android.media.MediaScannerConnection.scanFile(
                            app,
                            arrayOf(originalFile.absolutePath, targetFile.absolutePath),
                            null,
                            null
                        )
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(app, "⚠️ Dosya yeniden adlandırılamadı (Yazma izni kısıtlı olabilir)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // 2. ID3 Etiketlerini yeni veya mevcut dosyaya yaz
            val success = com.example.otomuzik.util.Id3Helper.writeTag(
                context = app,
                path = effectivePath,
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                year = year,
                artworkBytes = artworkBytes,
                lyrics = lyrics,
                deleteArtwork = deleteArtwork
            )

            // 3. Veritabanını, favorileri ve çalma listelerini güncelle
            repository.updateSongDetails(
                oldPath = song.path,
                newPath = effectivePath,
                newTitle = title,
                newArtist = artist,
                newAlbum = album,
                newGenre = genre,
                newYear = year
            )

            // 4. Eğer şu an çalan şarkı bu şarkıysa, çalma kuyruğundaki Song nesnesini de güncelle
            if (MusicController.playerState.value.currentSong?.path == song.path) {
                val updatedSong = song.copy(
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    year = year,
                    path = effectivePath,
                    uriString = effectiveUriString
                )
                MusicController.updateCurrentSongInfo(updatedSong)
            }

            if (success || effectivePath != song.path) {
                com.example.otomuzik.util.AlbumArtHelper.clearCache()
                val updatedArtwork = com.example.otomuzik.util.AlbumArtHelper.getArtwork(
                    context = app,
                    path = effectivePath,
                    albumId = song.albumId,
                    uriString = effectiveUriString
                )
                _currentSongartwork.value = updatedArtwork
                _accentColor.value = com.example.otomuzik.theme.DynamicColorManager.extractDominantColor(updatedArtwork)
                _currentSongId3.value = com.example.otomuzik.util.Id3Helper.parseSong(
                    context = app,
                    filePath = effectivePath,
                    uriString = effectiveUriString
                )
                if (lyrics != null) {
                    val cleanLyrics = com.example.otomuzik.util.TurkishStringFixer.fix(lyrics.trim())
                    if (cleanLyrics.isBlank()) {
                        _lyricsData.value = null
                        _currentLyrics.value = null
                    } else {
                        val parsedLines = com.example.otomuzik.util.LyricsHelper.parseLyrics(cleanLyrics)
                        val isSynced = parsedLines.any { it.timestampMs >= 0 }
                        val newLyricsData = com.example.otomuzik.model.LyricsData(
                            songPath = effectivePath,
                            rawLyrics = cleanLyrics,
                            lines = parsedLines,
                            isSynced = isSynced,
                            source = if (isSynced) com.example.otomuzik.model.LyricsSource.LOCAL_LRC else com.example.otomuzik.model.LyricsSource.LOCAL_TXT
                        )
                        _lyricsData.value = newLyricsData
                        _currentLyrics.value = cleanLyrics
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val msg = if (effectivePath != song.path) "✓ Dosya adı ve etiketler kaydedildi: $cleanName" else "✓ ID3 etiketleri kaydedildi"
                    android.widget.Toast.makeText(app, msg, android.widget.Toast.LENGTH_SHORT).show()
                }

                refreshAll()
            }
        }
    }

    fun deleteSong(song: Song, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            val success = repository.deleteSong(song)
            if (success) {
                if (MusicController.playerState.value.currentSong?.path == song.path) {
                    MusicController.playNext()
                }
                val updated = _allSongs.value.filter { it.path != song.path }
                _allSongs.value = updated
                MusicController.syncQueueWithLibrary(updated)

                val currentPath = _currentFolderPath.value
                if (!currentPath.isNullOrEmpty()) {
                    _currentFolderItems.value = repository.getFolderContents(currentPath)
                }
                loadPlaylists()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "🗑️ '${song.title}' silindi", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "⚠️ Dosya silinemedi (Yazma izni kısıtlı olabilir)", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            onComplete?.invoke(success)
        }
    }

    fun deleteSongs(songs: List<Song>, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            var deletedCount = 0
            val deletedPaths = mutableSetOf<String>()
            val currentPlayingPath = MusicController.playerState.value.currentSong?.path

            for (song in songs) {
                val success = repository.deleteSong(song)
                if (success) {
                    deletedCount++
                    deletedPaths.add(song.path)
                }
            }

            if (deletedCount > 0) {
                if (currentPlayingPath != null && deletedPaths.contains(currentPlayingPath)) {
                    MusicController.playNext()
                }
                val updated = _allSongs.value.filter { it.path !in deletedPaths }
                _allSongs.value = updated
                MusicController.syncQueueWithLibrary(updated)

                val currentPath = _currentFolderPath.value
                if (!currentPath.isNullOrEmpty()) {
                    _currentFolderItems.value = repository.getFolderContents(currentPath)
                }
                loadPlaylists()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "🗑️ $deletedCount adet mükerrer kopya silindi", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "⚠️ Dosyalar silinemedi (Yazma izni kısıtlı olabilir)", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            onComplete?.invoke(deletedCount)
        }
    }

    fun renameSong(song: Song, newNameWithoutExt: String, onComplete: ((Boolean, Song?) -> Unit)? = null) {
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            val updatedSong = repository.renameSongFile(song, newNameWithoutExt)
            if (updatedSong != null) {
                val updatedList = _allSongs.value.map { if (it.path == song.path) updatedSong else it }
                _allSongs.value = updatedList
                MusicController.syncQueueWithLibrary(updatedList)

                if (MusicController.playerState.value.currentSong?.path == song.path) {
                    MusicController.updateCurrentSongInfo(updatedSong)
                }

                val currentPath = _currentFolderPath.value
                if (!currentPath.isNullOrEmpty()) {
                    _currentFolderItems.value = repository.getFolderContents(currentPath)
                }
                loadPlaylists()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "✓ Dosya yeniden adlandırıldı: ${updatedSong.title}", android.widget.Toast.LENGTH_SHORT).show()
                }
                onComplete?.invoke(true, updatedSong)
            } else {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "⚠️ Dosya adı değiştirilemedi (Aynı isimde dosya var veya yazma izni kısıtlı)", android.widget.Toast.LENGTH_SHORT).show()
                }
                onComplete?.invoke(false, null)
            }
        }
    }

    fun openLyricsDialog() {
        _showLyricsDialog.value = true
        // Şarkı varsa ve henüz söz yüklenmemişse internetten otomatik aramayı başlat
        if (_lyricsData.value == null) {
            val song = playerState.value.currentSong
            if (song != null) {
                searchLyricsOnline()
            }
        }
    }

    fun closeLyricsDialog() {
        _showLyricsDialog.value = false
    }

    fun toggleCoverLyrics() {
        val next = !_showCoverLyrics.value
        _showCoverLyrics.value = next
        repository.setShowCoverLyrics(next)
        if (next && _lyricsData.value == null && playerState.value.currentSong != null) {
            searchLyricsOnline()
        }
    }

    fun searchLyricsOnline(customArtist: String? = null, customTitle: String? = null) {
        val song = playerState.value.currentSong ?: return
        if (_isSearchingLyrics.value) return

        viewModelScope.launch {
            _isSearchingLyrics.value = true
            _lyricsStatusMessage.value = "İnternette şarkı sözleri aranıyor (LRCLIB & lyrics.ovh)..."

            val id3 = _currentSongId3.value
            val rawArtist = customArtist?.takeIf { it.isNotBlank() }
                ?: id3?.artist?.takeIf { it.isNotBlank() }
                ?: song.artist
            val rawTitle = customTitle?.takeIf { it.isNotBlank() }
                ?: id3?.title?.takeIf { it.isNotBlank() }
                ?: song.title
            val album = id3?.album?.takeIf { it.isNotBlank() } ?: song.album
            val durationSec = (song.durationMs / 1000L).toInt()

            val result = com.example.otomuzik.util.LyricsHelper.fetchOnlineLyrics(
                artist = rawArtist,
                title = rawTitle,
                album = album,
                durationSec = durationSec,
                songPath = song.path
            )

            if (result != null) {
                if (playerState.value.currentSong?.path == song.path) {
                    _lyricsData.value = result
                    _currentLyrics.value = result.rawLyrics
                    val sourceName = when (result.source) {
                        LyricsSource.ONLINE_LRCLIB -> "LRCLIB"
                        LyricsSource.ONLINE_OVH -> "lyrics.ovh"
                        else -> "İnternet"
                    }
                    _lyricsStatusMessage.value = if (result.isSynced) {
                        "✓ $sourceName üzerinden senkronize sözler bulundu!"
                    } else {
                        "✓ $sourceName üzerinden sözler bulundu (düz metin)."
                    }
                }
                // Otomatik olarak app cache, disk ve ID3'e kaydet
                val app = getApplication<android.app.Application>()
                com.example.otomuzik.util.LyricsHelper.saveLyricsToAppCache(app, song.path, result.rawLyrics)
                com.example.otomuzik.util.LyricsHelper.saveLyricsToFile(song.path, result)
                if (repository.getAutoSaveId3Lyrics()) {
                    com.example.otomuzik.util.LyricsHelper.saveEmbeddedLyrics(song.path, result.rawLyrics)
                }
            } else {
                if (playerState.value.currentSong?.path == song.path) {
                    _lyricsStatusMessage.value = "Şarkı sözü bulunamadı. 'Google'da Ara' butonunu kullanabilirsiniz."
                }
            }

            _isSearchingLyrics.value = false
        }
    }

    fun pasteLyricsFromClipboard(clipboardText: String) {
        if (clipboardText.isBlank()) {
            _lyricsStatusMessage.value = "Pano boş, kopyalanmış söz bulunamadı."
            return
        }

        val song = playerState.value.currentSong ?: return
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            val fixedText = com.example.otomuzik.util.TurkishStringFixer.fix(clipboardText.trim())
            val parsedLines = com.example.otomuzik.util.LyricsHelper.parseLyrics(fixedText)
            val isSynced = parsedLines.any { it.timestampMs >= 0 }
            val newLyricsData = com.example.otomuzik.model.LyricsData(
                songPath = song.path,
                rawLyrics = fixedText,
                lines = parsedLines,
                isSynced = isSynced,
                source = LyricsSource.MANUAL_CLIPBOARD
            )

            _lyricsData.value = newLyricsData
            _currentLyrics.value = fixedText

            // Uygulama içi önbelleğe her koşulda garantili kaydet
            com.example.otomuzik.util.LyricsHelper.saveLyricsToAppCache(app, song.path, fixedText)

            // Diske ve ID3 etiketine otomatik kaydet
            val fileSaved = com.example.otomuzik.util.LyricsHelper.saveLyricsToFile(song.path, newLyricsData)
            val id3Saved = if (repository.getAutoSaveId3Lyrics()) {
                com.example.otomuzik.util.LyricsHelper.saveEmbeddedLyrics(song.path, fixedText)
            } else false

            _lyricsStatusMessage.value = "✓ Panodan yapıştırıldı ve kaydedildi (${if (isSynced) "Senkronize LRC" else "Düz Metin"})."
        }
    }

    fun saveLyricsToDisk(onResult: ((Boolean, String) -> Unit)? = null) {
        val song = playerState.value.currentSong ?: run {
            onResult?.invoke(false, "Aktif çalan şarkı bulunamadı")
            return
        }
        val data = _lyricsData.value ?: run {
            onResult?.invoke(false, "Kaydedilecek söz bulunamadı")
            return
        }

        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            // Uygulama içi önbelleğe her koşulda garantili kaydet
            com.example.otomuzik.util.LyricsHelper.saveLyricsToAppCache(app, song.path, data.rawLyrics)

            val fileSaved = com.example.otomuzik.util.LyricsHelper.saveLyricsToFile(song.path, data)
            val id3Saved = com.example.otomuzik.util.LyricsHelper.saveEmbeddedLyrics(song.path, data.rawLyrics)

            val msg = when {
                fileSaved && id3Saved -> "✓ Sözler şarkı klasörüne (${if (data.isSynced) ".lrc" else ".txt"}) ve ID3 etiketine kaydedildi"
                fileSaved -> "✓ Sözler şarkı klasörüne kaydedildi (${if (data.isSynced) ".lrc" else ".txt"})"
                id3Saved -> "✓ Sözler MP3 ID3 etiketine kaydedildi"
                else -> "✓ Sözler uygulama hafızasına kaydedildi"
            }
            _lyricsStatusMessage.value = msg
            onResult?.invoke(true, msg)
        }
    }

    fun seekToLyricPosition(positionMs: Long) {
        MusicController.seekTo(positionMs)
    }

    fun openQueue() {
        onUserNavigated()
        _activeTab.value = TabType.QUEUE
        _isQueueVisible.value = true
    }
    fun closeQueue() {
        onUserNavigated()
        _isQueueVisible.value = false
        if (_activeTab.value == TabType.QUEUE) {
            _activeTab.value = TabType.HOME
        }
    }
    fun openQueueDialog() { openQueue() }
    fun closeQueueDialog() { closeQueue() }
    fun playAtQueueIndex(index: Int) = MusicController.playAtIndex(index)

    fun openSleepTimerDialog() { _showSleepTimerDialog.value = true }
    fun closeSleepTimerDialog() { _showSleepTimerDialog.value = false }
    
    fun setSleepTimer(minutes: Int) {
        MusicController.setSleepTimer(minutes)
        closeSleepTimerDialog()
    }
    fun cancelSleepTimer() {
        MusicController.cancelSleepTimer()
        closeSleepTimerDialog()
    }
    
    fun setLoudnessEnhancerEnabled(enabled: Boolean) = MusicController.setLoudnessEnhancerEnabled(enabled)

    fun setTab(tab: TabType) {
        onUserNavigated()
        if (tab == TabType.QUEUE) {
            _isQueueVisible.value = true
        } else {
            _isQueueVisible.value = false
        }
        _activeTab.value = tab
        if (tab == TabType.PLAYLISTS) {
            loadPlaylists()
        }
        if (tab == TabType.FOLDERS) {
            loadFolderRootsIfNeeded()
        }
    }

    /**
     * Klasör dizinlerini yalnızca kullanıcı 'Klasörler' sekmesine girdiğinde yükler.
     * Uygulamanın ilk açılışında disk/USB belleği taramasını engeller.
     */
    fun loadFolderRootsIfNeeded() {
        if (_storageRoots.value.isNotEmpty()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val roots = repository.getStorageRoots()
                _storageRoots.value = roots

                val currentPath = _currentFolderPath.value ?: repository.getLastFolder()
                if (!currentPath.isNullOrEmpty()) {
                    _currentFolderPath.value = currentPath
                    _currentFolderItems.value = repository.getFolderContents(currentPath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private var isRefreshing = false

    /**
     * Açılışta ASLA dosya/disk taraması yapmaz.
     * Bilgileri doğrudan SQLite veritabanı kütüphanesinden çeker (0ms disk beklemesi).
     * Dosya taraması yalnızca kullanıcı "Kütüphaneyi Yeniden Tara" dediğinde veya kütüphane ilk kurulumda tamamen boşsa yapılır.
     */
    fun refreshAll(forceRefresh: Boolean = false) {
        if (isRefreshing && !forceRefresh) return
        isRefreshing = true
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _keepScreenOn.value = repository.getKeepScreenOn()
                
                // 1. Doğrudan SQLite kütüphanesinden oku - ASLA dosya tarama yapma
                val songs = if (forceRefresh) {
                    repository.scanAndIndexLibrary()
                } else {
                    val cached = repository.getSongsFromCacheOnly()
                    if (cached.isEmpty()) {
                        // Kütüphane tamamen boşsa (ilk kurulumda tek seferlik) tara
                        repository.scanAndIndexLibrary()
                    } else {
                        cached
                    }
                }
                _allSongs.value = songs

                // Önceden çalınan parça varsa listeye yükle ve ana göstergeyi anında hazırla
                if (MusicController.playerState.value.currentSong == null && songs.isNotEmpty()) {
                    val lastPath = repository.getLastPlayedPath()
                    val lastPos = repository.getLastPlayedPosition()
                    val lastIndex = if (lastPath != null) {
                        songs.indexOfFirst { it.path == lastPath }.takeIf { it != -1 } ?: 0
                    } else 0

                    val autoPlay = repository.getAutoPlayOnStart()
                    MusicController.setQueue(songs, lastIndex, autoPlay = autoPlay)
                    if (lastPos > 0) {
                        MusicController.seekTo(lastPos)
                    }
                }
                if (songs.isNotEmpty()) {
                    scheduleAutoOpenQueue()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                isRefreshing = false
            }
        }
    }

    /**
     * Kütüphaneyi sıfırdan tüm depolama alanlarında tarar ve SQLite veritabanını günceller.
     * Taranan dizin ve bulunan şarkı sayısını anlık olarak yayar.
     */
    fun rescanLibrary() {
        if (_isLibraryScanning.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _isLibraryScanning.value = true
            _libraryScanCompletedMessage.value = null
            _libraryScanStatus.value = "Tarama başlatılıyor..."
            _libraryScanCount.value = 0
            try {
                val updatedSongs = repository.scanAndIndexLibrary { currentDir, count ->
                    _libraryScanStatus.value = currentDir
                    _libraryScanCount.value = count
                }
                _allSongs.value = updatedSongs
                val roots = repository.getStorageRoots()
                _storageRoots.value = roots
                
                val currentPath = _currentFolderPath.value ?: repository.getLastFolder()
                if (!currentPath.isNullOrEmpty()) {
                    _currentFolderPath.value = currentPath
                    _currentFolderItems.value = repository.getFolderContents(currentPath)
                }
                loadPlaylists()
                // Aktif çalma sırasını güncel kütüphane ile senkronize et (Silinenleri çıkar, yenileri ekle, sayıyı güncelle)
                MusicController.syncQueueWithLibrary(updatedSongs)
                _libraryScanCompletedMessage.value = "✅ Tarama tamamlandı: ${updatedSongs.size} şarkı güncellendi."
            } catch (e: Exception) {
                e.printStackTrace()
                _libraryScanCompletedMessage.value = "❌ Tarama sırasında hata: ${e.message}"
            } finally {
                _isLoading.value = false
                _isLibraryScanning.value = false
            }
        }
    }

    fun openFolder(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _currentFolderPath.value = path
                repository.saveLastFolder(path) // Son Klasörü kaydet
                _currentFolderItems.value = repository.getFolderContents(path)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun playallSongs() {
        val songs = _allSongs.value
        if (songs.isNotEmpty()) {
            MusicController.setQueue(songs, 0, autoPlay = true)
            _activeTab.value = TabType.HOME
            _isQueueVisible.value = false
        }
    }

    fun goBackFolder(): Boolean {
        val current = _currentFolderPath.value
        if (current == null) {
            if (_activeTab.value != TabType.HOME) {
                _activeTab.value = TabType.HOME
                return true
            }
            return false
        }

        val roots = _storageRoots.value
        fun normalizePath(p: String): String = java.io.File(p).absolutePath.trimEnd('/', '\\')

        val currentNorm = normalizePath(current)
        val internalRoot = android.os.Environment.getExternalStorageDirectory()?.absolutePath
        val isAtRoot = roots.any { normalizePath(it.path).equals(currentNorm, ignoreCase = true) } ||
                (internalRoot != null && normalizePath(internalRoot).equals(currentNorm, ignoreCase = true)) ||
                currentNorm == "/storage" ||
                currentNorm == "/storage/emulated" ||
                currentNorm.isEmpty() ||
                currentNorm == "/"

        if (isAtRoot) {
            // Zaten bir depolama kök dizinindeyiz (örn. USB ana dizini veya Dahili Hafıza).
            // Geri basılınca ana ekrana dönülür.
            _currentFolderPath.value = null
            _currentFolderItems.value = emptyList()
            repository.saveLastFolder("")
            _activeTab.value = TabType.HOME
            return true
        }

        // Bir alt klasördeyiz; bir üst klasörün (parent) yolunu al
        val parentFile = java.io.File(current).parentFile
        if (parentFile == null || !parentFile.exists() || !parentFile.canRead()) {
            _currentFolderPath.value = null
            _currentFolderItems.value = emptyList()
            repository.saveLastFolder("")
            _activeTab.value = TabType.HOME
            return true
        }

        val parentNorm = normalizePath(parentFile.absolutePath)
        if (parentNorm == "/storage" || parentNorm == "/storage/emulated" || parentNorm == "/" || parentNorm.isEmpty()) {
            _currentFolderPath.value = null
            _currentFolderItems.value = emptyList()
            repository.saveLastFolder("")
            _activeTab.value = TabType.HOME
            return true
        }

        // Kademeli olarak bir üst klasöre dön
        openFolder(parentFile.absolutePath)
        return true
    }

    fun playEntireFolder(folderPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val songs = repository.getAllSongsInDirectory(folderPath)
                if (songs.isNotEmpty()) {
                    MusicController.setQueue(songs, 0, autoPlay = true)
                    _isQueueVisible.value = false
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playSongFromList(song: Song, List: List<Song>) {
        val index = List.indexOfFirst { it.path == song.path || (it.id != 0L && it.id == song.id) }
        val startIdx = if (index != -1) index else 0
        MusicController.setQueue(List, startIdx, autoPlay = true)
        _isQueueVisible.value = false
    }

    fun playFavorites() {
        val songs = favoriteSongs.value
        if (songs.isNotEmpty()) {
            MusicController.setQueue(songs, 0, autoPlay = true)
            _isQueueVisible.value = false
        }
    }

    fun toggleFavorite(song: Song) {
        val isFav = repository.toggleFavorite(song.path)
        val updated = _favoritePaths.value.toMutableSet()
        if (isFav) updated.add(song.path) else updated.remove(song.path)
        _favoritePaths.value = updated
    }

    // --- Klasr ve Şarkı Context Menü İşlemileri ---
    fun openFolderContext(folderItem: FolderItem) {
        _folderContextItem.value = folderItem
    }

    fun closeFolderContext() {
        _folderContextItem.value = null
    }

    fun openSongContext(song: Song) {
        _songContextItem.value = song
    }

    fun closeSongContext() {
        _songContextItem.value = null
    }

    fun AddFolderToQueue(folderPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val songs = repository.getAllSongsInDirectory(folderPath)
                if (songs.isNotEmpty()) {
                    MusicController.addToQueue(songs)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun AddSongToQueue(song: Song) {
        MusicController.addToQueue(listOf(song))
    }

    /**
     * Arama sonuçlarından seçilen parçayı mevcut çalma sırasını bozmadan
     * sıradaki parça olarak ekler.
     */
    fun insertAsNextSong(song: Song, playImmediately: Boolean = false) {
        MusicController.insertAsNextSong(song, playImmediately)
        val app = getApplication<android.app.Application>()
        val msg = if (playImmediately) {
            "▶ Çalınıyor: ${song.title}"
        } else {
            "✓ Sıradaki şarkı olarak eklendi: ${song.title}"
        }
        android.widget.Toast.makeText(app, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    // --- ÇÇalma Listesi (Playlist) Fonksiyonları ---
    fun loadPlaylists() {
        viewModelScope.launch {
            val showSmart = repository.getShowSmartPlaylists()
            val smartLists = if (showSmart) repository.getSmartPlaylists() else emptyList()
            val dbLists = repository.getPlaylists()
            _Playlists.value = smartLists + dbLists
        }
    }

    fun openCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = true
    }

    fun closeCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = false
    }

    fun createPlaylist(name: String, songsToadd: List<Song>? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val plId = repository.createPlaylist(name)
            if (plId != -1L && !songsToadd.isNullOrEmpty()) {
                repository.addSongsToPlaylist(plId, songsToadd)
            }
            loadPlaylists()
            closeCreatePlaylistDialog()
            closeaddToPlaylistDialog()
        }
    }

    fun deletePlaylist(Playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(Playlist.id)
            if (_selectedPlaylist.value?.id == Playlist.id) {
                closePlaylist()
            }
            loadPlaylists()
        }
    }

    fun openPlaylist(Playlist: Playlist) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _selectedPlaylist.value = Playlist
                _selectedPlaylistSongs.value = repository.getSongsForPlaylist(Playlist.id)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun closePlaylist() {
        _selectedPlaylist.value = null
        _selectedPlaylistSongs.value = emptyList()
    }

    fun playPlaylist(Playlist: Playlist) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val songs = repository.getSongsForPlaylist(Playlist.id)
                if (songs.isNotEmpty()) {
                    MusicController.setQueue(songs, 0, autoPlay = true)
                    _isQueueVisible.value = false
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeSongFromPlaylist(PlaylistId: Long, song: Song) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(PlaylistId, song.path)
            _selectedPlaylistSongs.value = repository.getSongsForPlaylist(PlaylistId)
            loadPlaylists()
        }
    }

    fun openaddToPlaylistDialogForFolder(folderPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val songs = repository.getAllSongsInDirectory(folderPath)
                if (songs.isNotEmpty()) {
                    _songsToaddToPlaylist.value = songs
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openaddToPlaylistDialogForSong(song: Song) {
        _songsToaddToPlaylist.value = listOf(song)
    }

    fun closeaddToPlaylistDialog() {
        _songsToaddToPlaylist.value = null
    }

    fun addSongsToPlaylist(PlaylistId: Long, songs: List<Song>) {
        viewModelScope.launch {
            repository.addSongsToPlaylist(PlaylistId, songs)
            loadPlaylists()
            if (_selectedPlaylist.value?.id == PlaylistId) {
                _selectedPlaylistSongs.value = repository.getSongsForPlaylist(PlaylistId)
            }
            closeaddToPlaylistDialog()
        }
    }

    fun exportCurrentPlaylistM3u(playlist: Playlist) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val app = getApplication<Application>()
            val songs = repository.getSongsForPlaylist(playlist.id)
            if (songs.isEmpty()) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "Bu çalma listesinde hiç şarkı yok.", android.widget.Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            val result = com.example.otomuzik.util.PlaylistM3uHelper.exportToM3u(playlist, songs)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.fold(
                    onSuccess = { file ->
                        android.widget.Toast.makeText(
                            app,
                            "✓ M3U Listesi kaydedildi:\n${file.name} (Müzik klasörü)",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    },
                    onFailure = { err ->
                        android.widget.Toast.makeText(
                            app,
                            "Dışa aktarma başarısız: ${err.localizedMessage ?: "Hata"}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    fun importM3uFile(file: File) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val app = getApplication<Application>()
            val (name, songs) = com.example.otomuzik.util.PlaylistM3uHelper.parseM3u(file, _allSongs.value)
            if (songs.isEmpty()) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        app,
                        "M3U dosyasında kütüphanenizle eşleşen şarkı bulunamadı.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }
            val playlistId = repository.createPlaylist(name)
            if (playlistId != -1L) {
                repository.addSongsToPlaylist(playlistId, songs)
                loadPlaylists()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        app,
                        "✓ '$name' listesi içe aktarıldı (${songs.size} parça)",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun batchUpdateId3Tags(
        folderPath: String,
        newArtist: String,
        newAlbum: String,
        newGenre: String,
        newYear: String
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val app = getApplication<Application>()
            val dir = File(folderPath)
            if (!dir.exists() || !dir.isDirectory) return@launch

            val songsInFolder = _allSongs.value.filter {
                try {
                    val file = File(it.path)
                    file.parentFile?.absolutePath == dir.absolutePath
                } catch (e: Exception) { false }
            }

            var updatedCount = 0
            for (song in songsInFolder) {
                val effectiveArtist = if (newArtist.isNotBlank()) newArtist else song.artist
                val effectiveAlbum = if (newAlbum.isNotBlank()) newAlbum else song.album
                val effectiveGenre = if (newGenre.isNotBlank()) newGenre else song.genre
                val effectiveYear = if (newYear.isNotBlank()) newYear else song.year

                val ok = com.example.otomuzik.util.Id3Helper.writeTag(
                    context = app,
                    path = song.path,
                    title = song.title,
                    artist = effectiveArtist,
                    album = effectiveAlbum,
                    genre = effectiveGenre,
                    year = effectiveYear
                )

                repository.updateSongDetails(
                    oldPath = song.path,
                    newPath = song.path,
                    newTitle = song.title,
                    newArtist = effectiveArtist,
                    newAlbum = effectiveAlbum,
                    newGenre = effectiveGenre,
                    newYear = effectiveYear
                )
                if (ok) updatedCount++
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(
                    app,
                    "✓ $updatedCount parçanın ID3 etiketleri güncellendi",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            refreshAll()
        }
    }

    // Oynatma kontrolleri
    fun togglePlayPause() = MusicController.togglePlayPause()
    fun playNext() = MusicController.playNext()
    fun playPrevious() = MusicController.playPrevious()
    fun seekTo(positionMs: Long) = MusicController.seekTo(positionMs)
    fun toggleShuffle() = MusicController.toggleShuffle()
    fun reshuffleUpcoming() = MusicController.reshuffleUpcoming()
    fun toggleRepeatMode() = MusicController.toggleRepeatMode()

    // --- Gelişmiş Ayar Fonksiyonları ---
    fun setAutoPlayOnBluetooth(enabled: Boolean) {
        _autoPlayOnBluetooth.value = enabled
        repository.setAutoPlayOnBluetooth(enabled)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        repository.setKeepScreenOn(enabled)
    }

    fun setCrossfadeDuration(durationMs: Int) {
        _crossfadeDuration.value = durationMs
        repository.setCrossfadeDuration(durationMs)
    }

    fun setAutoPlayOnStart(enabled: Boolean) {
        _autoPlayOnStart.value = enabled
        repository.setAutoPlayOnStart(enabled)
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
        repository.setGaplessPlayback(enabled)
    }

    fun setDefaultStartTab(tab: String) {
        _defaultStartTab.value = tab
        repository.setDefaultStartTab(tab)
    }

    fun setShowSmartPlaylists(show: Boolean) {
        _showSmartPlaylists.value = show
        repository.setShowSmartPlaylists(show)
    }

    fun setShowBlurredBackground(show: Boolean) {
        _showBlurredBackground.value = show
        repository.setShowBlurredBackground(show)
    }

    fun setShowWaveform(show: Boolean) {
        _showWaveform.value = show
        repository.setShowWaveform(show)
    }

    fun setIsAlbumArtCropped(cropped: Boolean) {
        _isalbumartCropped.value = cropped
        repository.setIsAlbumArtCropped(cropped)
    }

    fun setStartOnBoot(enabled: Boolean) {
        _startOnBoot.value = enabled
        repository.setStartOnBoot(enabled)
    }

    fun hideSongFromLibrary(song: Song, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            repository.hideSongFromLibrary(song)
            _hiddenSongsCount.value = repository.getHiddenSongsCount()

            if (MusicController.playerState.value.currentSong?.path == song.path) {
                MusicController.playNext()
            }
            val updated = _allSongs.value.filter { it.path != song.path }
            _allSongs.value = updated
            MusicController.syncQueueWithLibrary(updated)

            val currentPath = _currentFolderPath.value
            if (!currentPath.isNullOrEmpty()) {
                _currentFolderItems.value = repository.getFolderContents(currentPath)
            }
            loadPlaylists()

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(app, "👁️ '${song.title}' kütüphaneden gizlendi", android.widget.Toast.LENGTH_SHORT).show()
            }
            onComplete?.invoke()
        }
    }

    fun clearHiddenSongs() {
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            repository.clearHiddenSongs()
            _hiddenSongsCount.value = 0
            rescanLibrary()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(app, "Gizlenen tüm şarkılar kütüphaneye geri yüklendi", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setStopOnTaskRemoved(stop: Boolean) {
        _stopOnTaskRemoved.value = stop
        repository.setStopOnTaskRemoved(stop)
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        repository.setThemeMode(mode)
    }

    fun setThemePalette(palette: String) {
        _themePalette.value = palette
        repository.setThemePalette(palette)
        if (palette == "DYNAMIC") {
            viewModelScope.launch {
                val art = _currentSongartwork.value
                _accentColor.value = if (art != null) {
                    com.example.otomuzik.theme.DynamicColorManager.extractDominantColor(art)
                } else {
                    androidx.compose.ui.graphics.Color(0xFF00E5FF)
                }
            }
        } else {
            _accentColor.value = getPalettePrimaryColor(palette)
        }
    }

    fun setAutoOpenQueueOnStart(enabled: Boolean) {
        _autoOpenQueueOnStart.value = enabled
        repository.setAutoOpenQueueOnStart(enabled)
    }

    fun setLyricsFontSize(size: String) {
        _lyricsFontSize.value = size
        repository.setLyricsFontSize(size)
    }

    fun setAutoSaveId3Lyrics(enabled: Boolean) {
        _autoSaveId3Lyrics.value = enabled
        repository.setAutoSaveId3Lyrics(enabled)
        if (enabled) {
            val song = playerState.value.currentSong
            val data = _lyricsData.value
            if (song != null && data != null && data.rawLyrics.isNotBlank()) {
                viewModelScope.launch {
                    com.example.otomuzik.util.LyricsHelper.saveEmbeddedLyrics(song.path, data.rawLyrics)
                }
            }
        }
    }

    fun clearPlayHistory() {
        viewModelScope.launch {
            repository.clearPlayHistory()
            loadPlaylists() // Playlists'i yenilemeli
        }
    }

    fun setUiMode(mode: String) {
        _uiMode.value = mode
        repository.setUiMode(mode)
    }

    fun confirmUiMode(mode: String) {
        _uiMode.value = mode
        repository.setUiMode(mode)
        _isUiModeConfigured.value = true
        repository.setIsUiModeConfigured(true)
        _showModeSelectDialog.value = false
    }

    fun openModeSelectDialog() {
        _showModeSelectDialog.value = true
    }

    fun closeModeSelectDialog() {
        _showModeSelectDialog.value = false
    }
}





















