package com.example.otomuzik.ui.components

import android.content.Context
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.R
import com.example.otomuzik.model.LibraryStats
import com.example.otomuzik.theme.BaseCarBgDark
import com.example.otomuzik.theme.CarBorder
import com.example.otomuzik.theme.CarSurfaceDark
import com.example.otomuzik.theme.CarSurfaceVariant
import com.example.otomuzik.theme.CarTextMuted
import com.example.otomuzik.theme.CarTextPrimary

@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit,
    accentColor: Color,
    libraryStats: LibraryStats? = null
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "3.2"
        } catch (e: Exception) {
            "3.2"
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            color = CarSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Program Logosu (Büyük & Vurgulu)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        .border(2.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_logo),
                        contentDescription = "RidoPlay Logo",
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Uygulama Adı
                Text(
                    text = "RidoPlay",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Sürüm Rozeti
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Sürüm v$versionName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Geliştirici İmzası
                Text(
                    text = "vibecoded by RK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CarTextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Açıklama Kutusu
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CarSurfaceVariant.copy(alpha = 0.45f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarBorder.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AboutFeatureRow("🚗", "Otomobil & Mobil Cihazlar İçin Optimize")
                        AboutFeatureRow("🏷️", "Gelişmiş ID3 Etiket ve Şarkı Sözü Düzenleyici")
                        AboutFeatureRow("🎛️", "Dahili Ekolayzer, Loudness & Gapless Geçiş")
                        AboutFeatureRow("📁", "Hızlı USB ve Dahili Hafıza Klasör Gezgini")
                    }
                }

                if (libraryStats != null && libraryStats.totalSongs > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CarSurfaceVariant.copy(alpha = 0.35f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CarBorder.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Kütüphane Durumu",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🎵 ${libraryStats.totalSongs} Şarkı",
                                    fontSize = 12.sp,
                                    color = CarTextPrimary
                                )
                                Text(
                                    text = "📁 ${libraryStats.totalFolders} Klasör",
                                    fontSize = 12.sp,
                                    color = CarTextPrimary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⏱️ ${libraryStats.totalDurationFormatted}",
                                    fontSize = 12.sp,
                                    color = CarTextMuted
                                )
                                Text(
                                    text = "💾 ${libraryStats.totalSizeFormatted}",
                                    fontSize = 12.sp,
                                    color = CarTextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Kapat Butonu
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color(0xFF0C0E12)
                    )
                ) {
                    Text(
                        text = "Kapat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutFeatureRow(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = icon, fontSize = 15.sp)
        Text(
            text = text,
            fontSize = 12.sp,
            color = CarTextPrimary,
            fontWeight = FontWeight.Normal
        )
    }
}
