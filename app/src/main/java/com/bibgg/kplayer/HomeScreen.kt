package com.bibgg.kplayer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    videos: List<LocalVideo>,
    hasVideoAccess: Boolean,
    onRequestVideoAccess: () -> Unit,
    onOpenLocalVideo: (Uri) -> Unit,
    onOpenVideo: () -> Unit
) {
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
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

                Spacer(Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VIDEOS ON THIS DEVICE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(videos.size.toString(), color = Color(0xFFB8F36B), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))

                when {
                    !hasVideoAccess -> {
                        Text("Allow video access to show videos stored on your phone.", color = Color(0xFF9BA69D), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onRequestVideoAccess) { Text("Allow access", color = Color(0xFFB8F36B)) }
                    }
                    videos.isEmpty() -> {
                        Text("No videos found on this device.", color = Color(0xFF9BA69D), fontSize = 12.sp)
                    }
                    else -> {
                        videos.forEach { video ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenLocalVideo(video.uri) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play video", tint = Color(0xFFB8F36B))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(video.name, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatLibraryDuration(video.durationMs), color = Color(0xFF9BA69D), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
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

private fun formatLibraryDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
