package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.R
import com.example.otomuzik.model.FolderItem
import com.example.otomuzik.model.Playlist
import com.example.otomuzik.model.Song





import com.example.otomuzik.theme.CarTextMuted


import com.example.otomuzik.ui.main.TabType
import java.io.File

@Composable
fun LibraryPanel(
    activeTab: TabType,
    onTabSelected: (TabType) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    // Klasör Gezgini
    storageRoots: List<FolderItem>,
    currentFolderPath: String?,
    currentFolderItems: List<FolderItem>,
    onOpenFolder: (String) -> Unit,
    onBackFolder: () -> Unit,
    onPlayFolder: (String) -> Unit,
    onPlayallSongs: () -> Unit = {},
    onFolderLongClick: (FolderItem) -> Unit,
    // Şarkı Listeleri
    filteredSongs: List<Song>,
    favoriteSongs: List<Song>,
    currentPlayingSongPath: String?,
    onPlaySong: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    // Çalma Listeleri
    Playlists: List<Playlist>,
    selectedPlaylist: Playlist?,
    selectedPlaylistSongs: List<Song>,
    onOpenPlaylist: (Playlist) -> Unit,
    onClosePlaylist: () -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onRemoveSongFromPlaylist: (Long, Song) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onExportPlaylist: ((Playlist) -> Unit)? = null,
    onImportM3uClick: (() -> Unit)? = null,
    // Çalma Sırası (Queue) Entegrasyonu
    isQueueVisible: Boolean = false,
    onCloseQueue: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    currentQueue: List<Song> = emptyList(),
    currentQueueIndex: Int = -1,
    isShuffle: Boolean = false,
    accentColor: Color = CarCyan,
    onPlayQueueIndex: (Int) -> Unit = {},
    onPlayNext: () -> Unit = {},
    onPlayPrevious: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onReshuffleUpcoming: () -> Unit = {},
    // Genel
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    isLoading: Boolean,
    showBlurredBackground: Boolean = false,
    currentSong: Song? = null,
    previousSong: Song? = null,
    nextSong: Song? = null,
    isPlaying: Boolean = false,
    onPlayPause: () -> Unit = {},
    onPlayFavorites: () -> Unit = {},
    onManageFolders: () -> Unit = {},
    isCarSearchOpen: Boolean = false,
    onOpenSearch: () -> Unit = {},
    onCloseSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    val panelBg = if (showBlurredBackground) {
        CarSurfaceDark.copy(alpha = 0.58f)
    } else {
        CarSurfaceDark
    }

    val panelShape = RoundedCornerShape(if (isCompact) 18.dp else 22.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = panelShape,
                spotColor = accentColor.copy(alpha = 0.35f),
                ambientColor = Color.Black
            )
            .clip(panelShape)
            .background(panelBg)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.40f),
                        accentColor.copy(alpha = 0.70f),
                        if (showBlurredBackground) CarBorder.copy(alpha = 0.85f) else CarBorder,
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = panelShape
            )
            .padding(if (isCompact) 8.dp else 12.dp)
    ) {
        // 3D Dışbükey Cam Yansıması (Glossy Sheen Overlay)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
        )
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (isQueueVisible || activeTab == TabType.QUEUE) {
            // Çalma Sırası Ekranı
            Column(modifier = Modifier.fillMaxSize()) {
                SubScreenHeader(
                    title = "Çalma Sırası (${currentQueue.size})",
                    onBack = {
                        onCloseQueue()
                        onTabSelected(TabType.HOME)
                    },
                    actions = {
                        CarHeaderActionButton(
                            icon = "🔀",
                            text = if (isShuffle) "Karışık Açık" else "Karıştır",
                            accentColor = if (isShuffle) accentColor else CarSurfaceVariant,
                            onClick = onToggleShuffle
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PlaybackQueueView(
                    queue = currentQueue,
                    currentIndex = currentQueueIndex,
                    isShuffle = isShuffle,
                    accentColor = accentColor,
                    onPlayAtIndex = {
                        onPlayQueueIndex(it)
                        onTabSelected(TabType.HOME)
                    },
                    onPlayNext = onPlayNext,
                    onPlayPrevious = onPlayPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onReshuffleUpcoming = onReshuffleUpcoming,
                    onClose = {
                        onCloseQueue()
                        onTabSelected(TabType.HOME)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        } else {
            // Ana Sürüş, Alt Sayfalar ve Arama Modu
            // NOT: FixedCarSearchBar bu sütunun en altında sabit kalır.
            // Böylece ilk karakter yazıldığında veya arama modu aktifleştiğinde
            // bileşen ağaçtan silinmez, klavye ve odak ASLA kaybolmaz!
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isCarSearchOpen || searchQuery.isNotEmpty()) {
                        // Arama Modu
                        Column(modifier = Modifier.fillMaxSize()) {
                            SubScreenHeader(
                                title = if (searchQuery.isEmpty()) "Arama Modu" else "Arama Sonuçları (${filteredSongs.size})",
                                onBack = {
                                    onCloseSearch()
                                    onSearchQueryChange("")
                                },
                                actions = {
                                    if (searchQuery.isNotEmpty()) {
                                        CarHeaderActionButton(
                                            icon = "✕",
                                            text = "Temizle",
                                            accentColor = CarSurfaceVariant,
                                            onClick = { onSearchQueryChange("") }
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                SearchResultsView(
                                    songs = if (searchQuery.isEmpty()) emptyList() else filteredSongs,
                                    query = searchQuery,
                                    currentPlayingSongPath = currentPlayingSongPath,
                                    onPlaySong = { song ->
                                        onPlaySong(song, filteredSongs)
                                        onCloseSearch()
                                        onSearchQueryChange("")
                                    },
                                    onSongLongClick = onSongLongClick
                                )
                            }
                        }
                    } else {
                        when (activeTab) {
                            TabType.HOME -> {
                                // ── ANA SÜRÜŞ DASHBOARD'U ──
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // 1. Üst Kısım: 6 Büyük Menü Butonu
                                    CarMenuGrid(
                                        queueCount = currentQueue.size,
                                        onNavigate = { onTabSelected(it) },
                                        onOpenSettings = onOpenSettings,
                                        accentColor = accentColor,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

                                    // 2. Orta Kısım: Önceki, Çalan ve Sonraki Şarkı Akış Kartı
                                    CarTrackFlowCard(
                                        currentSong = currentSong,
                                        previousSong = previousSong,
                                        nextSong = nextSong,
                                        isPlaying = isPlaying,
                                        accentColor = accentColor,
                                        onPlayPrevious = onPlayPrevious,
                                        onPlayNext = onPlayNext,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    )
                                }
                            }

                            TabType.FOLDERS -> {
                                // ── KLASÖR GEZGİNİ SAYFASI ──
                                val folderName = if (currentFolderPath != null) {
                                    storageRoots.find { it.path == currentFolderPath }?.name
                                        ?: File(currentFolderPath).name
                                } else "Klasör Gezgini"

                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubScreenHeader(
                                        title = folderName,
                                        onBack = {
                                            if (currentFolderPath != null) onBackFolder() else onTabSelected(TabType.HOME)
                                        },
                                        actions = {
                                            CarHeaderActionButton(
                                                icon = "🔄",
                                                text = "Tara",
                                                accentColor = CarSurfaceVariant,
                                                onClick = onRefresh
                                            )
                                            if (currentFolderPath != null) {
                                                CarHeaderActionButton(
                                                    icon = "📁",
                                                    text = "Klasörü Çal",
                                                    accentColor = accentColor,
                                                    onClick = {
                                                        onPlayFolder(currentFolderPath)
                                                        onTabSelected(TabType.HOME)
                                                    }
                                                )
                                            }
                                            CarHeaderActionButton(
                                                icon = "▶",
                                                text = "Tümünü Çal",
                                                accentColor = CarSurfaceVariant,
                                                onClick = {
                                                    onPlayallSongs()
                                                    onTabSelected(TabType.HOME)
                                                }
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        FolderBrowserView(
                                            storageRoots = storageRoots,
                                            currentFolderPath = currentFolderPath,
                                            currentFolderItems = currentFolderItems,
                                            currentPlayingSongPath = currentPlayingSongPath,
                                            onOpenFolder = onOpenFolder,
                                            onBackFolder = onBackFolder,
                                            onPlayFolder = {
                                                onPlayFolder(it)
                                                onTabSelected(TabType.HOME)
                                            },
                                            onPlayallSongs = {
                                                onPlayallSongs()
                                                onTabSelected(TabType.HOME)
                                            },
                                            onFolderLongClick = onFolderLongClick,
                                            onPlaySong = { song, list ->
                                                onPlaySong(song, list)
                                                onTabSelected(TabType.HOME)
                                            },
                                            onSongLongClick = onSongLongClick,
                                            onManageFolders = onManageFolders
                                        )
                                    }
                                }
                            }

                            TabType.ALL_SONGS -> {
                                // ── TÜM ŞARKILAR SAYFASI ──
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubScreenHeader(
                                        title = "Tüm Şarkılar (${filteredSongs.size})",
                                        onBack = { onTabSelected(TabType.HOME) },
                                        actions = {
                                            CarHeaderActionButton(
                                                icon = "🔄",
                                                text = "Tara",
                                                accentColor = CarSurfaceVariant,
                                                onClick = onRefresh
                                            )
                                            CarHeaderActionButton(
                                                icon = "▶",
                                                text = "Tümünü Çal",
                                                accentColor = accentColor,
                                                onClick = {
                                                    onPlayallSongs()
                                                    onTabSelected(TabType.HOME)
                                                }
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        SongListView(
                                            songs = filteredSongs,
                                            currentPlayingSongPath = currentPlayingSongPath,
                                            onPlaySong = { song ->
                                                onPlaySong(song, filteredSongs)
                                                onTabSelected(TabType.HOME)
                                            },
                                            onSongLongClick = onSongLongClick,
                                            accentColor = accentColor
                                        )
                                    }
                                }
                            }

                            TabType.PLAYLISTS -> {
                                // ── ÇALMA LİSTELERİ SAYFASI ──
                                val title = selectedPlaylist?.name ?: "Çalma Listeleri (${Playlists.size})"
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubScreenHeader(
                                        title = title,
                                        onBack = {
                                            if (selectedPlaylist != null) onClosePlaylist() else onTabSelected(TabType.HOME)
                                        },
                                        actions = {
                                            if (selectedPlaylist != null && selectedPlaylistSongs.isNotEmpty()) {
                                                CarHeaderActionButton(
                                                    icon = "▶",
                                                    text = "Listeyi Çal",
                                                    accentColor = accentColor,
                                                    onClick = {
                                                        onPlayPlaylist(selectedPlaylist)
                                                        onTabSelected(TabType.HOME)
                                                    }
                                                )
                                            } else {
                                                CarHeaderActionButton(
                                                    icon = "+",
                                                    text = "Yeni Liste",
                                                    accentColor = accentColor,
                                                    onClick = onCreatePlaylistClick
                                                )
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        PlaylistsView(
                                            Playlists = Playlists,
                                            selectedPlaylist = selectedPlaylist,
                                            selectedPlaylistSongs = selectedPlaylistSongs,
                                            currentPlayingSongPath = currentPlayingSongPath,
                                            onOpenPlaylist = onOpenPlaylist,
                                            onClosePlaylist = onClosePlaylist,
                                            onPlayPlaylist = {
                                                onPlayPlaylist(it)
                                                onTabSelected(TabType.HOME)
                                            },
                                            onDeletePlaylist = onDeletePlaylist,
                                            onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                                            onCreatePlaylistClick = onCreatePlaylistClick,
                                            onPlaySong = { song ->
                                                onPlaySong(song, selectedPlaylistSongs)
                                                onTabSelected(TabType.HOME)
                                            },
                                            onSongLongClick = onSongLongClick,
                                            onExportPlaylist = onExportPlaylist,
                                            onImportM3uClick = onImportM3uClick
                                        )
                                    }
                                }
                            }

                            TabType.FAVORITES -> {
                                // ── FAVORİLER SAYFASI ──
                                Column(modifier = Modifier.fillMaxSize()) {
                                    SubScreenHeader(
                                        title = "Favoriler (${favoriteSongs.size})",
                                        onBack = { onTabSelected(TabType.HOME) },
                                        actions = {
                                            if (favoriteSongs.isNotEmpty()) {
                                                CarHeaderActionButton(
                                                    icon = "▶",
                                                    text = "Favorileri Çal",
                                                    accentColor = CarAmber,
                                                    onClick = {
                                                        onPlayFavorites()
                                                        onTabSelected(TabType.HOME)
                                                    }
                                                )
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        SongListView(
                                            songs = favoriteSongs,
                                            currentPlayingSongPath = currentPlayingSongPath,
                                            onPlaySong = { song ->
                                                onPlaySong(song, favoriteSongs)
                                                onTabSelected(TabType.HOME)
                                            },
                                            onSongLongClick = onSongLongClick,
                                            accentColor = accentColor,
                                            emptyMessage = "Henüz favori şarkı eklenmedi.\nŞarkı çalarken kalp simgesine dokunun veya basılı tutarak ekleyin."
                                        )
                                    }
                                }
                            }

                            TabType.QUEUE -> {
                                // QUEUE durumu üstteki if (isQueueVisible || activeTab == TabType.QUEUE) içinde ele alınmaktadır
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))

                // Sabit ve kalıcı arama çubuğu: Recomposition veya sayfa/arama değişse dahi
                // TextField ağaçtan silinmediği için klavye ve odak asla kaybolmaz!
                FixedCarSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    resultCount = if (searchQuery.isNotEmpty()) filteredSongs.size else null,
                    onSearchBarClick = onOpenSearch,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CarMenuGrid(
    queueCount: Int,
    onNavigate: (TabType) -> Unit,
    onOpenSettings: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    // %50 Büyütülmüş hızlı sürüş ve tablet butonları
    val btnHeight = if (isCompact) 115.dp else 145.dp
    val gridSpacing = if (isCompact) 10.dp else 14.dp

    val cyanBorder = Color(0xFF00E5FF)
    val amberBorder = Color(0xFFFFD54F)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gridSpacing)
    ) {
        // Satır 1: Klasörler, Şarkılar, Listeler
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing)
        ) {
            CarBigMenuButton(
                icon = "📁",
                title = "Klasörler",
                borderColor = cyanBorder,
                onClick = { onNavigate(TabType.FOLDERS) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
            CarBigMenuButton(
                icon = "🎵",
                title = "Şarkılar",
                borderColor = cyanBorder,
                onClick = { onNavigate(TabType.ALL_SONGS) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
            CarBigMenuButton(
                icon = "📑",
                title = "Listeler",
                borderColor = amberBorder,
                onClick = { onNavigate(TabType.PLAYLISTS) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
        }

        // Satır 2: Favoriler, Çalma Sırası, Ayarlar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing)
        ) {
            CarBigMenuButton(
                icon = "🩵",
                title = "Favoriler",
                borderColor = cyanBorder,
                onClick = { onNavigate(TabType.FAVORITES) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
            CarBigMenuButton(
                icon = "🎧",
                title = if (queueCount > 0) "Sıra ($queueCount)" else "Sıra",
                borderColor = amberBorder,
                onClick = { onNavigate(TabType.QUEUE) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
            CarBigMenuButton(
                icon = "⚙️",
                title = "Ayarlar",
                borderColor = cyanBorder,
                onClick = onOpenSettings,
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
        }
    }
}

@Composable
private fun CarBigMenuButton(
    icon: String,
    title: String,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val shape = RoundedCornerShape(if (isCompact) 22.dp else 28.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isCompact) 6.dp else 10.dp,
                shape = shape,
                spotColor = borderColor.copy(alpha = 0.55f),
                ambientColor = Color.Black
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF222A3B), Color(0xFF10141F), Color(0xFF090C13))
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.50f),
                        borderColor,
                        borderColor.copy(alpha = 0.45f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = shape
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Üst 3D Cam Parlama (Glossy Sheen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 46.dp else 58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(borderColor.copy(alpha = 0.30f), Color.Transparent),
                            radius = 120f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = if (isCompact) 30.sp else 38.sp
                )
            }
            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
            Text(
                text = title,
                fontSize = if (isCompact) 17.sp else 21.sp,
                fontWeight = FontWeight.Bold,
                color = CarTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CarTrackFlowCard(
    currentSong: Song?,
    previousSong: Song?,
    nextSong: Song?,
    isPlaying: Boolean,
    accentColor: Color,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val cardShape = RoundedCornerShape(if (isCompact) 20.dp else 26.dp)
    val neonCyan = Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isCompact) 8.dp else 12.dp,
                shape = cardShape,
                spotColor = neonCyan.copy(alpha = 0.40f),
                ambientColor = Color.Black
            )
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E2638), Color(0xFF0F1420), Color(0xFF07090F))
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        neonCyan,
                        neonCyan.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = cardShape
            )
            .padding(if (isCompact) 8.dp else 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
        ) {
        // 1. ÖNCEKİ PARÇA
        TrackFlowRow(
            leadSymbol = "⏮",
            trailSymbol = "⏭",
            song = previousSong,
            emptyText = "Listenin başındasınız",
            isCurrent = false,
            onClick = if (previousSong != null) onPlayPrevious else null
        )

        // 2. ŞU AN ÇALAN PARÇA (Vurgulu & Amber)
        TrackFlowRow(
            leadSymbol = "❮",
            trailSymbol = if (isPlaying) "🔊" else "⏸",
            song = currentSong,
            emptyText = "Henüz parça seçilmedi",
            isCurrent = true,
            onClick = null
        )

        // 3. SIRADAKİ PARÇA
        TrackFlowRow(
            leadSymbol = "❯",
            trailSymbol = "⏭",
            song = nextSong,
            emptyText = "Sıranın sonundasınız",
            isCurrent = false,
            onClick = if (nextSong != null) onPlayNext else null
        )
    }
}
}

@Composable
private fun TrackFlowRow(
    leadSymbol: String,
    trailSymbol: String,
    song: Song?,
    emptyText: String,
    isCurrent: Boolean,
    onClick: (() -> Unit)?
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val rowShape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp)

    val rowBg = if (isCurrent) {
        Color(0xFFFFB300).copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    val rowBorder = if (isCurrent) {
        Color(0xFFFFD54F).copy(alpha = 0.40f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 48.dp else 58.dp)
            .clip(rowShape)
            .background(rowBg)
            .border(if (isCurrent) 1.dp else 0.dp, rowBorder, rowShape)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = if (isCompact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol sembol (❮ / ❯ / ⏮)
        Text(
            text = leadSymbol,
            fontSize = if (isCompact) 14.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrent) Color(0xFFFFD54F) else CarTextMuted
        )

        Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))

        // Albüm Kapağı Minyatürü
        Box(
            modifier = Modifier
                .size(if (isCompact) 32.dp else 38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎵",
                fontSize = if (isCompact) 16.sp else 20.sp
            )
        }

        Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))

        // Parça Başlığı ve Sanatçı
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (song != null) "${com.example.otomuzik.util.TurkishStringFixer.fix(song.title)} - ${com.example.otomuzik.util.TurkishStringFixer.fix(song.artist)}" else emptyText,
                fontSize = if (isCompact) 14.sp else 16.sp,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isCurrent) Color(0xFFFFD54F) else (if (song != null) CarTextPrimary else CarTextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Sağ sembol (🔊 / ⏭)
        Text(
            text = trailSymbol,
            fontSize = if (isCompact) 14.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrent) Color(0xFFFFD54F) else CarTextMuted
        )
    }
}

@Composable
private fun SubScreenHeader(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPhoneLayout = configuration.screenWidthDp < 600
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 44.dp else 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(if (isCompact) 10.dp else 14.dp))
                .background(CarCyan.copy(alpha = 0.18f))
                .border(1.5.dp, CarCyan, RoundedCornerShape(if (isCompact) 10.dp else 14.dp))
                .clickable { onBack() }
                .padding(horizontal = if (isPhoneLayout) 10.dp else (if (isCompact) 12.dp else 16.dp), vertical = if (isCompact) 6.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = if (isCompact) 16.sp else 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CarCyan
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isPhoneLayout) "Geri" else "Ana Menü",
                fontSize = if (isCompact) 12.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = CarCyan
            )
        }

        Text(
            text = title,
            fontSize = if (isPhoneLayout) 14.sp else (if (isCompact) 15.sp else 18.sp),
            fontWeight = FontWeight.ExtraBold,
            color = CarTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
        }
    }
}

@Composable
private fun CarHeaderActionButton(
    icon: String,
    text: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
            .background(accentColor)
            .clickable { onClick() }
            .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = if (isCompact) 13.sp else 15.sp,
            color = Color(0xFF0C0E12)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = if (isCompact) 12.sp else 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0C0E12)
        )
    }
}

@Composable
internal fun FixedCarSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int? = null,
    onSearchBarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val pillShape = RoundedCornerShape(50)
    val neonCyan = Color(0xFF00E5FF)
    val amberGold = Color(0xFFFFD54F)

    Row(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = pillShape, spotColor = neonCyan.copy(alpha = 0.35f))
            .height(if (isCompact) 52.dp else 64.dp)
            .clip(pillShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF182030), Color(0xFF0C101A), Color(0xFF06080E))
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.50f),
                        neonCyan,
                        neonCyan.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                ),
                shape = pillShape
            )
            .then(
                if (onSearchBarClick != null) Modifier.clickable { onSearchBarClick() }
                else Modifier
            )
            .padding(horizontal = if (isCompact) 16.dp else 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_search),
            contentDescription = "Ara",
            tint = amberGold,
            modifier = Modifier.size(if (isCompact) 24.dp else 28.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        if (onSearchBarClick != null) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Ara (şarkı, sanatçı veya klasör)...",
                        color = CarTextMuted,
                        fontSize = if (isCompact) 15.sp else 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = query,
                        color = CarTextPrimary,
                        fontSize = if (isCompact) 16.sp else 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = CarTextPrimary,
                    fontSize = if (isCompact) 16.sp else 19.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(neonCyan),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Ara (şarkı, sanatçı veya klasör)...",
                            color = CarTextMuted,
                            fontSize = if (isCompact) 15.sp else 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Bulunan sonuç adedi rozeti
        if (resultCount != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2800E5FF))
                    .border(0.5.dp, CarCyan, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$resultCount parça",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarCyan
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Temizle Butonu (Büyük Dokunmatik Hedef)
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarTextPrimary
                )
            }
        }
    }
}

