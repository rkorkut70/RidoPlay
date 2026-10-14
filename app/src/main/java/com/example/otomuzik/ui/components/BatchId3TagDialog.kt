package com.example.otomuzik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.otomuzik.R
import com.example.otomuzik.theme.*

/**
 * Bir klasördeki tüm şarkıların ID3 etiketlerini (Sanatçı, Albüm, Tür, Yıl)
 * tek seferde toplu olarak güncellemek için kullanılan diyalog.
 */
@Composable
fun BatchId3TagDialog(
    folderName: String,
    folderPath: String,
    songCount: Int,
    accentColor: Color = CarCyan,
    onSave: (artist: String, album: String, genre: String, year: String) -> Unit,
    onDismiss: () -> Unit
) {
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    val hasAnyValue = artist.isNotBlank() || album.isNotBlank() || genre.isNotBlank() || year.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(CarSurfaceDark)
                .border(2.dp, CarBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x28FFB300)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder),
                            contentDescription = null,
                            tint = CarAmber,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Toplu ID3 Düzenle",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarTextPrimary
                        )
                        Text(
                            text = "$folderName • $songCount parça",
                            fontSize = 13.sp,
                            color = CarTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Boş bırakılan alanlar mevcut halleriyle korunur. Sadece doldurduğunuz alanlar klasördeki tüm şarkılara uygulanır.",
                    fontSize = 12.sp,
                    color = CarTextMuted,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Ortak Sanatçı
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Ortak Sanatçı") },
                    placeholder = { Text("Mevcut sanatçıları korumak için boş bırakın") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = CarBorder,
                        focusedTextColor = CarTextPrimary,
                        unfocusedTextColor = CarTextPrimary,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = CarTextMuted,
                        focusedContainerColor = CarSurfaceVariant,
                        unfocusedContainerColor = CarSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Ortak Albüm
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Ortak Albüm") },
                    placeholder = { Text("Mevcut albümleri korumak için boş bırakın") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = CarBorder,
                        focusedTextColor = CarTextPrimary,
                        unfocusedTextColor = CarTextPrimary,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = CarTextMuted,
                        focusedContainerColor = CarSurfaceVariant,
                        unfocusedContainerColor = CarSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Ortak Müzik Türü & Yıl Yan Yana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Tür (Genre)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = CarBorder,
                            focusedTextColor = CarTextPrimary,
                            unfocusedTextColor = CarTextPrimary,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = CarTextMuted,
                            focusedContainerColor = CarSurfaceVariant,
                            unfocusedContainerColor = CarSurfaceVariant
                        ),
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = year,
                        onValueChange = { if (it.length <= 4) year = it },
                        label = { Text("Yıl") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = CarBorder,
                            focusedTextColor = CarTextPrimary,
                            unfocusedTextColor = CarTextPrimary,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = CarTextMuted,
                            focusedContainerColor = CarSurfaceVariant,
                            unfocusedContainerColor = CarSurfaceVariant
                        ),
                        modifier = Modifier.weight(0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CarSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Vazgeç", color = CarTextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (hasAnyValue) {
                                onSave(artist.trim(), album.trim(), genre.trim(), year.trim())
                                onDismiss()
                            }
                        },
                        enabled = hasAnyValue,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            disabledContainerColor = CarSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Tümüne Uygula",
                            color = if (hasAnyValue) Color(0xFF0C0E12) else CarTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
