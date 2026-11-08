package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.example.model.DramaSeries
import com.example.ui.theme.OnyxBlack
import com.example.ui.theme.RubyRed
import com.example.ui.theme.TextSecondary

@Composable
fun ReelDramaHomeScreen(
    dramas: List<DramaSeries>,
    onPlayDrama: (String) -> Unit,
    onBrowseAll: () -> Unit
) {
    val featured = dramas.firstOrNull { it.isTrending } ?: dramas.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(OnyxBlack),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("REEL", color = RubyRed, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("DRAMA", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("Discover", color = TextSecondary, fontSize = 13.sp)
            }
        }

        if (featured != null) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(featured.posterColorHex), Color(0xFF120D17), OnyxBlack)
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text("⭐  FEATURED SERIES", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                featured.title,
                                color = Color.White,
                                fontSize = 27.sp,
                                lineHeight = 31.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${featured.genre}  •  ${featured.totalEpisodes} Episodes  •  ★ ${featured.rating}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                featured.synopsis,
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onPlayDrama(featured.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = RubyRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Play Episode 1", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            DramaSection(
                title = "Trending Now",
                subtitle = "Popular stories people are watching",
                dramas = dramas.filter { it.isTrending },
                onPlayDrama = onPlayDrama
            )
        }

        item {
            DramaSection(
                title = "Short Dramas",
                subtitle = "Quick stories. Big twists.",
                dramas = dramas,
                onPlayDrama = onPlayDrama
            )
        }

        item {
            DramaSection(
                title = "More to Explore",
                subtitle = "Discover your next favorite drama",
                dramas = dramas,
                onPlayDrama = onPlayDrama
            )
        }

        item {
            Button(
                onClick = onBrowseAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242431)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Browse all series", color = Color.White)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DramaSection(
    title: String,
    subtitle: String,
    dramas: List<DramaSeries>,
    onPlayDrama: (String) -> Unit
) {
    Column {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)
        ) {
            items(dramas, key = { it.id }) { drama ->
                Column(
                    modifier = Modifier
                        .width(142.dp)
                        .clickable { onPlayDrama(drama.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(drama.posterColorHex), Color(0xFF15131B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(drama.title.take(1).uppercase(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                            Text("REELDRAMA", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        if (drama.isTrending) {
                            Text(
                                "TOP 10",
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(RubyRed)
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(drama.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(12.dp))
                        Text(" ${drama.rating}  •  ${drama.genre}", color = TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
