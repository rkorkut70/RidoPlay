package com.example.otomuzik.ui.phone

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.otomuzik.R
import com.example.otomuzik.model.FolderItem
import com.example.otomuzik.model.Playlist
import com.example.otomuzik.model.Song
import com.example.otomuzik.theme.BaseCarBgDark
import com.example.otomuzik.theme.BaseCarCyan
import com.example.otomuzik.theme.CarBorder
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarSurfaceVariant
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.theme.CarTextPrimary
import com.example.otomuzik.ui.components.AddToPlaylistDialog
import com.example.otomuzik.ui.components.CreatePlaylistDialog
import com.example.otomuzik.ui.components.EqualizerDialog
import com.example.otomuzik.ui.components.FixedCarSearchBar
import com.example.otomuzik.ui.components.FolderBrowserView
import com.example.otomuzik.ui.components.FolderactionDialog
import com.example.otomuzik.ui.components.Id3InfoDialog
import com.example.otomuzik.ui.components.LyricsDialog
import com.example.otomuzik.ui.components.PlaylistsView
import com.example.otomuzik.ui.components.QueuePanel
import com.example.otomuzik.ui.components.SearchResultsView
import com.example.otomuzik.ui.components.SettingsDialog
import com.example.otomuzik.ui.components.SongListView
import com.example.otomuzik.ui.components.SongactionDialog
import com.example.otomuzik.ui.main.MainScreenViewModel
import com.example.otomuzik.ui.main.TabType

/**
 * Cep telefonu için optimize edilmiş dikey ana ekran.
 * Tek elle kullanıma uygun: Üstte sekmeler ve arama, ortada akıcı liste, altta Mini-Player.
 */
