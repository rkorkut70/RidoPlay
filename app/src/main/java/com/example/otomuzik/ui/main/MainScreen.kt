package com.example.otomuzik.ui.main
import com.example.otomuzik.theme.*

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.otomuzik.ui.components.AddToPlaylistDialog
import com.example.otomuzik.ui.components.CreatePlaylistDialog
import com.example.otomuzik.ui.components.EqualizerDialog
import com.example.otomuzik.ui.components.FolderactionDialog
import com.example.otomuzik.ui.components.Id3InfoDialog
import com.example.otomuzik.ui.components.LibraryPanel
import com.example.otomuzik.ui.components.NowPlayingPanel
import com.example.otomuzik.ui.components.SongactionDialog

@Composable
fun MainScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val favoritePaths by viewModel.favoritePaths.collectAsStateWithLifecycle()
    val storageRoots by viewModel.storageRoots.collectAsStateWithLifecycle()
    val currentFolderPath by viewModel.currentFolderPath.collectAsStateWithLifecycle()
    val currentFolderItems by viewModel.currentFolderItems.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Çalma Listeleri Durumu
    val Playlists by viewModel.Playlists.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistSongs by viewModel.selectedPlaylistSongs.collectAsStateWithLifecycle()

    // Context Menüleri ve Diyaloglar
    val folderContextItem by viewModel.folderContextItem.collectAsStateWithLifecycle()
    val songContextItem by viewModel.songContextItem.collectAsStateWithLifecycle()
    val songsToaddToPlaylist by viewModel.songsToaddToPlaylist.collectAsStateWithLifecycle()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()

    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val showEqualizerDialog by viewModel.showEqualizerDialog.collectAsStateWithLifecycle()
    val showId3Dialog by viewModel.showId3Dialog.collectAsStateWithLifecycle()
    val currentSongartwork by viewModel.currentSongartwork.collectAsStateWithLifecycle()
    val currentSongId3 by viewModel.currentSongId3.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
    
    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
    val showQueueDialog by viewModel.showQueueDialog.collectAsStateWithLifecycle()
    val isQueueVisible by viewModel.isQueueVisible.collectAsStateWithLifecycle()
    val showBlurredBackground by viewModel.showBlurredBackground.collectAsStateWithLifecycle()
    val showWaveform by viewModel.showWaveform.collectAsStateWithLifecycle()
    val isalbumartCropped by viewModel.isalbumartCropped.collectAsStateWithLifecycle()
    val showCoverLyrics by viewModel.showCoverLyrics.collectAsStateWithLifecycle()
    val lyricsData by viewModel.lyricsData.collectAsStateWithLifecycle()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsStateWithLifecycle()

    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Geri tuşu kontrolü (Sıra, Çalma Listesi, Klasör veya Alt Sekmeler açıksa kademeli geri döner)
    BackHandler(enabled = isQueueVisible || selectedPlaylist != null || currentFolderPath != null || activeTab != TabType.HOME) {
        when {
            isQueueVisible -> viewModel.closeQueue()
            selectedPlaylist != null -> viewModel.closePlaylist()
            currentFolderPath != null -> viewModel.goBackFolder()
            activeTab != TabType.HOME -> viewModel.setTab(TabType.HOME)
        }
    }

    val currentSongPath = playerState.currentSong?.path
    val isFavorite = currentSongPath != null && favoritePaths.contains(currentSongPath)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CarBgDark)
    ) {
        // ── 1. BULANIK ARKA PLAN KATMANI (Edge-to-Edge) ──
        if (showBlurredBackground) {
            if (currentSongartwork != null) {
                androidx.compose.foundation.Image(
                    bitmap = currentSongartwork!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.15f, scaleY = 1.15f)
                        .blur(50.dp)
                )
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.38f))
                )
            } else {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.30f),
                                    accentColor.copy(alpha = 0.10f),
                                    androidx.compose.ui.graphics.Color.Transparent
                                ),
                                radius = 1100f
                            )
                        )
                )
            }
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(
                    horizontal = if (isCompact) 6.dp else 8.dp,
                    vertical = if (isCompact) 4.dp else 8.dp
                )
        ) {
            if (isLandscape) {
                // Otomobil Ekranları İçin İki Bölmeli Yatay Düzen (Landscape)
                Row(modifier = Modifier.fillMaxSize()) {
                    // Sol Bölme: Büyük Tuşlu Oynatıcı Kontrol Paneli (%46 genişlik)
                    NowPlayingPanel(
                        playerState = playerState,
                        artworkBitmap = currentSongartwork,
                        currentSongId3 = currentSongId3,
                        accentColor = accentColor,
                        isFavorite = isFavorite,
                        lyricsData = lyricsData,
                        showCoverLyrics = showCoverLyrics,
                        onToggleCoverLyrics = { viewModel.toggleCoverLyrics() },
                        onToggleFavorite = { playerState.currentSong?.let { viewModel.toggleFavorite(it) } },
                        onOpenEqualizer = { viewModel.openEqualizerDialog() },
                        onOpenId3Info = { viewModel.openId3Dialog() },
                        onOpenLyrics = { viewModel.openLyricsDialog() },
                        onOpenQueue = { viewModel.openQueueDialog() },
                        onOpenSleepTimer = { viewModel.openSleepTimerDialog() },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playNext() },
                        onPrevious = { viewModel.playPrevious() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggLERepeat = { viewModel.toggleRepeatMode() },
                        showWaveform = showWaveform,
                        isalbumartCropped = isalbumartCropped,
                        lyricsFontSize = lyricsFontSize,
                        showBlurredBackground = showBlurredBackground,
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                    )

                    Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 10.dp))

                    // Sağ Bölme: USB Klasör Gezgini, Kütüphane & Çalma Listeleri (%54 genişlik)
                    LibraryPanel(
                        activeTab = activeTab,
                        onTabSelected = { viewModel.setTab(it) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        storageRoots = storageRoots,
                        currentFolderPath = currentFolderPath,
                        currentFolderItems = currentFolderItems,
                        onOpenFolder = { viewModel.openFolder(it) },
                        onBackFolder = { viewModel.goBackFolder() },
                        onPlayFolder = { viewModel.playEntireFolder(it) },
                        onPlayallSongs = { viewModel.playallSongs() },
                        onFolderLongClick = { viewModel.openFolderContext(it) },
                        filteredSongs = filteredSongs,
                        favoriteSongs = favoriteSongs,
                        currentPlayingSongPath = currentSongPath,
                        onPlaySong = { song, List -> viewModel.playSongFromList(song, List) },
                        onSongLongClick = { viewModel.openSongContext(it) },
                        Playlists = Playlists,
                        selectedPlaylist = selectedPlaylist,
                        selectedPlaylistSongs = selectedPlaylistSongs,
                        onOpenPlaylist = { viewModel.openPlaylist(it) },
                        onClosePlaylist = { viewModel.closePlaylist() },
                        onPlayPlaylist = { viewModel.playPlaylist(it) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) },
                        onRemoveSongFromPlaylist = { plId, song -> viewModel.removeSongFromPlaylist(plId, song) },
                        onCreatePlaylistClick = { viewModel.openCreatePlaylistDialog() },
                        isQueueVisible = isQueueVisible,
                        onCloseQueue = { viewModel.closeQueue() },
                        onOpenQueue = { viewModel.openQueue() },
                        currentQueue = currentQueue,
                        currentQueueIndex = currentQueueIndex,
                        isShuffle = playerState.isShuffle,
                        accentColor = accentColor,
                        onPlayQueueIndex = { viewModel.playAtQueueIndex(it) },
                        onPlayNext = { viewModel.playNext() },
                        onPlayPrevious = { viewModel.playPrevious() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onReshuffleUpcoming = { viewModel.reshuffleUpcoming() },
                        onRefresh = { viewModel.rescanLibrary() },
                        onOpenSettings = { viewModel.openSettingsDialog() },
                        isLoading = isLoading,
                        showBlurredBackground = showBlurredBackground,
                        currentSong = playerState.currentSong,
                        previousSong = playerState.previousSong,
                        nextSong = playerState.nextSong,
                        isPlaying = playerState.isPlaying,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onPlayFavorites = { viewModel.playFavorites() },
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                    )
                }
            } else {
                // Dikey Ekran Düzeni (Telefon / Tesla Tipi Ekranlar)
                Column(modifier = Modifier.fillMaxSize()) {
                    NowPlayingPanel(
                        playerState = playerState,
                        artworkBitmap = currentSongartwork,
                        currentSongId3 = currentSongId3,
                        accentColor = accentColor,
                        isFavorite = isFavorite,
                        lyricsData = lyricsData,
                        showCoverLyrics = showCoverLyrics,
                        onToggleCoverLyrics = { viewModel.toggleCoverLyrics() },
                        onToggleFavorite = { playerState.currentSong?.let { viewModel.toggleFavorite(it) } },
                        onOpenEqualizer = { viewModel.openEqualizerDialog() },
                        onOpenId3Info = { viewModel.openId3Dialog() },
                        onOpenLyrics = { viewModel.openLyricsDialog() },
                        onOpenQueue = { viewModel.openQueueDialog() },
                        onOpenSleepTimer = { viewModel.openSleepTimerDialog() },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playNext() },
                        onPrevious = { viewModel.playPrevious() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggLERepeat = { viewModel.toggleRepeatMode() },
                        showWaveform = showWaveform,
                        isalbumartCropped = isalbumartCropped,
                        lyricsFontSize = lyricsFontSize,
                        showBlurredBackground = showBlurredBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.48f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LibraryPanel(
                        activeTab = activeTab,
                        onTabSelected = { viewModel.setTab(it) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        storageRoots = storageRoots,
                        currentFolderPath = currentFolderPath,
                        currentFolderItems = currentFolderItems,
                        onOpenFolder = { viewModel.openFolder(it) },
                        onBackFolder = { viewModel.goBackFolder() },
                        onPlayFolder = { viewModel.playEntireFolder(it) },
                        onPlayallSongs = { viewModel.playallSongs() },
                        onFolderLongClick = { viewModel.openFolderContext(it) },
                        filteredSongs = filteredSongs,
                        favoriteSongs = favoriteSongs,
                        currentPlayingSongPath = currentSongPath,
                        onPlaySong = { song, List -> viewModel.playSongFromList(song, List) },
                        onSongLongClick = { viewModel.openSongContext(it) },
                        Playlists = Playlists,
                        selectedPlaylist = selectedPlaylist,
                        selectedPlaylistSongs = selectedPlaylistSongs,
                        onOpenPlaylist = { viewModel.openPlaylist(it) },
                        onClosePlaylist = { viewModel.closePlaylist() },
                        onPlayPlaylist = { viewModel.playPlaylist(it) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) },
                        onRemoveSongFromPlaylist = { plId, song -> viewModel.removeSongFromPlaylist(plId, song) },
                        onCreatePlaylistClick = { viewModel.openCreatePlaylistDialog() },
                        isQueueVisible = isQueueVisible,
                        onCloseQueue = { viewModel.closeQueue() },
                        onOpenQueue = { viewModel.openQueue() },
                        currentQueue = currentQueue,
                        currentQueueIndex = currentQueueIndex,
                        isShuffle = playerState.isShuffle,
                        accentColor = accentColor,
                        onPlayQueueIndex = { viewModel.playAtQueueIndex(it) },
                        onPlayNext = { viewModel.playNext() },
                        onPlayPrevious = { viewModel.playPrevious() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onReshuffleUpcoming = { viewModel.reshuffleUpcoming() },
                        onRefresh = { viewModel.rescanLibrary() },
                        onOpenSettings = { viewModel.openSettingsDialog() },
                        isLoading = isLoading,
                        showBlurredBackground = showBlurredBackground,
                        currentSong = playerState.currentSong,
                        previousSong = playerState.previousSong,
                        nextSong = playerState.nextSong,
                        isPlaying = playerState.isPlaying,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onPlayFavorites = { viewModel.playFavorites() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.52f)
                    )
                }
            }
        }

        // Klasr Basılı Tutma (Context Menü) Penceresi
        folderContextItem?.let { folder ->
            FolderactionDialog(
                folderItem = folder,
                onPlayFolder = { viewModel.playEntireFolder(folder.path) },
                onaddToPlaylist = { viewModel.openaddToPlaylistDialogForFolder(folder.path) },
                onaddToQueue = { viewModel.AddFolderToQueue(folder.path) },
                onDismiss = { viewModel.closeFolderContext() }
            )
        }

        // Şarkı Basılı Tutma (Context Menü) Penceresi
        songContextItem?.let { song ->
            SongactionDialog(
                song = song,
                isFavorite = favoritePaths.contains(song.path),
                onaddToPlaylist = { viewModel.openaddToPlaylistDialogForSong(song) },
                onaddToQueue = { viewModel.AddSongToQueue(song) },
                onToggleFavorite = { viewModel.toggleFavorite(song) },
                onDismiss = { viewModel.closeSongContext() }
            )
        }

        // Çalma Listesine Ekleme Seçim Penceresi
        songsToaddToPlaylist?.let { songs ->
            AddToPlaylistDialog(
                Playlists = Playlists.filter { it.id > 0 },
                songsCount = songs.size,
                onSelectPlaylist = { pl -> viewModel.addSongsToPlaylist(pl.id, songs) },
                onCreateNewPlaylist = { viewModel.openCreatePlaylistDialog() },
                onDismiss = { viewModel.closeaddToPlaylistDialog() }
            )
        }

        // Yeni Çalma Listesi Oluşturma Penceresi
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onCreate = { name -> viewModel.createPlaylist(name, songsToaddToPlaylist) },
                onDismiss = { viewModel.closeCreatePlaylistDialog() }
            )
        }

        // Ekolayzer & Bas ayarı Penceresi
        if (showEqualizerDialog) {
            EqualizerDialog(
                eqState = equalizerState,
                onToggleEnable = { viewModel.toggleEqualizer(it) },
                onSelectPreset = { viewModel.setEqPreset(it) },
                onBandChange = { idx, lvl -> viewModel.setEqBandLevel(idx, lvl) },
                onBassBoostChange = { viewModel.setEqBassBoost(it) },
                onLoudnessEnhancerChange = { viewModel.setLoudnessEnhancerEnabled(it) },
                onDismiss = { viewModel.closeEqualizerDialog() }
            )
        }

        val showSleepTimerDialog by viewModel.showSleepTimerDialog.collectAsStateWithLifecycle()
        val sleepTimerLeftMs by viewModel.sleepTimerLeftMs.collectAsStateWithLifecycle()

        if (showSleepTimerDialog) {
            com.example.otomuzik.ui.components.SleepTimerDialog(
                sleepTimerLeftMs = sleepTimerLeftMs,
                onSetTimer = { viewModel.setSleepTimer(it) },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { viewModel.closeSleepTimerDialog() }
            )
        }

        // ID3 Şarkı Bilgisi ve Teknik Detaylar Penceresi
        if (showId3Dialog && playerState.currentSong != null) {
            Id3InfoDialog(
                song = playerState.currentSong!!,
                onDismiss = { viewModel.closeId3Dialog() },
                onSave = { newFileName, title, artist, album, genre, year, artworkBytes ->
                    viewModel.saveId3Tags(playerState.currentSong!!, newFileName, title, artist, album, genre, year, artworkBytes)
                    viewModel.closeId3Dialog()
                }
            )
        }

        val showLyricsDialog by viewModel.showLyricsDialog.collectAsStateWithLifecycle()
        val isSearchingLyrics by viewModel.isSearchingLyrics.collectAsStateWithLifecycle()
        val lyricsStatusMessage by viewModel.lyricsStatusMessage.collectAsStateWithLifecycle()
        val autoSaveId3Lyrics by viewModel.autoSaveId3Lyrics.collectAsStateWithLifecycle()

        // Şarkı Sözleri (Lyrics) Penceresi
        if (showLyricsDialog) {
            val currentTitle = currentSongId3?.title?.takeIf { it.isNotBlank() } ?: playerState.currentSong?.title ?: "Şarkı"
            val currentArtist = currentSongId3?.artist?.takeIf { it.isNotBlank() } ?: playerState.currentSong?.artist ?: "Bilinmeyen Sanatçı"

            com.example.otomuzik.ui.components.LyricsDialog(
                lyricsData = lyricsData,
                currentPositionMs = playerState.currentPositionMs,
                durationMs = playerState.durationMs,
                isPlaying = playerState.isPlaying,
                songTitle = currentTitle,
                artistName = currentArtist,
                accentColor = accentColor,
                isSearching = isSearchingLyrics,
                statusMessage = lyricsStatusMessage,
                autoSaveId3Lyrics = autoSaveId3Lyrics,
                onToggleAutoSaveId3 = { viewModel.setAutoSaveId3Lyrics(it) },
                onSearchOnline = { artist, title ->
                    viewModel.searchLyricsOnline(artist, title)
                },
                onSaveLyrics = { onResult ->
                    viewModel.saveLyricsToDisk(onResult)
                },
                onPasteLyrics = { text ->
                    viewModel.pasteLyricsFromClipboard(text)
                },
                onSeekTo = { posMs ->
                    viewModel.seekToLyricPosition(posMs)
                },
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.playPrevious() },
                onNext = { viewModel.playNext() },
                onDismiss = { viewModel.closeLyricsDialog() }
            )
        }

        // Çalma Sırası (Queue) Penceresi
        if (showQueueDialog) {
            com.example.otomuzik.ui.components.QueuePanel(
                queue = currentQueue,
                currentIndex = currentQueueIndex,
                isShuffle = playerState.isShuffle,
                accentColor = accentColor,
                onPlayAtIndex = {
                    viewModel.playAtQueueIndex(it)
                    viewModel.closeQueueDialog()
                },
                onPlayNext = { viewModel.playNext() },
                onPlayPrevious = { viewModel.playPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onReshuffleUpcoming = { viewModel.reshuffleUpcoming() },
                onDismiss = { viewModel.closeQueueDialog() }
            )
        }

        // Ayarlar Penceresi
        if (showSettingsDialog) {
            com.example.otomuzik.ui.components.SettingsDialog(
                onDismissRequest = { viewModel.closeSettingsDialog() },
                viewModel = viewModel
            )
        }
    }
}



















