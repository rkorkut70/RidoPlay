package com.example.otomuzik.ui.components
import com.example.otomuzik.theme.*

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay

/**
 * Liste öğesine sıralı (staggered) belirme animasyonu uygular.
 * Her öğe kendi indeksine göre gecikmeli başlar.
 *
 * @param index     Listedeki sıra numarası
 * @param delayMs   İlk öğe için bekleme süresi (ms)
 * @param stepMs    ÖğeLER arası gecikme artışı (ms)
 */
@Composable
fun Modifier.staggeredFadeIn(
    index: Int,
    delayMs: Long = 0L,
    stepMs: Long = 40L
): Modifier {
    val visibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(index) {
        delay(delayMs + index * stepMs)
        visibleState.targetState = true
    }

    val transition = updateTransition(visibleState, label = "StaggerFade_$index")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "alpha_$index"
    ) { if (it) 1f else 0f }

    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "Scale_$index"
    ) { if (it) 1f else 0.95f }

    return this
        .alpha(alpha)
        .scale(scale)
}

/**
 * Çalan şarkı satırı için nabız atışı (pulse) alpha değeri döner.
 * isactive=true ise 0.6f–1.0f arasında sürekli titrer, false ise sabit 1f.
 */
@Composable
fun rememberPulsealpha(isactive: Boolean): Float {
    if (!isactive) return 1f
    val infiniteTransition = rememberInfiniteTransition(label = "Pulsealpha")
    val pulsealpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulsealphaValue"
    )
    return pulsealpha
}











