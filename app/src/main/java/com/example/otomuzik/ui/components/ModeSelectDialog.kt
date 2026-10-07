package com.example.otomuzik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.otomuzik.theme.BaseCarCyan
import com.example.otomuzik.theme.CarBorder
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarSurfaceVariant
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.theme.CarTextPrimary

/**
 * İlk açılışta veya Ayarlar menüsünden çağrılan Cihaz Modu Seçim Penceresi.
 * Kullanıcı "Araç / Tablet", "Cep Telefonu" veya "Otomatik" modlarını seçebilir.
 */
@Composable
fun ModeSelectDialog(
    currentMode: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(22.dp))
                .background(CarSurfaceDark)
                .border(1.5.dp, BaseCarCyan.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Arayüz Modu Seçimi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = BaseCarCyan,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "RidoPlay'i hangi cihazda kullanacaksınız?\n(Dilediğiniz zaman Ayarlar'dan değiştirebilirsiniz)",
                    fontSize = 13.sp,
                    color = CarTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Seçenek 1: Araç / Tablet Modu
                ModeOptionCard(
                    icon = "🚗",
                    title = "Araç / Tablet Modu",
                    subtitle = "Yatay ekran, dev sürüş butonları, bölünmüş çalma paneli",
                    isSelected = selectedMode == "CAR",
                    onClick = { selectedMode = "CAR" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Seçenek 2: Cep Telefonu Modu
                ModeOptionCard(
                    icon = "📱",
                    title = "Cep Telefonu Modu",
                    subtitle = "Dikey arayüz, tek elle kullanım, altta Mini-Player barı",
                    isSelected = selectedMode == "PHONE",
                    onClick = { selectedMode = "PHONE" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Seçenek 3: Otomatik Mod
                ModeOptionCard(
                    icon = "⚡",
                    title = "Otomatik Algıla (Tavsiye Edilen)",
                    subtitle = "Dikey tutulduğunda telefon, yatay çevrildiğinde araç paneli",
                    isSelected = selectedMode == "AUTO",
                    onClick = { selectedMode = "AUTO" }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onConfirm(selectedMode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BaseCarCyan,
                        contentColor = Color(0xFF0C0E12)
                    )
                ) {
                    Text(
                        text = "Seçimi Kaydet ve Başla",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeOptionCard(
    icon: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) BaseCarCyan else CarBorder
    val bgColor = if (isSelected) BaseCarCyan.copy(alpha = 0.12f) else CarSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 1.8.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = icon,
                fontSize = 26.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) BaseCarCyan else CarTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = CarTextMuted,
                    lineHeight = 15.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = BaseCarCyan,
                    unselectedColor = CarBorder
                )
            )
        }
    }
}
