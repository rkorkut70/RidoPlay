package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.otomuzik.R
import com.example.otomuzik.model.FolderItem
import com.example.otomuzik.model.Playlist
import com.example.otomuzik.model.Song





import com.example.otomuzik.theme.CarTextMuted



/**
 * Klasre uzun basıldığında açılan araç içi hızlı işlem menüsü.
 */
@Composable
fun FolderactionDialog(
    folderItem: FolderItem,
    onPlayFolder: () -> Unit,
    onaddToPlaylist: () -> Unit,
    onaddToQueue: () -> Unit,
    onBatchEditTags: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(CarSurfaceDark)
                .border(2.dp, CarBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Başlık alanı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x28FFB300)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder),
                            contentDescription = null,
                            tint = CarAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folderItem.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CarTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (folderItem.songCount > 0) {
                            Text(
                                text = "${folderItem.songCount} Müzik dosyası",
                                fontSize = 14.sp,
                                color = CarTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Seenek 1: Bu Klasrü Çal
                CaractionMenuButton(
                    iconRes = R.drawable.ic_play,
                    title = "Bu Klasörü Çal",
                    subtitle = "Klasördeki tüm şarkıları hemen oynatır",
                    accentColor = CarCyan,
                    isPrimary = true,
                    onClick = {
                        onDismiss()
                        onPlayFolder()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Seçenek 2: Çalma Listesine Ekle
                CaractionMenuButton(
                    iconRes = R.drawable.ic_playlist,
                    title = "Çalma Listesine Ekle",
                    subtitle = "Mevcut veya yeni bir çalma listesine aktar",
                    accentColor = CarAmber,
                    onClick = {
                        onDismiss()
                        onaddToPlaylist()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Seçenek 3: Sıraya Ekle
                CaractionMenuButton(
                    iconRes = R.drawable.ic_queue,
                    title = "Şimdi Çalınan Sıraya Ekle",
                    subtitle = "Şarkıyı kesmeden çalma sırasının sonuna ekler",
                    accentColor = CarCyan,
                    onClick = {
                        onDismiss()
                        onaddToQueue()
                    }
                )

                if (onBatchEditTags != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    CaractionMenuButton(
                        iconRes = R.drawable.ic_settings,
                        title = "Toplu ID3 Düzenle",
                        subtitle = "Klasördeki tüm parçalara ortak sanatçı/albüm ata",
                        accentColor = CarAmber,
                        onClick = {
                            onDismiss()
                            onBatchEditTags()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // İİptal Butonu
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Vazgeç",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Şarkıya basılı tutulduğunda açılan araç içi işlem menüsü.
 */
@Composable
fun SongactionDialog(
    song: Song,
    isFavorite: Boolean,
    onaddToPlaylist: () -> Unit,
    onaddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetAsRingtone: (() -> Unit)? = null,
    onRemoveFromLibrary: () -> Unit = {},
    onDeletePhysicalFile: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPhone = configuration.screenWidthDp < 600
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    var showDeleteConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showHideConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isPhone) 0.95f else 0.75f)
                .heightIn(max = if (isCompact) 520.dp else 650.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CarSurfaceDark)
                .border(2.dp, CarBorder, RoundedCornerShape(24.dp))
                .padding(if (isCompact) 16.dp else 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showDeleteConfirm) {
                    // ── 1. DOSYAYI FİZİKSEL SİLME ONAY EKRANI ──
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FF3D57)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = null,
                            tint = Color(0xFFFF3D57),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Dosyayı Kalıcı Olarak Sil",
                        fontSize = if (isCompact) 18.sp else 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF3D57)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "⚠️ Bu işlem geri alınamaz! Müzik dosyası depolama biriminden/diskinden kalıcı olarak silinecektir.",
                        fontSize = 13.5.sp,
                        color = CarTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dosya Detay Kartı
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161B26))
                            .border(1.dp, Color(0x33FF3D57), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = song.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 13.sp,
                                color = CarTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "📁 ${song.path}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF8E9BAE),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Butonlar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Vazgeç",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextSecondary
                            )
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onDeletePhysicalFile()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D57)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Kalıcı Olarak Sil",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else if (showHideConfirm) {
                    // ── 2. KÜTÜPHANEDEN SİLME (GİZLEME) ONAY EKRANI ──
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CarAmber.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_visibility_off),
                            contentDescription = null,
                            tint = CarAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Kütüphaneden Sil",
                        fontSize = if (isCompact) 18.sp else 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CarAmber
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ℹ️ Bu işlem müzik dosyasını silmez. Parça depolama biriminde kalır ancak RidoPlay listelerinden gizlenir.\n\nİstediğiniz zaman Ayarlar > Kütüphane Yönetimi kısmından gizlenen şarkıları geri yükleyebilirsiniz.",
                        fontSize = 13.5.sp,
                        color = CarTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Parça Bilgi Kartı
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161B26))
                            .border(1.dp, CarAmber.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = song.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 13.sp,
                                color = CarTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showHideConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Vazgeç",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextSecondary
                            )
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onRemoveFromLibrary()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CarAmber),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Kütüphaneden Sil",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0C0E12)
                            )
                        }
                    }
                } else {
                    // ── 3. ANA İŞLEM MENÜSÜ ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 44.dp else 52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x2800E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_music_note),
                                contentDescription = null,
                                tint = CarCyan,
                                modifier = Modifier.size(if (isCompact) 24.dp else 30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = com.example.otomuzik.util.TurkishStringFixer.fix(song.title),
                                fontSize = if (isCompact) 17.sp else 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CarTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = com.example.otomuzik.util.TurkishStringFixer.fix(song.artist),
                                fontSize = if (isCompact) 13.sp else 14.sp,
                                color = CarTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seçenek 1: Çalma Listesine Ekle
                    CaractionMenuButton(
                        iconRes = R.drawable.ic_playlist,
                        title = "Çalma Listesine Ekle",
                        subtitle = "İstediğiniz bir listeye bu parçayı ekleyin",
                        accentColor = CarCyan,
                        isPrimary = true,
                        onClick = {
                            onDismiss()
                            onaddToPlaylist()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Seçenek 2: Sıraya Ekle
                    CaractionMenuButton(
                        iconRes = R.drawable.ic_queue,
                        title = "Sıradaki Parçalara Ekle",
                        subtitle = "Mevcut çalma listesinin sonuna ekle",
                        accentColor = CarAmber,
                        onClick = {
                            onDismiss()
                            onaddToQueue()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Seçenek 3: Favori Değiştir
                    CaractionMenuButton(
                        iconRes = R.drawable.ic_favorite,
                        title = if (isFavorite) "Favorilerden Kaldır" else "Favorilere Ekle",
                        subtitle = if (isFavorite) "Kalp işaretini kaldırır" else "Hızlı erişim için favorilere kaydeder",
                        accentColor = if (isFavorite) CarAmber else CarCyan,
                        onClick = {
                            onDismiss()
                            onToggleFavorite()
                        }
                    )

                    if (onSetAsRingtone != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CaractionMenuButton(
                            iconRes = R.drawable.ic_music_note,
                            title = "Zil Sesi Olarak Ayarla",
                            subtitle = "Bu parçayı telefonun varsayılan zil sesi yap",
                            accentColor = CarCyan,
                            onClick = {
                                onDismiss()
                                onSetAsRingtone()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Seçenek 4: Kütüphaneden Sil (Gizle)
                    CaractionMenuButton(
                        iconRes = R.drawable.ic_visibility_off,
                        title = "Kütüphaneden Sil",
                        subtitle = "Dosya diskte kalır, sadece kütüphane listesinden çıkarılır",
                        accentColor = CarAmber,
                        onClick = { showHideConfirm = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Seçenek 5: Dosyayı Tamamen Sil
                    CaractionMenuButton(
                        iconRes = R.drawable.ic_delete,
                        title = "Dosyayı Sil",
                        subtitle = "Parça cihaz hafızasından / diskten kalıcı olarak silinir",
                        accentColor = Color(0xFFFF4D6D),
                        onClick = { showDeleteConfirm = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // İptal Butonu
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Vazgeç",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Şarkı veya Klasrü bir çÇalma Listesine ekleme seçim penceresi.
 */
@Composable
fun AddToPlaylistDialog(
    Playlists: List<Playlist>,
    songsCount: Int,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(CarSurfaceDark)
                .border(2.dp, CarBorder, RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x2800E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_playlist),
                            contentDescription = null,
                            tint = CarCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Çalma Listesine Ekle",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextPrimary
                        )
                        Text(
                            text = "$songsCount Müzik parçası eklenecek",
                            fontSize = 14.sp,
                            color = CarTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // "➕ Yeni ÇÇalma Listesi Olutur" Butonu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x2800E5FF))
                        .border(1.5.dp, CarCyan, RoundedCornerShape(16.dp))
                        .clickable {
                            onDismiss()
                            onCreateNewPlaylist()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CarCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            tint = Color(0xFF0C0E12),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Yeni Çalma Listesi Oluştur",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CarCyan
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mevcut ÇÇalma Listeileri
                if (Playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Kayıtlı çalma listeniz yok.\nYukarıdan yeni bir liste oluşturabilirsiniz.",
                            fontSize = 15.sp,
                            color = CarTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Mevcut Listeler:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CarTextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(Playlists) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(62.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CarSurfaceVariant)
                                    .border(1.dp, CarBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        onSelectPlaylist(pl)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_playlist),
                                    contentDescription = null,
                                    tint = CarAmber,
                                    modifier = Modifier.size(26.dp)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pl.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CarTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${pl.songCount} parça",
                                        fontSize = 13.sp,
                                        color = CarTextMuted
                                    )
                                }

                                Text(
                                    text = "Ekle ＋",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarCyan
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // İİptal Butonu
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Vazgeç",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Yeni ÇÇalma Listesi Oluturma Penceresi.
 * araba sürüşüne uygun büyük klavye kutusu ve tek dokunuşla hazır isim etiketileri sunar.
 */
@Composable
fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var PlaylistName by remember { mutableStateOf("") }
    val quickNames = listOf("Uzun Yol", "Türkçe Pop", "Rock", "Akustik", "Gece Sürüşü", "Eski Şarkılar")

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(CarSurfaceDark)
                .border(2.dp, CarBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x2800E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            tint = CarCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Yeni Çalma Listesi",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Metin Giriş Kutusu (Otomobil için 62dp büyük kutu)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CarSurfaceVariant)
                        .border(
                            width = if (PlaylistName.isNotEmpty()) 2.dp else 1.dp,
                            color = if (PlaylistName.isNotEmpty()) CarCyan else CarBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = PlaylistName,
                        onValueChange = { PlaylistName = it },
                        textStyle = TextStyle(
                            color = CarTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(CarCyan),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (PlaylistName.isEmpty()) {
                                Text(
                                    text = "Liste adını girin…",
                                    color = CarTextMuted,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (PlaylistName.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { PlaylistName = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hızlı Seçim Etiketileri (Hazır Başlıklar - Tek dokunuşla Seilir)
                Text(
                    text = "Hızlı İsimler:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CarTextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickNames) { name ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (PlaylistName == name) Color(0x3300E5FF) else CarSurfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (PlaylistName == name) CarCyan else CarBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { PlaylistName = name }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (PlaylistName == name) CarCyan else CarTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // aksiyon Butonları (Vazgeç / Olutur)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Vazgeç",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            if (PlaylistName.isNotBlank()) {
                                onCreate(PlaylistName.trim())
                            }
                        },
                        enabled = PlaylistName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CarCyan,
                            disabledContainerColor = CarSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Oluştur",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (PlaylistName.isNotBlank()) Color(0xFF0C0E12) else CarTextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * araç içi menü öğesi butonu (Büyük dokunma alanı, simge, başlık ve açıklama).
 */
@Composable
private fun CaractionMenuButton(
    iconRes: Int,
    title: String,
    subtitle: String,
    accentColor: Color,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPrimary) Color(0x2800E5FF) else CarSurfaceVariant)
            .border(
                width = if (isPrimary) 1.5.dp else 1.dp,
                color = if (isPrimary) CarCyan else CarBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isPrimary) CarCyan else Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (isPrimary) Color(0xFF0C0E12) else accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = CarTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = CarTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Cihazdaki .m3u / .m3u8 çalma listesi dosyalarını seçip içe aktarmayı sağlayan pencere.
 */
@Composable
fun M3uImportDialog(
    m3uFiles: List<java.io.File>,
    accentColor: Color = CarCyan,
    onSelectFile: (java.io.File) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(CarSurfaceDark)
                .border(2.dp, CarBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x28FFB300)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_playlist),
                            contentDescription = null,
                            tint = CarAmber,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "M3U İçe Aktar",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextPrimary
                        )
                        Text(
                            text = "Müzik veya İndirilenler klasöründen liste seçin",
                            fontSize = 12.sp,
                            color = CarTextMuted
                        )
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Yenile",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (m3uFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(CarSurfaceVariant, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Müzik veya İndirilenler klasöründe henüz .m3u / .m3u8 dosyası bulunamadı.",
                            fontSize = 13.sp,
                            color = CarTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(m3uFiles) { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CarSurfaceVariant)
                                    .border(1.dp, CarBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        onSelectFile(file)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_playlist),
                                    contentDescription = null,
                                    tint = CarAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.nameWithoutExtension,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CarTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = file.name,
                                        fontSize = 12.sp,
                                        color = CarTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "İçe Aktar ➔",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Vazgeç",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarTextSecondary
                    )
                }
            }
        }
    }
}





















