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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    val panelBg = if (showBlurredBackground) {
        CarSurfaceDark.copy(alpha = 0.58f)
    } else {
        CarSurfaceDark
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 18.dp else 22.dp))
            .background(panelBg)
            .border(1.5.dp, if (showBlurredBackground) CarBorder.copy(alpha = 0.85f) else CarBorder, RoundedCornerShape(if (isCompact) 18.dp else 22.dp))
            .padding(if (isCompact) 8.dp else 12.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (searchQuery.isNotEmpty()) {
            // Arama Modu
            Column(modifier = Modifier.fillMaxSize()) {
                SubScreenHeader(
                    title = "Arama Sonuçları",
                    onBack = { onSearchQueryChange("") },
                    actions = {
                        CarHeaderActionButton(
                            icon = "✕",
                            text = "Temizle",
                            accentColor = CarSurfaceVariant,
                            onClick = { onSearchQueryChange("") }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    SearchResultsView(
                        songs = filteredSongs,
                        query = searchQuery,
                        currentPlayingSongPath = currentPlayingSongPath,
                        onPlaySong = { song ->
                            onPlaySong(song, filteredSongs)
                            onSearchQueryChange("")
                        },
                        onSongLongClick = onSongLongClick
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FixedCarSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    resultCount = filteredSongs.size,
                    modifier = Modifier.fillMaxWidth()
                )
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
            // Ana Sürüş ve Alt Sayfalar
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

                        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

                        // 3. Alt Kısım: Büyük Sürüş Arama Çubuğu
                        FixedCarSearchBar(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            resultCount = null,
                            modifier = Modifier.fillMaxWidth()
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
                                onSongLongClick = onSongLongClick
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FixedCarSearchBar(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            resultCount = null,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                onSongLongClick = onSongLongClick
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FixedCarSearchBar(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            resultCount = null,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                onSongLongClick = onSongLongClick
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FixedCarSearchBar(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            resultCount = null,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                emptyMessage = "Henüz favori şarkı eklenmedi.\nŞarkı çalarken kalp simgesine dokunun veya basılı tutarak ekleyin."
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FixedCarSearchBar(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            resultCount = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                TabType.QUEUE -> {
                    // QUEUE durumu üstteki if (isQueueVisible || activeTab == TabType.QUEUE) içinde ele alınmaktadır
                }
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
    // %50 Büyütülmüş hızlı sürüş butonları (Sürüş esnasında kolay dokunma için)
    val btnHeight = if (isCompact) 80.dp else 98.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
    ) {
        // Satır 1: Klasörler, Şarkılar, Listeler
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
        ) {
            CarBigMenuButton(
                icon = "📁",
                title = "Klasörler",
                subtitle = "Klasör Seç",
                accentColor = accentColor,
                onClick = { onNavigate(TabType.FOLDERS) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
            CarBigMenuButton(
                icon = "🎵",
                title = "Şarkılar",
                subtitle = "Tüm Liste",
                accentColor = accentColor,
                onClick = { onNavigate(TabType.ALL_SONGS) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
            CarBigMenuButton(
                icon = "📑",
                title = "Listeler",
                subtitle = "Çalma Listesi",
                accentColor = accentColor,
                onClick = { onNavigate(TabType.PLAYLISTS) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
        }

        // Satır 2: Favoriler, Çalma Sırası, Ayarlar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
        ) {
            CarBigMenuButton(
                icon = "❤️",
                title = "Favoriler",
                subtitle = "Beğenilenler",
                accentColor = CarAmber,
                onClick = { onNavigate(TabType.FAVORITES) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
            CarBigMenuButton(
                icon = "🎧",
                title = if (queueCount > 0) "Sıra ($queueCount)" else "Sıra",
                subtitle = "Çalma Sırası",
                accentColor = accentColor,
                onClick = { onNavigate(TabType.QUEUE) },
                modifier = Modifier
                    .weight(1f)
                    .height(btnHeight)
            )
            CarBigMenuButton(
                icon = "⚙️",
                title = "Ayarlar",
                subtitle = "Yapılandırma",
                accentColor = CarTextSecondary,
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
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val shape = RoundedCornerShape(if (isCompact) 16.dp else 22.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(CarSurfaceVariant)
            .border(1.5.dp, CarBorder, shape)
            .clickable { onClick() }
            .padding(horizontal = if (isCompact) 8.dp else 14.dp, vertical = if (isCompact) 6.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = icon,
                fontSize = if (isCompact) 28.sp else 36.sp
            )
            Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = if (isCompact) 17.sp else 22.sp,
                    fontWeight = FontWeight.Black,
                    color = CarTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isCompact) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CarTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isCompact) 14.dp else 18.dp))
            .background(CarSurfaceDark.copy(alpha = 0.70f))
            .border(1.2.dp, CarBorder, RoundedCornerShape(if (isCompact) 14.dp else 18.dp))
            .padding(if (isCompact) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
    ) {
        // 1. ÖNCEKİ PARÇA
        TrackFlowRow(
            label = "ÖNCEKİ PARÇA",
            song = previousSong,
            emptyText = "Listenin başındasınız",
            iconRes = R.drawable.ic_skip_previous,
            accentColor = accentColor,
            isCurrent = false,
            onClick = if (previousSong != null) onPlayPrevious else null
        )

        // 2. ŞU AN ÇALAN PARÇA (Vurgulu)
        TrackFlowRow(
            label = if (isPlaying) "ŞU AN ÇALIYOR" else "DURAKLATILDI",
            song = currentSong,
            emptyText = "Henüz parça seçilmedi",
            iconRes = if (isPlaying) R.drawable.ic_play else R.drawable.ic_pause,
            accentColor = accentColor,
            isCurrent = true,
            onClick = null
        )

        // 3. SIRADAKİ PARÇA
        TrackFlowRow(
            label = "SIRADAKİ PARÇA",
            song = nextSong,
            emptyText = "Sıranın sonundasınız",
            iconRes = R.drawable.ic_skip_next,
            accentColor = accentColor,
            isCurrent = false,
            onClick = if (nextSong != null) onPlayNext else null
        )
    }
}

@Composable
private fun TrackFlowRow(
    label: String,
    song: Song?,
    emptyText: String,
    iconRes: Int,
    accentColor: Color,
    isCurrent: Boolean,
    onClick: (() -> Unit)?
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val rowShape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp)

    val rowBg = if (isCurrent) {
        accentColor.copy(alpha = 0.16f)
    } else {
        CarSurfaceVariant.copy(alpha = 0.70f)
    }

    val rowBorder = if (isCurrent) {
        accentColor.copy(alpha = 0.85f)
    } else {
        CarBorder.copy(alpha = 0.50f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 50.dp else 60.dp)
            .clip(rowShape)
            .background(rowBg)
            .border(if (isCurrent) 1.5.dp else 1.dp, rowBorder, rowShape)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = if (isCompact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 34.dp else 40.dp)
                .clip(CircleShape)
                .background(if (isCurrent) accentColor else CarSurfaceDark)
                .border(1.dp, if (isCurrent) accentColor else CarBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = if (isCurrent) Color(0xFF0C0E12) else (if (song != null) CarTextPrimary else CarTextMuted),
                modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
            )
        }

        Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = if (isCompact) 10.sp else 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCurrent) accentColor else CarTextMuted,
                letterSpacing = 1.sp
            )
            Text(
                text = if (song != null) "${song.title} • ${song.artist}" else emptyText,
                fontSize = if (isCompact) 13.sp else 15.sp,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else (if (song != null) FontWeight.SemiBold else FontWeight.Normal),
                color = if (song != null) CarTextPrimary else CarTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onClick != null && song != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CarSurfaceDark)
                    .border(0.5.dp, CarBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Geç ➔",
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun SubScreenHeader(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 46.dp else 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(if (isCompact) 10.dp else 14.dp))
                .background(CarCyan.copy(alpha = 0.18f))
                .border(1.5.dp, CarCyan, RoundedCornerShape(if (isCompact) 10.dp else 14.dp))
                .clickable { onBack() }
                .padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = if (isCompact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = if (isCompact) 18.sp else 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CarCyan
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Ana Menü",
                fontSize = if (isCompact) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = CarCyan
            )
        }

        Text(
            text = title,
            fontSize = if (isCompact) 15.sp else 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CarTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
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
private fun FixedCarSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int? = null,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    Row(
        modifier = modifier
            .height(if (isCompact) 52.dp else 64.dp)
            .clip(RoundedCornerShape(if (isCompact) 14.dp else 18.dp))
            .background(CarSurfaceVariant)
            .border(
                width = if (query.isNotEmpty()) 2.dp else 1.2.dp,
                color = if (query.isNotEmpty()) CarCyan else CarBorder,
                shape = RoundedCornerShape(if (isCompact) 14.dp else 18.dp)
            )
            .padding(horizontal = if (isCompact) 14.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_search),
            contentDescription = "ara",
            tint = if (query.isNotEmpty()) CarCyan else CarTextMuted,
            modifier = Modifier.size(if (isCompact) 26.dp else 30.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = CarTextPrimary,
                fontSize = if (isCompact) 16.sp else 19.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(CarCyan),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "Şarkı, Sanatçı veya Albüm ara…",
                        color = CarTextMuted,
                        fontSize = if (isCompact) 15.sp else 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                innerTextField()
            }
        )

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
private fun SearchResultsView(
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
            Text(
                text = "\"$query\" aramasıyla eşleşen parça bulunamadı",
                color = CarTextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
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
private fun FolderBrowserView(
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
    onSongLongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Klasöre Dön ve "Bu Klasörü Çal" Başlığı
        if (currentFolderPath != null) {
            val folderName = storageRoots.find { it.path == currentFolderPath }?.name
                ?: File(currentFolderPath).name
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
                    // tmünü Çal Butonu (48dp Yükseklik)
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

                    // Bu Klasrü Çal Butonu (48dp Yükseklik)
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
                items(itemsToDisplay) { item ->
                    if (item.isDirectory) {
                        // Büyütülmüş Klasör Satırı - Dokununca açılır, Basılı Tutunca İşlem Menüsü Çıkar!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isCompact) 62.dp else 74.dp)
                                .clip(RoundedCornerShape(if (isCompact) 14.dp else 16.dp))
                                .background(CarSurfaceVariant)
                                .border(1.dp, CarBorder, RoundedCornerShape(if (isCompact) 14.dp else 16.dp))
                                .combinedClickable(
                                    onClick = { onOpenFolder(item.path) },
                                    onLongClick = { onFolderLongClick(item) }
                                )
                                .padding(horizontal = if (isCompact) 12.dp else 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (item.isStorageRoot && item.name.contains("USB")) R.drawable.ic_usb else R.drawable.ic_folder
                                ),
                                contentDescription = null,
                                tint = if (item.isStorageRoot) CarAmber else CarCyan,
                                modifier = Modifier.size(if (isCompact) 28.dp else 34.dp)
                            )

                            Spacer(modifier = Modifier.width(if (isCompact) 12.dp else 16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = if (isCompact) 16.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.songCount > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.songCount} Müzik parçası (Basılı tut: Çal / Listeye ekle)",
                                        fontSize = 13.sp,
                                        color = CarTextMuted
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Klasör Seçenekleri için basılı tutun",
                                        fontSize = 12.sp,
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
private fun PlaylistsView(
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
    onSongLongClick: (Song) -> Unit
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
            // ➕ Yeni Çalma Listesi Oluştur Butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 52.dp else 64.dp)
                    .clip(RoundedCornerShape(if (isCompact) 12.dp else 16.dp))
                    .background(Color(0x2800E5FF))
                    .border(1.5.dp, CarCyan, RoundedCornerShape(if (isCompact) 12.dp else 16.dp))
                    .clickable { onCreatePlaylistClick() }
                    .padding(horizontal = if (isCompact) 12.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 36.dp else 42.dp)
                        .clip(CircleShape)
                        .background(CarCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        tint = Color(0xFF0C0E12),
                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(if (isCompact) 12.dp else 16.dp))

                Text(
                    text = "Yeni Çalma Listesi Oluştur",
                    fontSize = if (isCompact) 15.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CarCyan
                )
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
private fun SongListView(
    songs: List<Song>,
    currentPlayingSongPath: String?,
    onPlaySong: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    emptyMessage: String = "Mzik dosyası bulunamadı"
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980
    val cardShape = RoundedCornerShape(if (isCompact) 14.dp else 18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 66.dp else 78.dp)
            .clip(cardShape)
            .background(if (isPlaying) Color(0x2800E5FF) else CarSurfaceVariant)
            .border(
                width = if (isPlaying) 2.dp else 1.dp,
                color = if (isPlaying) CarCyan else CarBorder,
                shape = cardShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = if (isCompact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 36.dp else 42.dp)
                    .clip(CircleShape)
                    .background(CarCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_music_note),
                    contentDescription = "Çalıyor",
                    tint = Color(0xFF0C0E12),
                    modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
                )
            }
        } else if (index != null) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 32.dp else 36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E222B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%02d".format(index),
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarTextMuted
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = null,
                tint = CarTextMuted,
                modifier = Modifier.size(if (isCompact) 24.dp else 28.dp)
            )
        }

        Spacer(modifier = Modifier.width(if (isCompact) 12.dp else 16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = if (isCompact) 16.sp else 19.sp,
                fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isPlaying) CarCyan else CarTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = song.artist,
                fontSize = if (isCompact) 13.sp else 15.sp,
                color = if (isPlaying) CarCyan.copy(alpha = 0.85f) else CarTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (song.durationMs > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = song.durationFormatted,
                fontSize = if (isCompact) 12.sp else 14.sp,
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




















