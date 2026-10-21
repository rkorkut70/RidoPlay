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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.otomuzik.ui.components.AboutDialog
import com.example.otomuzik.ui.components.AddToPlaylistDialog
import com.example.otomuzik.ui.components.CreatePlaylistDialog
import com.example.otomuzik.ui.components.DuplicateFinderDialog
import com.example.otomuzik.ui.components.EqualizerDialog
import com.example.otomuzik.ui.components.FixedCarSearchBar
import com.example.otomuzik.ui.components.FolderBrowserView
import com.example.otomuzik.ui.components.FolderSelectionDialog
import com.example.otomuzik.ui.components.FolderactionDialog
import com.example.otomuzik.ui.components.Id3InfoDialog
import com.example.otomuzik.ui.components.LyricsDialog
import com.example.otomuzik.ui.components.PlaylistsView
import com.example.otomuzik.ui.components.QueuePanel
import com.example.otomuzik.ui.components.SearchResultsView
import com.example.otomuzik.ui.components.SettingsDialog
import com.example.otomuzik.ui.components.SongListView
import androidx.compose.ui.platform.LocalContext
import com.example.otomuzik.util.RingtoneHelper
import com.example.otomuzik.ui.components.BatchId3TagDialog
import com.example.otomuzik.ui.components.M3uImportDialog
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
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val activeArtistMode by viewModel.activeArtistMode.collectAsStateWithLifecycle()
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
    val showFolderSelectionDialog by viewModel.showFolderSelectionDialog.collectAsStateWithLifecycle()
    val includedFolders by viewModel.includedFolders.collectAsStateWithLifecycle()

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
    val showCoverLyrics by viewModel.showCoverLyrics.collectAsStateWithLifecycle()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsStateWithLifecycle()
    val showAboutDialog by viewModel.showAboutDialog.collectAsStateWithLifecycle()
    val libraryStats by viewModel.libraryStats.collectAsStateWithLifecycle()
    val swipeSensitivity by viewModel.swipeSensitivity.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val showWaveform by viewModel.showWaveform.collectAsStateWithLifecycle()

    val batchId3FolderItem by viewModel.batchId3FolderItem.collectAsStateWithLifecycle()
    val showM3uImportDialog by viewModel.showM3uImportDialog.collectAsStateWithLifecycle()
    val availableM3uFiles by viewModel.availableM3uFiles.collectAsStateWithLifecycle()
    val showDuplicateFinderDialog by viewModel.showDuplicateFinderDialog.collectAsStateWithLifecycle()

    var showFullscreenPlayer by rememberSaveable { mutableStateOf(true) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val currentSong = playerState.currentSong ?: allSongs.firstOrNull()
    val isFavorite = currentSong != null && favoritePaths.contains(currentSong.path)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BaseCarBgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── 1. ÜST BAŞLIK (Brand & Eylemler) ───────────────────────────
            val headerShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = headerShape,
                        ambientColor = Color.Black.copy(alpha = 0.5f),
                        spotColor = Color.Black.copy(alpha = 0.7f)
                    )
                    .clip(headerShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF262C38),
                                Color(0xFF171A22)
                            )
                        )
                    )
                    .border(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF424D60),
                                Color(0xFF1D212A)
                            )
                        ),
                        headerShape
                    )
            ) {
                // Üst kenar 3D specular gloss ışık yansıması
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.openAboutDialog() }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(3.dp, CircleShape, ambientColor = accentColor, spotColor = accentColor)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF2B3342), Color(0xFF181C25))
                                    )
                                )
                                .border(1.5.dp, accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_splash_logo),
                                contentDescription = "Hakkında",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "RidoPlay",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Arama Aç/Kapat Butonu (3D Dokunsal)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(3.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF2D3545), Color(0xFF1A1E27))
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    if (isSearchExpanded || searchQuery.isNotEmpty()) accentColor else Color(0xFF434E62),
                                    CircleShape
                                )
                                .clickable { isSearchExpanded = !isSearchExpanded },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = "Ara",
                                tint = if (isSearchExpanded || searchQuery.isNotEmpty()) accentColor else Color(0xFFF1F5F9),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // EQ butonu (3D Dokunsal)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(3.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF2D3545), Color(0xFF1A1E27))
                                    )
                                )
                                .border(1.2.dp, Color(0xFF434E62), CircleShape)
                                .clickable { viewModel.openEqualizerDialog() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "EQ", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                        }

                        // Ayarlar butonu (3D Dokunsal)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(3.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF2D3545), Color(0xFF1A1E27))
                                    )
                                )
                                .border(1.2.dp, Color(0xFF434E62), CircleShape)
                                .clickable { viewModel.openSettingsDialog() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "Ayarlar",
                                tint = Color(0xFFF1F5F9),
                                modifier = Modifier.size(20.dp)
                            )
                        }
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

            // ── 3. DİKEY TELEFON SEKMELERİ (3D Dokunsal Butonlar) ────────────────────
            if (searchQuery.isEmpty()) {
                val tabs = listOf(
                    Triple(TabType.ALL_SONGS, "Şarkılar", allSongs.size),
                    Triple(TabType.FOLDERS, "Klasörler", storageRoots.size),
                    Triple(TabType.PLAYLISTS, "Listeler", playlists.size),
                    Triple(TabType.FAVORITES, "Favoriler", favoriteSongs.size)
                )

                // Ekranı tam kaplayan 4'lü eşit 3D Segmented Bar (Asla ekrandan taşmaz)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { (tabType, title, count) ->
                        val isSelected = activeTab == tabType || (activeTab == TabType.HOME && tabType == TabType.ALL_SONGS)
                        val tabShape = RoundedCornerShape(12.dp)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .shadow(
                                    elevation = if (isSelected) 5.dp else 1.dp,
                                    shape = tabShape,
                                    ambientColor = if (isSelected) accentColor else Color.Black,
                                    spotColor = if (isSelected) accentColor else Color.Black
                                )
                                .clip(tabShape)
                                .background(
                                    if (isSelected) {
                                        Brush.verticalGradient(
                                            listOf(
                                                accentColor.copy(alpha = 0.35f),
                                                accentColor.copy(alpha = 0.16f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF242A36),
                                                Color(0xFF161922)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    brush = if (isSelected) {
                                        Brush.verticalGradient(
                                            listOf(accentColor, accentColor.copy(alpha = 0.50f))
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF384355), Color(0xFF1C2029))
                                        )
                                    },
                                    shape = tabShape
                                )
                                .clickable { viewModel.setTab(tabType) },
                            contentAlignment = Alignment.Center
                        ) {
                            // Üst parlama çizgisi (Glossy sheen)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                if (isSelected) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.18f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .align(Alignment.TopCenter)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = if (configuration.screenWidthDp < 360) 11.sp else 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) accentColor else Color(0xFFF1F5F9),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (count > 999) "999+" else "$count",
                                        fontSize = if (configuration.screenWidthDp < 360) 9.sp else 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) accentColor.copy(alpha = 0.85f) else Color(0xFF8E9BAE),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
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
                            viewModel.insertAsNextSong(song, playImmediately = true)
                            viewModel.setSearchQuery("")
                            isSearchExpanded = false
                        },
                        onPlayImmediately = { song ->
                            viewModel.insertAsNextSong(song, playImmediately = true)
                            viewModel.setSearchQuery("")
                            isSearchExpanded = false
                        },
                        onAddToNext = { song ->
                            viewModel.insertAsNextSong(song, playImmediately = false)
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
                                onSongLongClick = { viewModel.openSongContext(it) },
                                onManageFolders = { viewModel.openFolderSelectionDialog() }
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
                                onSongLongClick = { viewModel.openSongContext(it) },
                                onExportPlaylist = { viewModel.exportCurrentPlaylistM3u(it) },
                                onImportM3uClick = { viewModel.openM3uImportDialog() }
                            )
                        }

                        TabType.FAVORITES -> {
                            SongListView(
                                songs = favoriteSongs,
                                currentPlayingSongPath = currentSong?.path,
                                onPlaySong = { song -> viewModel.playSongFromList(song, favoriteSongs) },
                                onSongLongClick = { viewModel.openSongContext(it) },
                                accentColor = accentColor,
                                emptyMessage = "Henüz favori şarkı eklenmedi."
                            )
                        }

                        else -> {
                            // ALL_SONGS & HOME (Telefonda varsayılan olarak şarkı listesi)
                            SongListView(
                                songs = allSongs,
                                currentPlayingSongPath = currentSong?.path,
                                onPlaySong = { song -> viewModel.playSongFromList(song, allSongs) },
                                onSongLongClick = { viewModel.openSongContext(it) },
                                accentColor = accentColor
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
                    onPlayMoreFromArtist = { viewModel.playMoreFromArtist(currentSong.artist, currentSong) },
                    isArtistModeActive = activeArtistMode != null && currentSong.artist.trim().equals(activeArtistMode, ignoreCase = true),
                    onClick = { showFullscreenPlayer = true },
                    swipeSensitivity = swipeSensitivity
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
                lyricsData = lyricsData,
                showCoverLyrics = showCoverLyrics,
                onToggleCoverLyrics = { viewModel.toggleCoverLyrics() },
                lyricsFontSize = lyricsFontSize,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeekTo = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onToggleFavorite = { viewModel.toggleFavorite(currentSong) },
                onPlayMoreFromArtist = { viewModel.playMoreFromArtist(currentSong.artist, currentSong) },
                isArtistModeActive = activeArtistMode != null && currentSong.artist.trim().equals(activeArtistMode, ignoreCase = true),
                onOpenLyrics = { viewModel.openLyricsDialog() },
                onOpenEqualizer = { viewModel.openEqualizerDialog() },
                onOpenId3Info = { viewModel.openId3Dialog() },
                onOpenSettings = { viewModel.openSettingsDialog() },
                currentQueue = currentQueue,
                currentQueueIndex = currentQueueIndex,
                onPlayQueueIndex = { viewModel.playAtQueueIndex(it) },
                onReshuffleUpcoming = { viewModel.reshuffleUpcoming() },
                swipeSensitivity = swipeSensitivity,
                playbackSpeed = playbackSpeed,
                onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
                showWaveform = showWaveform,
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
                onBatchEditTags = { viewModel.openBatchId3Dialog(folder) },
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
                onPlayMoreFromArtist = { viewModel.playMoreFromArtist(song.artist, song) },
                isArtistModeActive = activeArtistMode != null && song.artist.trim().equals(activeArtistMode, ignoreCase = true),
                onSetAsRingtone = { RingtoneHelper.setAsRingtone(context, song) },
                onRemoveFromLibrary = { viewModel.hideSongFromLibrary(song) },
                onDeletePhysicalFile = { viewModel.deleteSong(song) },
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
                onDismiss = { viewModel.closeEqualizerDialog() },
                accentColor = accentColor
            )
        }

        // ID3 Şarkı Bilgisi Penceresi
        if (showId3Dialog && playerState.currentSong != null) {
            Id3InfoDialog(
                song = playerState.currentSong!!,
                onDismiss = { viewModel.closeId3Dialog() },
                onSave = { newFileName, title, artist, album, genre, year, artworkBytes, lyrics, deleteArtwork ->
                    viewModel.saveId3Tags(playerState.currentSong!!, newFileName, title, artist, album, genre, year, artworkBytes, lyrics, deleteArtwork)
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

        // Müzik Klasörleri Yönetim Penceresi
        if (showFolderSelectionDialog) {
            FolderSelectionDialog(
                storageRoots = storageRoots,
                includedFolders = includedFolders,
                onToggleFolder = { path, isInc -> viewModel.toggleFolderIncluded(path, isInc) },
                onRescan = { viewModel.rescanLibrary() },
                onDismissRequest = { viewModel.closeFolderSelectionDialog() }
            )
        }

        // Hakkında Penceresi (Logo tıklamasıyla açılır)
        if (showAboutDialog) {
            AboutDialog(
                onDismissRequest = { viewModel.closeAboutDialog() },
                accentColor = accentColor,
                libraryStats = libraryStats
            )
        }

        // Toplu ID3 Etiket Düzenleme Penceresi
        batchId3FolderItem?.let { folder ->
            BatchId3TagDialog(
                folderName = folder.name,
                folderPath = folder.path,
                songCount = folder.songCount,
                accentColor = accentColor,
                onSave = { artist, album, genre, year ->
                    viewModel.batchUpdateId3Tags(folder.path, artist, album, genre, year)
                },
                onDismiss = { viewModel.closeBatchId3Dialog() }
            )
        }

        // M3U Çalma Listesi İçe Aktarma Penceresi
        if (showM3uImportDialog) {
            M3uImportDialog(
                m3uFiles = availableM3uFiles,
                accentColor = accentColor,
                onSelectFile = { viewModel.importM3uFile(it) },
                onRefresh = { viewModel.scanM3uFiles() },
                onDismiss = { viewModel.closeM3uImportDialog() }
            )
        }

        // Aynı / Mükerrer Şarkıları Bulma Penceresi
        if (showDuplicateFinderDialog) {
            DuplicateFinderDialog(
                allSongs = allSongs,
                accentColor = accentColor,
                onDismissRequest = { viewModel.closeDuplicateFinderDialog() },
                onDeleteSong = { song, onComplete ->
                    viewModel.deleteSong(song, onComplete)
                },
                onDeleteMultipleSongs = { songs, onComplete ->
                    viewModel.deleteSongs(songs, onComplete)
                },
                onRenameSong = { song, newName, onComplete ->
                    viewModel.renameSong(song, newName, onComplete)
                },
                onPreviewSong = { song ->
                    viewModel.playSongFromList(song, allSongs)
                }
            )
        }
    }
}
