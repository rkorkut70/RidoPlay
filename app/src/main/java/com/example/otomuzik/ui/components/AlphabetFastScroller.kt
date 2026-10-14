package com.example.otomuzik.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.model.Song
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarTextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Şarkı ve kütüphane listelerinde alfabetik hızlı zıplama ve kaydırma sağlayan dikey indeks çubuğu.
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
    var isDragging by remember { mutableStateOf(false) }

    // Şarkılardan harf -> ilk indeks haritası
    val letterMap = remember(songs) {
        val map = linkedMapOf<Char, Int>()
        songs.forEachIndexed { index, song ->
            val title = song.title.trim()
            val firstChar = title.firstOrNull()?.uppercaseChar() ?: '#'
            val letter = when (firstChar) {
                in 'A'..'Z', 'Ç', 'Ğ', 'İ', 'I', 'Ö', 'Ş', 'Ü' -> firstChar
                else -> '#'
            }
            if (!map.containsKey(letter)) {
                map[letter] = index + headerOffset
            }
        }
        map
    }

    val letters = remember(letterMap) { letterMap.keys.toList() }
    if (letters.size < 2) return

    var columnHeightPx by remember { mutableFloatStateOf(1f) }

    // Kaydırma bittiğinde baloncuk bir süre sonra sönsün
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            delay(500)
            selectedLetter = null
        }
    }

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.CenterEnd
    ) {
        // 1. Ortada çıkan büyük harf önizleme baloncuğu
        AnimatedVisibility(
            visible = isDragging && selectedLetter != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(end = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(accentColor, CarSurfaceDark)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (selectedLetter ?: ' ').toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // 2. Sağ kenardaki alfabetik dikey şerit
        Column(
            modifier = Modifier
                .width(26.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F141C).copy(alpha = 0.82f))
                .padding(vertical = 4.dp)
                .onGloballyPositioned {
                    columnHeightPx = it.size.height.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(letters) {
                    val onLetterTouch = { y: Float ->
                        val itemHeight = columnHeightPx / letters.size
                        val index = (y / itemHeight).toInt().coerceIn(0, letters.lastIndex)
                        val letter = letters[index]
                        selectedLetter = letter
                        isDragging = true
                        letterMap[letter]?.let { targetIndex ->
                            coroutineScope.launch {
                                listState.scrollToItem(targetIndex)
                            }
                        }
                    }

                    detectTapGestures(
                        onPress = { offset ->
                            onLetterTouch(offset.y)
                            tryAwaitRelease()
                            isDragging = false
                        }
                    )
                }
                .pointerInput(letters) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val itemHeight = columnHeightPx / letters.size
                            val index = (offset.y / itemHeight).toInt().coerceIn(0, letters.lastIndex)
                            val letter = letters[index]
                            selectedLetter = letter
                            isDragging = true
                            letterMap[letter]?.let { targetIndex ->
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val itemHeight = columnHeightPx / letters.size
                            val index = (change.position.y / itemHeight).toInt().coerceIn(0, letters.lastIndex)
                            val letter = letters[index]
                            if (selectedLetter != letter) {
                                selectedLetter = letter
                                letterMap[letter]?.let { targetIndex ->
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            letters.forEach { char ->
                val isSelected = selectedLetter == char
                Text(
                    text = char.toString(),
                    fontSize = if (letters.size > 22) 9.sp else 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = if (isSelected) accentColor else CarTextMuted.copy(alpha = 0.85f),
                    modifier = Modifier.padding(vertical = 0.5.dp)
                )
            }
        }
    }
}
