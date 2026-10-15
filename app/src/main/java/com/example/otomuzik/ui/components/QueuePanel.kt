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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

        // ── 1. ÜST KONTROL BAR (3D Kokpit Paneli: Kapat + Başlık + Hızlı Kontroller) ──
        val headerShape = RoundedCornerShape(if (isCompact) 14.dp else 18.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isCompact) 8.dp else 14.dp,
                    shape = headerShape,
                    spotColor = accentColor.copy(alpha = 0.35f),
                    ambientColor = Color.Black
                )
                .clip(headerShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF222B3D),
                            Color(0xFF141926),
                            Color(0xFF0D111A)
                        )
                    )
                )
                .border(
                    1.4.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    ),
                    headerShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompact) 10.dp else 14.dp,
                        vertical = if (isCompact) 8.dp else 12.dp
                    )
            ) {
                // Kart üst içi parlama çizgisi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(0.3f), Color.Transparent)
                            )
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 1. Satır: [⬅ Kapat] + Başlık + Hızlı [⏮] [⏭]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sol: Kapat / Dosyalara Dön Butonu (3D Buton)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .height(if (isCompact) 42.dp else 52.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2E384D), Color(0xFF181F2C))
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(0.40f), Color.Black.copy(0.70f))
                                ),
                                RoundedCornerShape(12.dp)
                            )
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

                    // Sağ: Önceki ve Sonraki Butonları (3D Tuşlar)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 42.dp else 52.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF2E384D), Color(0xFF181F2C))
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(0.40f), Color.Black.copy(0.70f))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onPlayPrevious() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skip_previous),
                                contentDescription = "Öncekini Çal",
                                tint = CarTextPrimary,
                                modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 42.dp else 52.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF2E384D), Color(0xFF181F2C))
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(0.40f), Color.Black.copy(0.70f))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onPlayNext() },
                            contentAlignment = Alignment.Center
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Karışık Çal 3D Toggle Butonu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isCompact) 42.dp else 50.dp)
                            .then(
                                if (isShuffle) {
                                    Modifier
                                        .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = accentColor.copy(0.5f))
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    accentColor.copy(alpha = 0.35f),
                                                    accentColor.copy(alpha = 0.15f),
                                                    Color(0xFF131A26)
                                                )
                                            )
                                        )
                                        .border(
                                            1.5.dp,
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(0.6f), accentColor, Color.Black.copy(0.8f))
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                } else {
                                    Modifier
                                        .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color(0x44000000))
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF222A3A), Color(0xFF141924))
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(0.2f), Color.Black.copy(0.6f))
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                }
                            )
                            .clickable { onToggleShuffle() }
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shuffle),
                            contentDescription = "Karışık Çal",
                            tint = if (isShuffle) accentColor else CarTextMuted,
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isShuffle) "Karışık: Açık" else "Karışık: Kapalı",
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isShuffle) accentColor else CarTextMuted,
                            maxLines = 1
                        )
                    }

                    // Sıradakileri Yeniden Oluştur (Sırayı Yenile) 3D Butonu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1.05f)
                            .height(if (isCompact) 42.dp else 50.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = CarCyan.copy(0.4f))
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF00E5FF).copy(0.25f),
                                        Color(0xFF152232),
                                        Color(0xFF0F1722)
                                    )
                                )
                            )
                            .border(
                                1.3.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(0.5f), CarCyan, Color.Black.copy(0.7f))
                                ),
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
 * Araç kullanımı için tasarlanmış büyük, 3D yüksek kontrastlı filtre butonu.
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
    val shape = RoundedCornerShape(if (isCompact) 12.dp else 14.dp)

    val bgBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                accentColor.copy(alpha = 0.35f),
                accentColor.copy(alpha = 0.16f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF262C38),
                Color(0xFF161922)
            )
        )
    }

    val borderBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                accentColor,
                accentColor.copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF3E4758),
                Color(0xFF1B1E26)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 4.dp,
                shape = shape,
                ambientColor = if (isSelected) accentColor else Color.Black,
                spotColor = if (isSelected) accentColor else Color.Black
            )
            .height(if (isCompact) 44.dp else 54.dp)
            .clip(shape)
            .background(bgBrush)
            .border(
                width = if (isSelected) 2.dp else 1.2.dp,
                brush = borderBrush,
                shape = shape
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp)
    ) {
        // Üst parlama / gloss çizgisi (3D hissi)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            if (isSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        Text(
            text = title,
            fontSize = if (isCompact) 13.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) accentColor else CarTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center)
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
    val cardShape = RoundedCornerShape(if (isCompact) 14.dp else 16.dp)

    val bgBrush = when {
        isCurrent -> Brush.verticalGradient(
            listOf(
                accentColor.copy(alpha = 0.28f),
                Color(0xFF1A2332),
                Color(0xFF121720)
            )
        )
        isNext -> Brush.verticalGradient(
            listOf(
                Color(0xFF252B38),
                Color(0xFF181C24)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color(0xFF1C1F28),
                Color(0xFF13151B)
            )
        )
    }

    val borderBrush = when {
        isCurrent -> Brush.verticalGradient(
            listOf(
                accentColor,
                accentColor.copy(alpha = 0.4f)
            )
        )
        isNext -> Brush.verticalGradient(
            listOf(
                Color(0xFF3E4758),
                Color(0xFF1E222B)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color(0xFF2C313C),
                Color(0xFF181A20)
            )
        )
    }

    val elevation = when {
        isCurrent -> 10.dp
        isNext -> 5.dp
        else -> 2.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = cardShape,
                ambientColor = if (isCurrent) accentColor else Color.Black,
                spotColor = if (isCurrent) accentColor else Color.Black
            )
            .clip(cardShape)
            .background(bgBrush)
            .border(
                width = if (isCurrent) 1.8.dp else 1.dp,
                brush = borderBrush,
                shape = cardShape
            )
            .clickable { onClick() }
    ) {
        // Üst kenar 3D kavis ışık yansıması
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            if (isCurrent) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isCompact) 10.dp else 14.dp,
                    vertical = if (isCompact) 8.dp else 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
        ) {
            // Sol Bölüm: Sıra Numarası veya Animasyonlu Ekolayzer
            Box(
                modifier = Modifier
                    .width(if (isCompact) 38.dp else 46.dp)
                    .height(if (isCompact) 38.dp else 46.dp)
                    .shadow(
                        elevation = if (isCurrent) 6.dp else 2.dp,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isCurrent) accentColor.copy(alpha = 0.22f)
                        else Color(0xFF1A1D26)
                    )
                    .border(
                        1.dp,
                        if (isCurrent) accentColor.copy(alpha = 0.6f) else Color(0xFF333B49),
                        RoundedCornerShape(10.dp)
                    ),
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor.copy(alpha = 0.95f)
                        )
                        Text(
                            text = "#${originalIndex + 1}",
                            fontSize = 9.sp,
                            color = CarTextMuted
                        )
                    }
                } else {
                    Text(
                        text = "#${originalIndex + 1}",
                        fontSize = 12.sp,
                        color = CarTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Orta Bölüm: Başlık ve Sanatçı
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = com.example.otomuzik.util.TurkishStringFixer.fix(song.title),
                    fontSize = if (isCompact) 15.sp else 16.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isCurrent) accentColor else CarTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = com.example.otomuzik.util.TurkishStringFixer.fix(song.artist),
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
                            .shadow(4.dp, RoundedCornerShape(8.dp), ambientColor = accentColor, spotColor = accentColor)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(accentColor, accentColor.copy(alpha = 0.8f))
                                )
                            )
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
                            .shadow(2.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E232E))
                            .border(1.dp, Color(0xFF384357), RoundedCornerShape(8.dp))
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
                            .background(Color(0xFF161820).copy(alpha = 0.8f))
                            .border(1.dp, Color(0xFF282D38), RoundedCornerShape(8.dp))
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
