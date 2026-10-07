package com.example.otomuzik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.otomuzik.theme.*

@Composable
fun SleepTimerDialog(
    sleepTimerLeftMs: Long?,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(CarSurfaceDark)
                .border(1.5.dp, CarCyan, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Başlık & Kapat Butonu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💤 Uyku Zamanlayıcısı",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CarTextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CarSurfaceVariant)
                            .border(1.dp, CarBorder, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = CarTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (sleepTimerLeftMs != null) {
                    val totalSeconds = sleepTimerLeftMs / 1000
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    val timeString = String.format("%02d:%02d", minutes, seconds)
                    
                    Text(
                        text = "Kalan Süre",
                        fontSize = 14.sp,
                        color = CarTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeString,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarCyan
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onCancelTimer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Zamanlayıcıyı İİptal Et",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    val options = listOf(15, 30, 45, 60, 90, 120)
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val chunked = options.chunked(2)
                        chunked.forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowOptions.forEach { minutes ->
                                    Button(
                                        onClick = { onSetTimer(minutes) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .border(1.dp, CarBorder, RoundedCornerShape(12.dp))
                                    ) {
                                        Text(
                                            text = "$minutes Dk",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CarTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}