@Composable
internal fun SearchResultsView(
    songs: List<Song>,
    query: String,
    currentPlayingSongPath: String?,
    onPlaySong: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (query.isEmpty()) "⌨️" else "🔍",
                    fontSize = 38.sp
                )
                Text(
                    text = if (query.isEmpty())
                        "Aramak istediğiniz şarkı, sanatçı veya klasörü\nsanal klavyeden yazın."
                    else
                        "\"$query\" aramasıyla eşleşen parça bulunamadı",
                    color = CarTextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Arama sonucu: ${songs.size} şarkı bulundu",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CarTextMuted,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp, top = 4.dp)
                )
            }
            itemsIndexed(songs) { index, song ->
                val isPlaying = song.path == currentPlayingSongPath
                SongItemRow(
                    song = song,
                    index = index + 1,
                    isPlaying = isPlaying,
                    onClick = { onPlaySong(song) },
                    onLongClick = { onSongLongClick(song) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderBrowserView(
    storageRoots: List<FolderItem>,
    currentFolderPath: String?,
    currentFolderItems: List<FolderItem>,
    currentPlayingSongPath: String?,
    onOpenFolder: (String) -> Unit,
    onBackFolder: () -> Unit,
    onPlayFolder: (String) -> Unit,
    onPlayallSongs: () -> Unit,
    onFolderLongClick: (FolderItem) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onManageFolders: () -> Unit = {}
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPhoneLayout = configuration.screenWidthDp < 600 || configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Klasöre Dön ve "Bu Klasörü Çal" Başlığı
        if (currentFolderPath != null) {
            val folderName = storageRoots.find { it.path == currentFolderPath }?.name
                ?: File(currentFolderPath).name
            if (isPhoneLayout) {
                // Telefonda kompakt dikey düzen: Üstte Geri + Klasör Adı, altta Çal butonları
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBackFolder() }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(CarSurfaceVariant, RoundedCornerShape(10.dp))
                                .border(1.dp, CarBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = "Geri",
                                tint = CarCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = folderName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPlayallSongs,
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(
                                text = "▶ Tümünü Çal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarCyan
                            )
                        }

                        Button(
                            onClick = { onPlayFolder(currentFolderPath) },
                            colors = ButtonDefaults.buttonColors(containerColor = CarCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(
                                text = "▶ Klasörü Çal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0C0E12)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onBackFolder() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(CarSurfaceVariant, RoundedCornerShape(12.dp))
                                .border(1.dp, CarBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = "Geri",
                                tint = CarCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = folderName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextPrimary,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Butonlar Grubu
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onPlayallSongs,
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = "▶ Tümünü Çal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarCyan
                            )
                        }

                        Button(
                            onClick = { onPlayFolder(currentFolderPath) },
                            colors = ButtonDefaults.buttonColors(containerColor = CarCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = "▶ Klasörü Çal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0C0E12)
                            )
                        }
                    }
                }
            }
        }

        // Klasr veya Depolama Listesi
        val itemsToDisplay = if (currentFolderPath == null) storageRoots else currentFolderItems

        if (itemsToDisplay.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentFolderPath == null) "Depolama veya USB cihazı bulunamadı" else "Bu Klasörde Müzik dosyası yok",
                    color = CarTextMuted,
                    fontSize = 16.sp
                )
            }
        } else {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
            ) {
                if (currentFolderPath == null) {
                    item {
                        val shape = RoundedCornerShape(if (isPhoneLayout) 12.dp else 16.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(CarSurfaceDark)
                                .border(1.dp, CarCyan.copy(alpha = 0.5f), shape)
                                .clickable { onManageFolders() }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_folder),
                                        contentDescription = null,
                                        tint = CarCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Müzik Klasörlerini Yönet",
                                            fontSize = if (isPhoneLayout) 14.sp else 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CarTextPrimary
                                        )
                                        Text(
                                            text = "Taranacak dizinleri seç / Download klasörünü gizle",
                                            fontSize = 11.sp,
                                            color = CarTextMuted
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CarCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Yönet",
                                        color = CarCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                items(itemsToDisplay) { item ->
                    if (item.isDirectory) {
                        // Klasör Satırı - Telefonda ve araçta dengeli yükseklik ve boşluk
                        val rowHeight = if (isPhoneLayout) 58.dp else if (isCompact) 62.dp else 74.dp
                        val iconSize = if (isPhoneLayout) 26.dp else if (isCompact) 28.dp else 34.dp
                        val cornerRadius = if (isPhoneLayout) 12.dp else if (isCompact) 14.dp else 16.dp

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(CarSurfaceVariant)
                                .border(1.dp, CarBorder, RoundedCornerShape(cornerRadius))
                                .combinedClickable(
                                    onClick = { onOpenFolder(item.path) },
                                    onLongClick = { onFolderLongClick(item) }
                                )
                                .padding(horizontal = if (isPhoneLayout) 12.dp else if (isCompact) 12.dp else 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (item.isStorageRoot && item.name.contains("USB")) R.drawable.ic_usb else R.drawable.ic_folder
                                ),
                                contentDescription = null,
                                tint = if (item.isStorageRoot) CarAmber else CarCyan,
                                modifier = Modifier.size(iconSize)
                            )

                            Spacer(modifier = Modifier.width(if (isPhoneLayout) 10.dp else if (isCompact) 12.dp else 16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = if (isPhoneLayout) 15.sp else if (isCompact) 16.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.songCount > 0) {
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${item.songCount} Müzik parçası",
                                        fontSize = if (isPhoneLayout) 11.5.sp else 13.sp,
                                        color = CarTextMuted
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "Klasör",
                                        fontSize = if (isPhoneLayout) 11.sp else 12.sp,
                                        color = CarTextMuted.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else if (item.song != null) {
                        val song = item.song
                        val isPlaying = song.path == currentPlayingSongPath
                        val songList = itemsToDisplay.mapNotNull { it.song }

                        SongItemRow(
                            song = song,
                            isPlaying = isPlaying,
                            onClick = { onPlaySong(song, songList) },
                            onLongClick = { onSongLongClick(song) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ÇÇalma Listeileri Grnmü (tm ListeLER & Seili Listenin içeriği).
 */
@Composable
internal fun PlaylistsView(
    Playlists: List<Playlist>,
    selectedPlaylist: Playlist?,
    selectedPlaylistSongs: List<Song>,
    currentPlayingSongPath: String?,
    onOpenPlaylist: (Playlist) -> Unit,
    onClosePlaylist: () -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onRemoveSongFromPlaylist: (Long, Song) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onExportPlaylist: ((Playlist) -> Unit)? = null,
    onImportM3uClick: (() -> Unit)? = null
) {
    if (selectedPlaylist != null) {
        // Seili ÇÇalma Listesi Detay Ekranı
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClosePlaylist() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(CarSurfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, CarBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Geri",
                            tint = CarCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = selectedPlaylist.name,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedPlaylistSongs.size} Müzik parçası",
                            fontSize = 13.sp,
                            color = CarTextMuted
                        )
                    }
                }

                if (selectedPlaylistSongs.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onExportPlaylist != null) {
                            IconButton(
                                onClick = { onExportPlaylist(selectedPlaylist) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(CarSurfaceVariant, RoundedCornerShape(12.dp))
                                    .border(1.dp, CarBorder, RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = "M3U",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarCyan
                                )
                            }
                        }
                        Button(
                            onClick = { onPlayPlaylist(selectedPlaylist) },
                            colors = ButtonDefaults.buttonColors(containerColor = CarCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = "▶ Listeyi Çal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0C0E12)
                            )
                        }
                    }
                }
            }

            if (selectedPlaylistSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bu Listede Henüz şarkı yok.\nKlasörlere veya Şarkılara basılı tutarak bu listeye ekleyebilirsiniz.",
                        color = CarTextMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(selectedPlaylistSongs) { index, song ->
                        val isPlaying = song.path == currentPlayingSongPath
                        SongItemRow(
                            song = song,
                            index = index + 1,
                            isPlaying = isPlaying,
                            onClick = { onPlaySong(song) },
                            onLongClick = { onSongLongClick(song) },
                            trailingaction = {
                                if (selectedPlaylist.id > 0) {
                                    IconButton(
                                        onClick = { onRemoveSongFromPlaylist(selectedPlaylist.id, song) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_delete),
                                            contentDescription = "Listeden Kaldır",
                                            tint = CarTextMuted,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    } else {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

        // Çalma Listeleri Genel Listesi
        Column(modifier = Modifier.fillMaxSize()) {
            // ➕ Yeni Çalma Listesi Oluştur & 📥 M3U İçe Aktar Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCompact) 52.dp else 60.dp)
                        .clip(RoundedCornerShape(if (isCompact) 12.dp else 16.dp))
                        .background(Color(0x2800E5FF))
                        .border(1.5.dp, CarCyan, RoundedCornerShape(if (isCompact) 12.dp else 16.dp))
                        .clickable { onCreatePlaylistClick() }
                        .padding(horizontal = if (isCompact) 10.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 32.dp else 36.dp)
                            .clip(CircleShape)
                            .background(CarCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            tint = Color(0xFF0C0E12),
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))

                    Text(
                        text = "Yeni Liste",
                        fontSize = if (isCompact) 14.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CarCyan
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCompact) 52.dp else 60.dp)
                        .clip(RoundedCornerShape(if (isCompact) 12.dp else 16.dp))
                        .background(Color(0x28FFB300))
                        .border(1.5.dp, CarAmber, RoundedCornerShape(if (isCompact) 12.dp else 16.dp))
                        .clickable { onImportM3uClick?.invoke() }
                        .padding(horizontal = if (isCompact) 10.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 32.dp else 36.dp)
                            .clip(CircleShape)
                            .background(CarAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_playlist),
                            contentDescription = null,
                            tint = Color(0xFF0C0E12),
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))

                    Text(
                        text = "M3U Aktar",
                        fontSize = if (isCompact) 14.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CarAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

            if (Playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz oluşturulmuş bir çalma listesi yok.\nYukarıdaki düğmeye basarak oluşturabilir veya\nklasörlere basılı tutarak liste yaratabilirsiniz.",
                        color = CarTextMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
                ) {
                    items(Playlists) { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isCompact) 62.dp else 74.dp)
                                .clip(RoundedCornerShape(if (isCompact) 14.dp else 16.dp))
                                .background(CarSurfaceVariant)
                                .border(1.dp, CarBorder, RoundedCornerShape(if (isCompact) 14.dp else 16.dp))
                                .clickable { onOpenPlaylist(pl) }
                                .padding(horizontal = if (isCompact) 12.dp else 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 38.dp else 44.dp)
                                    .clip(RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
                                    .background(Color(0x28FFB300)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_playlist),
                                    contentDescription = null,
                                    tint = CarAmber,
                                    modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(if (isCompact) 12.dp else 16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pl.name,
                                    fontSize = if (isCompact) 16.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${pl.songCount} parça",
                                    fontSize = if (isCompact) 12.sp else 14.sp,
                                    color = CarTextMuted
                                )
                            }

                            // Hızlı Oynat Butonu
                            if (pl.songCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompact) 40.dp else 46.dp)
                                        .clip(CircleShape)
                                        .background(CarCyan)
                                        .clickable { onPlayPlaylist(pl) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_play),
                                        contentDescription = "Oynat",
                                        tint = Color(0xFF0C0E12),
                                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // Sil Butonu
                            if (pl.id > 0) {
                                IconButton(
                                    onClick = { onDeletePlaylist(pl) },
                                    modifier = Modifier
                                        .size(if (isCompact) 40.dp else 46.dp)
                                        .background(CarSurfaceDark, RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
                                        .border(1.dp, CarBorder, RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_delete),
                                        contentDescription = "Listeyi Sil",
                                        tint = CarTextMuted,
                                        modifier = Modifier.size(if (isCompact) 20.dp else 22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SongListView(
    songs: List<Song>,
    currentPlayingSongPath: String?,
    onPlaySong: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    accentColor: Color = CarCyan,
    emptyMessage: String = "Müzik dosyası bulunamadı"
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                color = CarTextMuted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
        val listState = rememberLazyListState()

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (songs.size >= 12) 30.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
            ) {
                item {
                    Text(
                        text = "Toplam ${songs.size} şarkı listeleniyor",
                        fontSize = if (isCompact) 12.sp else 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CarTextMuted,
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp, top = 4.dp)
                    )
                }
                itemsIndexed(songs) { index, song ->
                    val isPlaying = song.path == currentPlayingSongPath
                    SongItemRow(
                        song = song,
                        index = index + 1,
                        isPlaying = isPlaying,
                        onClick = { onPlaySong(song) },
                        onLongClick = { onSongLongClick(song) }
                    )
                }
            }

            // Alfabetik Hızlı Kaydırma Çubuğu (A-Z)
            if (songs.size >= 12) {
                AlphabetFastScroller(
                    songs = songs,
                    listState = listState,
                    accentColor = accentColor,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp, top = 8.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItemRow(
    song: Song,
    index: Int? = null,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailingaction: (@Composable () -> Unit)? = null
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPhoneLayout = configuration.screenWidthDp < 600 || configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val cardShape = RoundedCornerShape(if (isPhoneLayout) 12.dp else if (isCompact) 14.dp else 18.dp)
    val rowHeight = if (isPhoneLayout) 60.dp else if (isCompact) 66.dp else 78.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clip(cardShape)
            .background(if (isPlaying) Color(0x2800E5FF) else CarSurfaceVariant)
            .border(
                width = if (isPlaying) 1.8.dp else 1.dp,
                color = if (isPlaying) CarCyan else CarBorder,
                shape = cardShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = if (isPhoneLayout) 12.dp else if (isCompact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(if (isPhoneLayout) 32.dp else if (isCompact) 36.dp else 42.dp)
                    .clip(CircleShape)
                    .background(CarCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_music_note),
                    contentDescription = "Çalıyor",
                    tint = Color(0xFF0C0E12),
                    modifier = Modifier.size(if (isPhoneLayout) 18.dp else if (isCompact) 22.dp else 26.dp)
                )
            }
        } else if (index != null) {
            Box(
                modifier = Modifier
                    .size(if (isPhoneLayout) 28.dp else if (isCompact) 32.dp else 36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E222B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%02d".format(index),
                    fontSize = if (isPhoneLayout) 11.sp else if (isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarTextMuted
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = null,
                tint = CarTextMuted,
                modifier = Modifier.size(if (isPhoneLayout) 20.dp else if (isCompact) 24.dp else 28.dp)
            )
        }

        Spacer(modifier = Modifier.width(if (isPhoneLayout) 10.dp else if (isCompact) 12.dp else 16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = com.example.otomuzik.util.TurkishStringFixer.fix(song.title),
                fontSize = if (isPhoneLayout) 14.5.sp else if (isCompact) 16.sp else 19.sp,
                fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isPlaying) CarCyan else CarTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = com.example.otomuzik.util.TurkishStringFixer.fix(song.artist),
                fontSize = if (isPhoneLayout) 12.sp else if (isCompact) 13.sp else 15.sp,
                color = if (isPlaying) CarCyan.copy(alpha = 0.85f) else CarTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (song.durationMs > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = song.durationFormatted,
                fontSize = if (isPhoneLayout) 11.5.sp else if (isCompact) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPlaying) CarCyan else CarTextMuted
            )
        }

        if (trailingaction != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingaction()
        }
    }
}




















