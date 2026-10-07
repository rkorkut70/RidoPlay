package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mini animasyonlu çalan göstergesi — 3 çubuk nabız atışı efekti.
 * Şarkı Listesinde çalan şarkının soluna konulur.
 *
 * @param isPlaying   true ise animasyonlu, false ise sabit küçük gösterge
 * @param color       Çubuk rengi
 * @param size        Göstergenin boyutu (genişlik × yükseklik için temel)
 */
@Composable
fun PlayingIndicator(
    isPlaying: Boolean,
    color: Color,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "PlayingIndicator")

    // Üç çubuk için farklı animasyon değerileri
    val bar1 by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.7f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Bar3"
    )

    Row(
        modifier = modifier.size(size),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barWidth = (size.value / 4f).dp
        listOf(bar1, bar2, bar3).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(if (isPlaying) fraction else 0.25f)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(if (isPlaying) color else color.copy(alpha = 0.4f))
            )
        }
    }
}















