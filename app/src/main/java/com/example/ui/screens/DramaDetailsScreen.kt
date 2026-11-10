package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DramaSeries
import com.example.ui.theme.OnyxBlack
import com.example.ui.theme.RubyRed
import com.example.ui.theme.TextSecondary

@Composable
fun DramaDetailsScreen(
    drama: DramaSeries,
    onBack: () -> Unit,
    onWatch: (Int) -> Unit
) {
    var selectedEpisodeIndex by androidx.compose.runtime.remember(drama.id) { androidx.compose.runtime.mutableIntStateOf(drama.episodes.indexOfFirst { it.videoUrl.isNotBlank() }.coerceAtLeast(0)) }
    val hasPlayableEpisode = drama.episodes.any { it.videoUrl.isNotBlank() }
    val selectedEpisode = drama.episodes.getOrNull(selectedEpisodeIndex)
    val canWatchSelectedEpisode = selectedEpisode?.videoUrl?.isNotBlank() == true
    Column(
        modifier = Modifier.fillMaxSize().background(OnyxBlack).verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(430.dp)) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(drama.posterColorHex), OnyxBlack))
                )
            )
            if (drama.coverImageUrl.isNotBlank()) {
                AsyncImage(
                    model = drama.coverImageUrl,
                    contentDescription = "${drama.title} poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.92f)))
                    )
                )
            }
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(top = 34.dp, start = 12.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp)) {
                Text("REELDRAMA ORIGINAL", color = RubyRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(drama.title, color = Color.White, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(drama.genre, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(14.dp))
                        Text(" ${drama.rating}", color = Color.White, fontSize = 12.sp)
                    }
                    Text("${drama.totalEpisodes} एपिसोड", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onWatch(selectedEpisodeIndex) },
                    enabled = canWatchSelectedEpisode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RubyRed, disabledContainerColor = Color(0xFF393943)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text(if (canWatchSelectedEpisode) "Episode ${selectedEpisode?.episodeNumber} अभी देखें" else "एपिसोड जल्द उपलब्ध होंगे", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text("कहानी", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(drama.synopsis.ifBlank { "इस ड्रामा की कहानी जल्द उपलब्ध होगी।" }, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(20.dp))
            Text("एपिसोड (${drama.episodes.size})", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            if (drama.episodes.isEmpty()) {
                Text("अभी कोई एपिसोड उपलब्ध नहीं है।", color = TextSecondary, fontSize = 13.sp)
            } else {
                drama.episodes.sortedBy { it.episodeNumber }.forEach { episode ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp).clip(RoundedCornerShape(10.dp)).background(if (selectedEpisodeIndex == drama.episodes.indexOf(episode)) Color(0xFF38202A) else Color(0xFF1B1B25)).clickable(enabled = episode.videoUrl.isNotBlank()) { selectedEpisodeIndex = drama.episodes.indexOf(episode) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF30303C)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Episode ${episode.episodeNumber}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(episode.title, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                        }
                        Text(if (episode.videoUrl.isNotBlank()) "Ready" else "Soon", color = if (episode.videoUrl.isNotBlank()) Color(0xFF65D69A) else TextSecondary, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
