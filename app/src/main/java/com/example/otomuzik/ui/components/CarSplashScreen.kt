package com.example.otomuzik.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.otomuzik.R
import com.example.otomuzik.theme.BaseCarBgDark
import com.example.otomuzik.theme.BaseCarCyan
import com.example.otomuzik.theme.CarBorder
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.theme.CarTextPrimary
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Açılışta beyaz ekran yerine gösterilen araç içi selamlama ve başlangıç ekranı.
 * Ortada RidoPlay logosu, altında günün saatine göre dinamik selamlama:
 * "Günaydın, Tünaydın, İyi Akşamlar, İyi Geceler... Yeniden Hoşgeldiniz"
 */
@Composable
fun CarSplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        scale.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        // 1.5 saniye ekranda kal, sonra ana sürücü ekranına geç
        delay(1400)
        alpha.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        onSplashFinished()
    }

    // Saate göre karşılama metni
    val (greetingIcon, greetingText) = remember { getGreetingMessage() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BaseCarBgDark)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Ekrana dokunulursa anında ana ekrana geç
                onSplashFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // Ortada hafif neon ışıma efekti
        Box(
            modifier = Modifier
                .size(360.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(BaseCarCyan.copy(alpha = 0.12f), Color.Transparent),
                        radius = 450f
                    ),
                    shape = CircleShape
                )
        )

        // ── ORTA BÖLÜM: Logo ve RidoPlay Başlığı ─────────────────────────────
        Column(
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // RidoPlay Neon Rozet Logosu
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .background(CarSurfaceDark, CircleShape)
                    .border(2.5.dp, BaseCarCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = "RidoPlay Logo",
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "RidoPlay",
                color = BaseCarCyan,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Araç Müzik Çalar",
                color = CarTextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }

        // ── ALT BÖLÜM: Günün Saatine Göre Karşılama Mesajı ────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .alpha(alpha.value)
        ) {
            Row(
                modifier = Modifier
                    .background(CarSurfaceDark.copy(alpha = 0.90f), RoundedCornerShape(24.dp))
                    .border(1.5.dp, BaseCarCyan.copy(alpha = 0.50f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = greetingIcon,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(
                    text = greetingText,
                    color = CarTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Günün saatine göre Türkçe selamlama üretir:
 * 05:00 - 11:59 -> Günaydın, Yeniden Hoşgeldiniz
 * 12:00 - 16:59 -> Tünaydın, Yeniden Hoşgeldiniz
 * 17:00 - 21:59 -> İyi Akşamlar, Yeniden Hoşgeldiniz
 * 22:00 - 04:59 -> İyi Geceler, Yeniden Hoşgeldiniz
 */
private fun getGreetingMessage(): Pair<String, String> {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> Pair("☀️", "Günaydın, Yeniden Hoşgeldiniz")
        in 12..16 -> Pair("🌤️", "Tünaydın, Yeniden Hoşgeldiniz")
        in 17..21 -> Pair("🌆", "İyi Akşamlar, Yeniden Hoşgeldiniz")
        else -> Pair("🌙", "İyi Geceler, Yeniden Hoşgeldiniz")
    }
}
