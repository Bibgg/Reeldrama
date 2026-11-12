package com.bibgg.kplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(onOpenVideo: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B0A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFB8F36B), Color(0xFF55BFA5))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF0B100D),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("K PLAYER", fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("YOUR VIDEO SPACE", fontSize = 9.sp, color = Color(0xFF9BA69D), letterSpacing = 2.sp)
                    }
                }

                Spacer(Modifier.height(62.dp))
                Text("Every frame.", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Your control.", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB8F36B))
                Spacer(Modifier.height(16.dp))
                Text(
                    "A fresh home for the videos you love.",
                    color = Color(0xFF9BA69D),
                    fontSize = 15.sp
                )

                Spacer(Modifier.height(42.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1F3028), Color(0xFF111A16), Color(0xFF17231C))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFFB8F36B),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Made for the big picture", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Local playback is ready to explore", color = Color(0xFF9BA69D), fontSize = 12.sp)
                    }
                }
            }

            Column {
                Button(
                    onClick = onOpenVideo,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB8F36B),
                        contentColor = Color(0xFF10150F)
                    )
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Open a video", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Choose a video stored on your device. Online library and cloud streaming will come in a later milestone.",
                    color = Color(0xFF778179),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