@Composable
fun PhoneMainScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val storageRoots by viewModel.storageRoots.collectAsStateWithLifecycle()
    val currentFolderPath by viewModel.currentFolderPath.collectAsStateWithLifecycle()
    val currentFolderItems by viewModel.currentFolderItems.collectAsStateWithLifecycle()
    val favoritePaths by viewModel.favoritePaths.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.Playlists.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistSongs by viewModel.selectedPlaylistSongs.collectAsStateWithLifecycle()
    val currentArtwork by viewModel.currentSongartwork.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()

    val folderContextItem by viewModel.folderContextItem.collectAsStateWithLifecycle()
    val songContextItem by viewModel.songContextItem.collectAsStateWithLifecycle()
    val songsToaddToPlaylist by viewModel.songsToaddToPlaylist.collectAsStateWithLifecycle()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()

    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val showEqualizerDialog by viewModel.showEqualizerDialog.collectAsStateWithLifecycle()
    val showId3Dialog by viewModel.showId3Dialog.collectAsStateWithLifecycle()
    val currentSongId3 by viewModel.currentSongId3.collectAsStateWithLifecycle()

    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
    val showQueueDialog by viewModel.showQueueDialog.collectAsStateWithLifecycle()

    val showLyricsDialog by viewModel.showLyricsDialog.collectAsStateWithLifecycle()
    val isSearchingLyrics by viewModel.isSearchingLyrics.collectAsStateWithLifecycle()
    val lyricsStatusMessage by viewModel.lyricsStatusMessage.collectAsStateWithLifecycle()
    val autoSaveId3Lyrics by viewModel.autoSaveId3Lyrics.collectAsStateWithLifecycle()
    val lyricsData by viewModel.lyricsData.collectAsStateWithLifecycle()

    var showFullscreenPlayer by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val currentSong = playerState.currentSong
    val isFavorite = currentSong != null && favoritePaths.contains(currentSong.path)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BaseCarBgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 1. ÜST BAŞLIK (Brand & Eylemler) ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CarSurfaceDark)
                            .border(1.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_splash_logo),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "RidoPlay",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Arama Aç/Kapat
                    IconButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        modifier = Modifier
                            .size(38.dp)
                            .background(CarSurfaceDark, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Ara",
                            tint = if (isSearchExpanded || searchQuery.isNotEmpty()) accentColor else CarTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // EQ butonu
                    IconButton(
                        onClick = { viewModel.openEqualizerDialog() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(CarSurfaceDark, CircleShape)
                    ) {
                        Text(text = "EQ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }

                    // Ayarlar butonu
                    IconButton(
                        onClick = { viewModel.openSettingsDialog() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(CarSurfaceDark, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Ayarlar",
                            tint = CarTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── 2. ARAMA ÇUBUĞU (Açıksa) ──────────────────────────────────
            if (isSearchExpanded || searchQuery.isNotEmpty()) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    FixedCarSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        resultCount = if (searchQuery.isNotEmpty()) filteredSongs.size else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── 3. DİKEY TELEFON SEKMELERİ ────────────────────────────────
            if (searchQuery.isEmpty()) {
                val tabs = listOf(
                    Triple(TabType.ALL_SONGS, "Şarkılar", allSongs.size),
                    Triple(TabType.FOLDERS, "Klasörler", storageRoots.size),
                    Triple(TabType.PLAYLISTS, "Listeler", playlists.size),
                    Triple(TabType.FAVORITES, "Favoriler", favoriteSongs.size)
                )

                val selectedTabIndex = tabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = accentColor,
                    edgePadding = 14.dp,
                    indicator = { tabPositions ->
                        if (selectedTabIndex in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = accentColor,
                                height = 3.dp
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, (tabType, title, count) ->
                        val isSelected = activeTab == tabType
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.setTab(tabType) },
                            text = {
                                Text(
                                    text = if (count > 0) "$title ($count)" else title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accentColor else CarTextMuted
                                )
                            }
                        )
                    }
                }
            }

            // ── 4. LİSTE İÇERİK ALANI (Gövde) ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 4.dp)
            ) {
                if (searchQuery.isNotEmpty()) {
                    SearchResultsView(
                        songs = filteredSongs,
                        query = searchQuery,
                        currentPlayingSongPath = currentSong?.path,
                        onPlaySong = { song ->
                            viewModel.playSongFromList(song, filteredSongs)
                            viewModel.setSearchQuery("")
                            isSearchExpanded = false
                        },
                        onSongLongClick = { viewModel.openSongContext(it) }
                    )
                } else {
                    when (activeTab) {
                        TabType.FOLDERS -> {
                            FolderBrowserView(
                                storageRoots = storageRoots,
                                currentFolderPath = currentFolderPath,
                                currentFolderItems = currentFolderItems,
                                currentPlayingSongPath = currentSong?.path,
                                onOpenFolder = { viewModel.openFolder(it) },
                                onBackFolder = { viewModel.goBackFolder() },
                                onPlayFolder = { viewModel.playEntireFolder(it) },
                                onPlayallSongs = { viewModel.playallSongs() },
                                onFolderLongClick = { viewModel.openFolderContext(it) },
                                onPlaySong = { song, list -> viewModel.playSongFromList(song, list) },
                                onSongLongClick = { viewModel.openSongContext(it) }
                            )
                        }

                        TabType.PLAYLISTS -> {
                            PlaylistsView(
                                Playlists = playlists,
                                selectedPlaylist = selectedPlaylist,
                                selectedPlaylistSongs = selectedPlaylistSongs,
                                currentPlayingSongPath = currentSong?.path,
                                onOpenPlaylist = { viewModel.openPlaylist(it) },
                                onClosePlaylist = { viewModel.closePlaylist() },
                                onPlayPlaylist = { viewModel.playPlaylist(it) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onRemoveSongFromPlaylist = { plId, s -> viewModel.removeSongFromPlaylist(plId, s) },
                                onCreatePlaylistClick = { viewModel.openCreatePlaylistDialog() },
                                onPlaySong = { song -> viewModel.playSongFromList(song, selectedPlaylistSongs) },
                                onSongLongClick = { viewModel.openSongContext(it) }
                            )
                        }

                        TabType.FAVORITES -> {
                            SongListView(
                                songs = favoriteSongs,
                                currentPlayingSongPath = currentSong?.path,
                                onPlaySong = { song -> viewModel.playSongFromList(song, favoriteSongs) },
                                onSongLongClick = { viewModel.openSongContext(it) },
                                emptyMessage = "Henüz favori şarkı eklenmedi."
                            )
                        }

                        else -> {
                            // ALL_SONGS & HOME (Telefonda varsayılan olarak şarkı listesi)
                            SongListView(
                                songs = allSongs,
                                currentPlayingSongPath = currentSong?.path,
                                onPlaySong = { song -> viewModel.playSongFromList(song, allSongs) },
                                onSongLongClick = { viewModel.openSongContext(it) }
                            )
                        }
                    }
                }
            }

            // ── 5. ALT MINI OYNATICI (Çalan şarkı varsa görünür) ───────────
            if (currentSong != null) {
                PhoneMiniPlayer(
                    currentSong = currentSong,
                    isPlaying = playerState.isPlaying,
                    artworkBitmap = currentArtwork,
                    isFavorite = isFavorite,
                    currentPositionMs = playerState.currentPositionMs,
                    durationMs = playerState.durationMs,
                    accentColor = accentColor,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onToggleFavorite = { viewModel.toggleFavorite(currentSong) },
                    onClick = { showFullscreenPlayer = true }
                )
            }
        }

        // ── 6. TAM EKRAN ÇALAN PARÇA PENCERESİ (Mini-Player'a tıklanınca açılır)
        if (showFullscreenPlayer && currentSong != null) {
            PhoneNowPlayingSheet(
                song = currentSong,
                isPlaying = playerState.isPlaying,
                artworkBitmap = currentArtwork,
                isFavorite = isFavorite,
                isShuffle = playerState.isShuffle,
                repeatMode = playerState.repeatMode,
                currentPositionMs = playerState.currentPositionMs,
                durationMs = playerState.durationMs,
                accentColor = accentColor,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeekTo = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onToggleFavorite = { viewModel.toggleFavorite(currentSong) },
                onOpenQueue = { viewModel.openQueueDialog() },
                onOpenLyrics = { viewModel.openLyricsDialog() },
                onOpenEqualizer = { viewModel.openEqualizerDialog() },
                onOpenId3Info = { viewModel.openId3Dialog() },
                onOpenSettings = { viewModel.openSettingsDialog() },
                onDismiss = { showFullscreenPlayer = false }
            )
        }

        // ── 7. TELEFON İÇİN DİYALOGLAR (EQ, Sıra, Şarkı Sözleri, Ayarlar vb.) ──
        folderContextItem?.let { folder ->
            FolderactionDialog(
                folderItem = folder,
                onPlayFolder = { viewModel.playEntireFolder(folder.path) },
                onaddToPlaylist = { viewModel.openaddToPlaylistDialogForFolder(folder.path) },
                onaddToQueue = { viewModel.AddFolderToQueue(folder.path) },
                onDismiss = { viewModel.closeFolderContext() }
            )
        }

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

        songsToaddToPlaylist?.let { songs ->
            AddToPlaylistDialog(
                Playlists = playlists.filter { it.id > 0 },
                songsCount = songs.size,
                onSelectPlaylist = { pl -> viewModel.addSongsToPlaylist(pl.id, songs) },
                onCreateNewPlaylist = { viewModel.openCreatePlaylistDialog() },
                onDismiss = { viewModel.closeaddToPlaylistDialog() }
            )
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onCreate = { name -> viewModel.createPlaylist(name, songsToaddToPlaylist) },
                onDismiss = { viewModel.closeCreatePlaylistDialog() }
            )
        }

        // Ekolayzer & Bas Ayarı Penceresi
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

        // ID3 Şarkı Bilgisi Penceresi
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

        // Şarkı Sözleri Penceresi
        if (showLyricsDialog) {
            val currentTitle = currentSongId3?.title?.takeIf { it.isNotBlank() } ?: playerState.currentSong?.title ?: "Şarkı"
            val currentArtist = currentSongId3?.artist?.takeIf { it.isNotBlank() } ?: playerState.currentSong?.artist ?: "Bilinmeyen Sanatçı"

            LyricsDialog(
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
                onSearchOnline = { artist, title -> viewModel.searchLyricsOnline(artist, title) },
                onSaveLyrics = { onResult -> viewModel.saveLyricsToDisk(onResult) },
                onPasteLyrics = { text -> viewModel.pasteLyricsFromClipboard(text) },
                onSeekTo = { posMs -> viewModel.seekToLyricPosition(posMs) },
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.playPrevious() },
                onNext = { viewModel.playNext() },
                onDismiss = { viewModel.closeLyricsDialog() }
            )
        }

        // Çalma Sırası Penceresi
        if (showQueueDialog) {
            QueuePanel(
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
            SettingsDialog(
                onDismissRequest = { viewModel.closeSettingsDialog() },
                viewModel = viewModel
            )
        }
    }
}
