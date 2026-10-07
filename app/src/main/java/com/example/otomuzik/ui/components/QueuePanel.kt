package com.example.otomuzik.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.R
import com.example.otomuzik.model.Song
import com.example.otomuzik.theme.*

enum class QueueFilterMode {
    ALL,
    PREVIOUS,
    NEXT
}

/**
 * Dosya listesi / Kütüphane panelinin sağ tarafında doğrudan gösterilen
 * gelişmiş Oynatma Listesi / Çalma Sırası bileşeni.
 *
 * - Araç kullanımı için BÜYÜK ve yüksek kontrastlı butonlar (Tümü, Öncekiler, Sonrakiler).
 * - Sıradakileri Yeniden Oluştur (Yeniden Karıştır) seçeneği.
 * - Öncekini Çal ve Sonrakini Çal büyük butonları.
 * - Karışık Çal açıp kapatma düğmesi.
 * - Tek tıkla Kapatıp klasör / dosya listesine dönüş.
 */
@Composable
fun PlaybackQueueView(
    queue: List<Song>,
    currentIndex: Int,
    isShuffle: Boolean,
    accentColor: Color = CarCyan,
    onPlayAtIndex: (Int) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onReshuffleUpcoming: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterMode by remember { mutableStateOf(QueueFilterMode.ALL) }
    val listState = rememberLazyListState()

    val previousCount = currentIndex.coerceAtLeast(0)
    val nextCount = (queue.size - currentIndex - 1).coerceAtLeast(0)

    // Çalan şarkı veya filtre modu değiştiğinde otomatik kaydır
    LaunchedEffect(currentIndex, filterMode) {
        if (queue.isNotEmpty() && currentIndex in queue.indices) {
            when (filterMode) {
                QueueFilterMode.ALL -> {
                    listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
                }
                QueueFilterMode.NEXT -> {
                    listState.animateScrollToItem(0)
                }
                QueueFilterMode.PREVIOUS -> {
                    if (previousCount > 0) {
                        listState.animateScrollToItem((previousCount - 1).coerceAtLeast(0))
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

        // ── 1. ÜST KONTROL BAR (Kapat + Başlık + Hızlı Kontroller) ──
        Surface(
            color = CarSurfaceVariant,
            shape = RoundedCornerShape(if (isCompact) 14.dp else 18.dp),
            border = BorderStroke(1.2.dp, CarBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompact) 10.dp else 14.dp,
                        vertical = if (isCompact) 8.dp else 12.dp
                    )
            ) {
                // 1. Satır: [⬅ Kapat] + Başlık + Hızlı [⏮] [⏭]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sol: Kapat / Dosyalara Dön Butonu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .height(if (isCompact) 42.dp else 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarSurfaceDark)
                            .border(1.2.dp, CarBorder, RoundedCornerShape(12.dp))
                            .clickable { onClose() }
                            .padding(horizontal = if (isCompact) 10.dp else 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Geri / Kapat",
                            tint = CarCyan,
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                        Text(
                            text = "Kapat",
                            fontSize = if (isCompact) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextPrimary
                        )
                    }

                    // Orta: Başlık ve Parça Durum Özeti
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_queue),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                        )
                        Text(
                            text = "Çalma Sırası",
                            fontSize = if (isCompact) 15.sp else 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CarTextPrimary
                        )
                        Text(
                            text = if (queue.isNotEmpty() && currentIndex >= 0)
                                "(${currentIndex + 1}/${queue.size})"
                            else
                                "(${queue.size})",
                            fontSize = if (isCompact) 12.sp else 14.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sağ: Önceki ve Sonraki Butonları
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onPlayPrevious,
                            modifier = Modifier
                                .size(if (isCompact) 42.dp else 52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CarSurfaceDark)
                                .border(1.2.dp, CarBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_previous),
                                contentDescription = "Öncekini Çal",
                                tint = CarTextPrimary,
                                modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
                            )
                        }

                        IconButton(
                            onClick = onPlayNext,
                            modifier = Modifier
                                .size(if (isCompact) 42.dp else 52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CarSurfaceDark)
                                .border(1.2.dp, CarBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_next),
                                contentDescription = "Sonrakini Çal",
                                tint = CarTextPrimary,
                                modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))

                // 2. Satır: [🔀 Karışık Çal / Düz] & [🔄 Sıradakileri Yeniden Oluştur (Sırayı Yenile)]
                val shuffleBg by animateColorAsState(
                    targetValue = if (isShuffle) accentColor.copy(alpha = 0.22f) else CarSurfaceDark,
                    label = "shuffleBg"
                )
                val shuffleTint by animateColorAsState(
                    targetValue = if (isShuffle) accentColor else CarTextMuted,
                    label = "shuffleTint"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Karışık Çal Toggle Butonu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isCompact) 42.dp else 50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(shuffleBg)
                            .border(
                                1.2.dp,
                                if (isShuffle) accentColor else CarBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onToggleShuffle() }
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shuffle),
                            contentDescription = "Karışık Çal",
                            tint = shuffleTint,
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isShuffle) "Karışık: Açık" else "Karışık: Kapalı",
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = shuffleTint,
                            maxLines = 1
                        )
                    }

                    // Sıradakileri Yeniden Oluştur Butonu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1.05f)
                            .height(if (isCompact) 42.dp else 50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarSurfaceDark)
                            .border(
                                1.2.dp,
                                CarCyan.copy(alpha = 0.7f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onReshuffleUpcoming() }
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Sıradakileri Yeniden Oluştur",
                            tint = CarCyan,
                            modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sırayı Yenile",
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarCyan,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))

                // 3. Satır: BÜYÜK FİLTRE BUTONLARI (Tümü / Öncekiler / Sonrakiler)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
                ) {
                    BigQueueFilterButton(
                        title = "Tümü (${queue.size})",
                        isSelected = filterMode == QueueFilterMode.ALL,
                        accentColor = accentColor,
                        onClick = { filterMode = QueueFilterMode.ALL },
                        modifier = Modifier.weight(1f)
                    )
                    BigQueueFilterButton(
                        title = "⏮ Öncekiler ($previousCount)",
                        isSelected = filterMode == QueueFilterMode.PREVIOUS,
                        accentColor = accentColor,
                        onClick = { filterMode = QueueFilterMode.PREVIOUS },
                        modifier = Modifier.weight(1f)
                    )
                    BigQueueFilterButton(
                        title = "⏭ Sonrakiler ($nextCount)",
                        isSelected = filterMode == QueueFilterMode.NEXT,
                        accentColor = accentColor,
                        onClick = { filterMode = QueueFilterMode.NEXT },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 2. ŞARKI LİSTESİ ──
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Çalma sırası boş.\nKlasörlerden veya şarkılardan bir parça seçin.",
                    color = CarTextMuted,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val displayedItems: List<IndexedValue<Song>> = when (filterMode) {
                QueueFilterMode.ALL -> queue.withIndex().toList()
                QueueFilterMode.PREVIOUS -> {
                    if (currentIndex > 0) {
                        queue.withIndex().take(currentIndex)
                    } else emptyList()
                }
                QueueFilterMode.NEXT -> {
                    if (currentIndex + 1 < queue.size) {
                        queue.withIndex().drop(currentIndex + 1)
                    } else emptyList()
                }
            }

            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (filterMode) {
                            QueueFilterMode.PREVIOUS -> "Henüz önceki parça bulunmuyor.\n(Listenin başındasınız)"
                            QueueFilterMode.NEXT -> "Sıranın sonundasınız.\n(Sonraki parça kalmadı)"
                            else -> "Parça bulunamadı."
                        },
                        color = CarTextMuted,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = displayedItems,
                        key = { "${it.index}_${it.value.path}" }
                    ) { item ->
                        val index = item.index
                        val song = item.value
                        val isCurrent = index == currentIndex
                        val isPrevious = index < currentIndex
                        val isNext = index > currentIndex

                        QueueSongItemCard(
                            originalIndex = index,
                            currentIndex = currentIndex,
                            song = song,
                            isCurrent = isCurrent,
                            isPrevious = isPrevious,
                            isNext = isNext,
                            accentColor = accentColor,
                            onClick = { onPlayAtIndex(index) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Araç kullanımı için tasarlanmış büyük, yüksek kontrastlı filtre butonu.
 */
@Composable
private fun BigQueueFilterButton(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    Box(
        modifier = modifier
            .height(if (isCompact) 44.dp else 54.dp)
            .clip(RoundedCornerShape(if (isCompact) 12.dp else 14.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.22f) else CarSurfaceDark)
            .border(
                width = if (isSelected) 2.dp else 1.2.dp,
                color = if (isSelected) accentColor else CarBorder,
                shape = RoundedCornerShape(if (isCompact) 12.dp else 14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = if (isCompact) 13.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) accentColor else CarTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QueueSongItemCard(
    originalIndex: Int,
    currentIndex: Int,
    song: Song,
    isCurrent: Boolean,
    isPrevious: Boolean,
    isNext: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 580 || configuration.screenWidthDp < 980

    val bgColor = when {
        isCurrent -> accentColor.copy(alpha = 0.18f)
        isPrevious -> CarSurfaceVariant.copy(alpha = 0.55f)
        else -> CarSurfaceVariant
    }

    val borderColor = when {
        isCurrent -> accentColor
        isPrevious -> CarBorder.copy(alpha = 0.5f)
        else -> CarBorder
    }

    val cardShape = RoundedCornerShape(if (isCompact) 14.dp else 16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(bgColor)
            .border(
                width = if (isCurrent) 1.8.dp else 1.dp,
                color = borderColor,
                shape = cardShape
            )
            .clickable { onClick() }
            .padding(
                horizontal = if (isCompact) 10.dp else 14.dp,
                vertical = if (isCompact) 8.dp else 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
    ) {
        // Sol Bölüm: Sıra Numarası veya Animasyonlu Ekolayzer
        Box(
            modifier = Modifier.width(if (isCompact) 36.dp else 42.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                PlayingIndicator(
                    isPlaying = true,
                    color = accentColor,
                    size = 22.dp
                )
            } else if (isNext) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "+${originalIndex - currentIndex}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor.copy(alpha = 0.95f)
                    )
                    Text(
                        text = "#${originalIndex + 1}",
                        fontSize = 10.sp,
                        color = CarTextMuted
                    )
                }
            } else {
                Text(
                    text = "#${originalIndex + 1}",
                    fontSize = 13.sp,
                    color = CarTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Orta Bölüm: Başlık ve Sanatçı
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = if (isCompact) 15.sp else 16.sp,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isCurrent) accentColor else CarTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = if (isCompact) 12.sp else 13.sp,
                color = if (isCurrent) CarTextPrimary.copy(alpha = 0.9f) else CarTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Sağ Bölüm: Rozet (Çalıyor / Sırada / Geçmiş) ve Süre
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "▶ ÇALIYOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0C0E12)
                    )
                }
            } else if (isNext) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarSurfaceDark)
                        .border(1.dp, CarBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Sırada",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarCyan
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarSurfaceDark.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Geçmiş",
                        fontSize = 11.sp,
                        color = CarTextMuted
                    )
                }
            }

            Text(
                text = song.durationFormatted,
                fontSize = 13.sp,
                color = if (isCurrent) accentColor else CarTextMuted,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Geriye dönük uyumluluk veya gerektiğinde diyalog olarak açılabilmesi için diyalog sarmalayıcısı.
 */
@Composable
fun QueuePanel(
    queue: List<Song>,
    currentIndex: Int,
    isShuffle: Boolean = false,
    accentColor: Color = CarCyan,
    onPlayAtIndex: (Int) -> Unit,
    onPlayNext: () -> Unit = {},
    onPlayPrevious: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onReshuffleUpcoming: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 680.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(CarSurfaceDark)
                .border(1.5.dp, accentColor, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            PlaybackQueueView(
                queue = queue,
                currentIndex = currentIndex,
                isShuffle = isShuffle,
                accentColor = accentColor,
                onPlayAtIndex = onPlayAtIndex,
                onPlayNext = onPlayNext,
                onPlayPrevious = onPlayPrevious,
                onToggleShuffle = onToggleShuffle,
                onReshuffleUpcoming = onReshuffleUpcoming,
                onClose = onDismiss
            )
        }
    }
}
