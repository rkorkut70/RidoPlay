package com.example.otomuzik.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.model.Song
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.util.SongSortHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Şarkı ve kütüphane listelerinde alfabetik hızlı zıplama ve kaydırma sağlayan dikey indeks çubuğu.
 * Dokunulduğunda doğrudan ilgili harfle başlayan ilk şarkıya anında atlar.
 */
@Composable
fun AlphabetFastScroller(
    songs: List<Song>,
    listState: LazyListState,
    accentColor: Color,
    modifier: Modifier = Modifier,
    headerOffset: Int = 1 // Liste başında toplam şarkı sayısı gibi header item varsa
) {
    if (songs.size < 12) return

    val coroutineScope = rememberCoroutineScope()
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var isInteracting by remember { mutableStateOf(false) }

    // Şarkılardan harf -> ilk indeks haritası
    val letterMap = remember(songs, headerOffset) {
        val map = linkedMapOf<Char, Int>()
        songs.forEachIndexed { index, song ->
            val letter = SongSortHelper.getFirstLetter(song.title)
            if (!map.containsKey(letter)) {
                map[letter] = index + headerOffset
            }
        }
        map
    }

    val letters = remember(letterMap) {
        SongSortHelper.ALPHABET_ORDER.filter { letterMap.containsKey(it) }
    }
    if (letters.size < 2) return

    // Dokunma / kaydırma bittiğinde baloncuk bir süre sonra sönsün
    LaunchedEffect(isInteracting) {
        if (!isInteracting) {
            delay(600)
            selectedLetter = null
        }
    }

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.CenterEnd
    ) {
        // 1. Ortada çıkan büyük harf önizleme baloncuğu
        AnimatedVisibility(
            visible = selectedLetter != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(end = 46.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(accentColor, CarSurfaceDark)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (selectedLetter ?: ' ').toString(),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // 2. Sağ kenardaki alfabetik dikey şerit
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F141C).copy(alpha = 0.88f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(vertical = 4.dp)
                .pointerInput(letters, letterMap, headerOffset) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        fun jumpToLetterAt(y: Float) {
                            if (letters.isEmpty()) return
                            val itemHeight = size.height.toFloat() / letters.size
                            val index = (y / itemHeight).toInt().coerceIn(0, letters.lastIndex)
                            val letter = letters[index]
                            if (selectedLetter != letter || !isInteracting) {
                                selectedLetter = letter
                                isInteracting = true
                                letterMap[letter]?.let { targetIndex ->
                                    coroutineScope.launch {
                                        val scrollPos = if (targetIndex <= headerOffset) 0 else targetIndex
                                        listState.scrollToItem(scrollPos)
                                    }
                                }
                            }
                        }

                        // İlk dokunmada anında ilgili harfe zıpla
                        jumpToLetterAt(down.position.y)
                        down.consume()

                        // Kaydırma boyunca harfleri gerçek zamanlı takip et
                        while (true) {
                            val event = awaitPointerEvent()
                            val currentChange = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!currentChange.pressed) break
                            currentChange.consume()
                            jumpToLetterAt(currentChange.position.y)
                        }
                        isInteracting = false
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { char ->
                val isSelected = selectedLetter == char
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.toString(),
                        fontSize = if (letters.size > 26) 8.5.sp else if (letters.size > 20) 9.5.sp else 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) accentColor else CarTextMuted.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
