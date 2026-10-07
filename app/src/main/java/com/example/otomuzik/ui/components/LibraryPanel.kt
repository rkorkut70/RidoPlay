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
            .padding(if (isCompact) 8.dp else 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. ÜST SEKME ÇUBUĞU (5 Büyük Sekme + Kütüphane Yenileme Butonu + Ayarlar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 44.dp else 56.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabButton(
                    title = if (isCompact) "📁 Klasör" else "📁 Klasörler",
                    isSelected = !isQueueVisible && activeTab == TabType.FOLDERS && searchQuery.isEmpty(),
                    onClick = {
                        if (isQueueVisible) onCloseQueue()
                        if (searchQuery.isNotEmpty()) onSearchQueryChange("")
                        onTabSelected(TabType.FOLDERS)
                    },
                    modifier = Modifier.weight(1.05f)
                )

                TabButton(
                    title = if (isCompact) "🎵 Şarkı" else "🎵 Şarkılar",
                    isSelected = !isQueueVisible && (activeTab == TabType.ALL_SONGS || searchQuery.isNotEmpty()),
                    onClick = {
                        if (isQueueVisible) onCloseQueue()
                        onTabSelected(TabType.ALL_SONGS)
                    },
                    modifier = Modifier.weight(0.95f)
                )

                TabButton(
                    title = if (isCompact) "📑 Liste" else "📑 Listeler",
                    isSelected = !isQueueVisible && activeTab == TabType.PLAYLISTS && searchQuery.isEmpty(),
                    onClick = {
                        if (isQueueVisible) onCloseQueue()
                        if (searchQuery.isNotEmpty()) onSearchQueryChange("")
                        onTabSelected(TabType.PLAYLISTS)
                    },
                    modifier = Modifier.weight(0.95f)
                )

                TabButton(
                    title = if (isCompact) "❤️ Favori" else "❤️ Favoriler",
                    isSelected = !isQueueVisible && activeTab == TabType.FAVORITES && searchQuery.isEmpty(),
                    onClick = {
                        if (isQueueVisible) onCloseQueue()
                        if (searchQuery.isNotEmpty()) onSearchQueryChange("")
                        onTabSelected(TabType.FAVORITES)
                    },
                    modifier = Modifier.weight(0.95f)
                )

                TabButton(
                    title = if (currentQueue.isNotEmpty()) "🎧 Sıra (${currentQueue.size})" else "🎧 Sıra",
                    isSelected = isQueueVisible,
                    onClick = { onOpenQueue() },
                    modifier = Modifier.weight(1.05f)
                )

                // Ayarlar (⚙️) Tuşu
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(if (isCompact) 42.dp else 56.dp)
                        .background(CarSurfaceVariant, RoundedCornerShape(if (isCompact) 10.dp else 14.dp))
                        .border(1.dp, CarBorder, RoundedCornerShape(if (isCompact) 10.dp else 14.dp))
                ) {
                    Text(
                        text = "⚙️",
                        fontSize = if (isCompact) 18.sp else 24.sp
                    )
                }

                // Kütüphane Veritabanını Yeniden İndeksleme Tuşu
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(if (isCompact) 42.dp else 56.dp)
                        .background(CarSurfaceVariant, RoundedCornerShape(if (isCompact) 10.dp else 14.dp))
                        .border(1.dp, CarBorder, RoundedCornerShape(if (isCompact) 10.dp else 14.dp))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = "Kütüphaneyi Yenile",
                        tint = CarCyan,
                        modifier = Modifier.size(if (isCompact) 20.dp else 26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

            if (isQueueVisible) {
                // 2. ÇALMA SIRASI / OYNATMA LİSTESİ GÖRÜNÜMÜ
                PlaybackQueueView(
                    queue = currentQueue,
                    currentIndex = currentQueueIndex,
                    isShuffle = isShuffle,
                    accentColor = accentColor,
                    onPlayAtIndex = onPlayQueueIndex,
                    onPlayNext = onPlayNext,
                    onPlayPrevious = onPlayPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onReshuffleUpcoming = onReshuffleUpcoming,
                    onClose = onCloseQueue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                // 2. İÇERİK ALANI (kaydırılabilir Liste - Weight 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CarCyan)
                        }
                    } else if (searchQuery.isNotEmpty()) {
                        // Arama Sonuçları
                        SearchResultsView(
                            songs = filteredSongs,
                            query = searchQuery,
                            currentPlayingSongPath = currentPlayingSongPath,
                            onPlaySong = { song -> onPlaySong(song, filteredSongs) },
                            onSongLongClick = onSongLongClick
                        )
                    } else {
                        when (activeTab) {
                            TabType.FOLDERS -> {
                                FolderBrowserView(
                                    storageRoots = storageRoots,
                                    currentFolderPath = currentFolderPath,
                                    currentFolderItems = currentFolderItems,
                                    currentPlayingSongPath = currentPlayingSongPath,
                                    onOpenFolder = onOpenFolder,
                                    onBackFolder = onBackFolder,
                                    onPlayFolder = onPlayFolder,
                                    onPlayallSongs = onPlayallSongs,
                                    onFolderLongClick = onFolderLongClick,
                                    onPlaySong = onPlaySong,
                                    onSongLongClick = onSongLongClick
                                )
                            }
                            TabType.ALL_SONGS -> {
                                SongListView(
                                    songs = filteredSongs,
                                    currentPlayingSongPath = currentPlayingSongPath,
                                    onPlaySong = { song -> onPlaySong(song, filteredSongs) },
                                    onSongLongClick = onSongLongClick
                                )
                            }
                            TabType.PLAYLISTS -> {
                                PlaylistsView(
                                    Playlists = Playlists,
                                    selectedPlaylist = selectedPlaylist,
                                    selectedPlaylistSongs = selectedPlaylistSongs,
                                    currentPlayingSongPath = currentPlayingSongPath,
                                    onOpenPlaylist = onOpenPlaylist,
                                    onClosePlaylist = onClosePlaylist,
                                    onPlayPlaylist = onPlayPlaylist,
                                    onDeletePlaylist = onDeletePlaylist,
                                    onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                                    onCreatePlaylistClick = onCreatePlaylistClick,
                                    onPlaySong = { song -> onPlaySong(song, selectedPlaylistSongs) },
                                    onSongLongClick = onSongLongClick
                                )
                            }
                            TabType.FAVORITES -> {
                                SongListView(
                                    songs = favoriteSongs,
                                    currentPlayingSongPath = currentPlayingSongPath,
                                    onPlaySong = { song -> onPlaySong(song, favoriteSongs) },
                                    onSongLongClick = onSongLongClick,
                                    emptyMessage = "Henüz favori şarkı eklenmedi.\nŞarkı çalarken kalp simgesine dokunun veya basılı tutarak ekleyin."
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. SABİT ARAMA ÇUBUĞU (Her zaman Listenin altında sabit - 58dp Yükseklik)
                FixedCarSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    resultCount = if (searchQuery.isNotEmpty()) filteredSongs.size else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    Box(
        modifier = modifier
            .height(if (isCompact) 44.dp else 56.dp)
            .clip(RoundedCornerShape(if (isCompact) 10.dp else 14.dp))
            .background(if (isSelected) CarCyan else CarSurfaceVariant)
            .border(
                1.5.dp,
                if (isSelected) CarCyan else CarBorder,
                RoundedCornerShape(if (isCompact) 10.dp else 14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = if (isCompact) 2.dp else 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = if (isCompact) 12.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF0C0E12) else CarTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
            .height(if (isCompact) 46.dp else 58.dp)
            .clip(RoundedCornerShape(if (isCompact) 12.dp else 16.dp))
            .background(CarSurfaceVariant)
            .border(
                width = if (query.isNotEmpty()) 2.dp else 1.dp,
                color = if (query.isNotEmpty()) CarCyan else CarBorder,
                shape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp)
            )
            .padding(horizontal = if (isCompact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_search),
            contentDescription = "ara",
            tint = if (query.isNotEmpty()) CarCyan else CarTextMuted,
            modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = CarTextPrimary,
                fontSize = 17.sp,
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
                        fontSize = 17.sp,
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
    val cardShape = RoundedCornerShape(if (isCompact) 14.dp else 16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 62.dp else 74.dp)
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
            .padding(horizontal = if (isCompact) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 32.dp else 38.dp)
                    .clip(CircleShape)
                    .background(CarCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_music_note),
                    contentDescription = "Çalıyor",
                    tint = Color(0xFF0C0E12),
                    modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                )
            }
        } else if (index != null) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 30.dp else 34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E222B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%02d".format(index),
                    fontSize = if (isCompact) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarTextMuted
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = null,
                tint = CarTextMuted,
                modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
            )
        }

        Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = if (isCompact) 15.sp else 18.sp,
                fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isPlaying) CarCyan else CarTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = song.artist,
                fontSize = if (isCompact) 12.sp else 14.sp,
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




















