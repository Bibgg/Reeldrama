package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DramaSeries
import com.example.model.Episode
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldVIP
import com.example.ui.theme.RubyRed

@Composable
fun ReelOverlayControls(
    drama: DramaSeries,
    episode: Episode,
    currentPositionMs: Long,
    durationMs: Long,
    isMuted: Boolean,
    playbackSpeed: Float,
    subtitlesEnabled: Boolean,
    currentSubtitleText: String,
    isBookmarked: Boolean,
    onToggleMute: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenEpisodes: () -> Unit,
    onOpenSeriesBrowser: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenBackendConfig: () -> Unit,
    onNextEpisode: () -> Unit,
    onPrevEpisode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedHook by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand and Drama series selector pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .clickable { onOpenSeriesBrowser() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("series_selector_pill")
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(RubyRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reel",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "REELDRAMA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RubyRed,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = drama.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Switch Series",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
            }

            // Quick Top Utilities (Sound, Speed, CC, API)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Sound toggle
                QuickUtilityButton(
                    icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    label = if (isMuted) "Mute" else "On",
                    onClick = onToggleMute,
                    testTag = "btn_sound_toggle"
                )

                // Speed toggle
                QuickUtilityButton(
                    icon = Icons.Default.Speed,
                    label = "${playbackSpeed}x",
                    onClick = onCycleSpeed,
                    testTag = "btn_speed_toggle"
                )

                // CC Subtitles
                QuickUtilityButton(
                    icon = Icons.Default.ClosedCaption,
                    label = if (subtitlesEnabled) "CC On" else "CC Off",
                    active = subtitlesEnabled,
                    onClick = onToggleSubtitles,
                    testTag = "btn_subtitles_toggle"
                )

                // Backend sync status / config button
                IconButton(
                    onClick = onOpenBackendConfig,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .testTag("btn_backend_config")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "API Status",
                        tint = GoldVIP,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // RIGHT ACTION SIDEBAR
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Series Poster Avatar / Follow button
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(drama.posterColorHex))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onOpenSeriesBrowser() },
                    contentAlignment = Alignment.Center
                ) {
                    if (drama.coverImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = drama.coverImageUrl,
                            contentDescription = "${drama.title} poster",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = drama.title.take(2).uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(RubyRed)
                        .clickable { onToggleBookmark() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBookmarked) "✓" else "+",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Like Button
            val heartColor by animateColorAsState(
                targetValue = if (episode.isLiked) RubyRed else Color.White,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "heartColor"
            )
            SidebarActionButton(
                icon = if (episode.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = formatCount(episode.likesCount),
                tint = heartColor,
                onClick = onToggleLike,
                testTag = "sidebar_btn_like"
            )

            // Episodes Button (Quick Jump)
            SidebarActionButton(
                icon = Icons.Default.Layers,
                label = "Ep.${episode.episodeNumber}",
                tint = GoldVIP,
                onClick = onOpenEpisodes,
                testTag = "sidebar_btn_episodes"
            )

            // Comments Button
            SidebarActionButton(
                icon = Icons.AutoMirrored.Filled.Chat,
                label = formatCount(episode.commentsCount),
                tint = Color.White,
                onClick = onOpenComments,
                testTag = "sidebar_btn_comments"
            )

            // Bookmark / Watchlist Button
            SidebarActionButton(
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = if (isBookmarked) "Saved" else "Save",
                tint = if (isBookmarked) GoldVIP else Color.White,
                onClick = onToggleBookmark,
                testTag = "sidebar_btn_bookmark"
            )

            // Share Button
            SidebarActionButton(
                icon = Icons.Default.Share,
                label = formatCount(episode.sharesCount),
                tint = Color.White,
                onClick = onShare,
                testTag = "sidebar_btn_share"
            )
        }

        // SUBTITLES FLOATING CAPTION
        if (subtitlesEnabled && currentSubtitleText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 145.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = currentSubtitleText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // BOTTOM EPISODE INFO & PLAYBACK CONTROLS
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .padding(start = 16.dp, bottom = 10.dp)
            ) {
                // Genre badge & Episode count badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = RubyRed.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RubyRed)
                    ) {
                        Text(
                            text = drama.genre,
                            color = RubyRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${drama.viewsCountFormatted} Views",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Episode Quick Jump Bar button (e.g. "EPISODE 03 / 45 >")
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(RubyRed.copy(alpha = 0.85f), DarkSurface)))
                        .clickable { onOpenEpisodes() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_episode_jump_bar"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Episodes",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EPISODE ${episode.episodeNumber} OF ${drama.totalEpisodes}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• All Episodes",
                        color = GoldVIP,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Expand",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Episode Title
                Text(
                    text = "Ep.${episode.episodeNumber} - ${episode.title}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Dramatic Hook / Description (clickable to expand)
                if (episode.introHook.isNotEmpty()) {
                    Text(
                        text = episode.introHook,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = if (expandedHook) 4 else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable { expandedHook = !expandedHook }
                            .padding(top = 2.dp)
                    )
                }
            }

            // SCRUBBER TIMELINE & CONTROLS BAR
            val progressFraction = if (durationMs > 0) {
                (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev button
                IconButton(
                    onClick = onPrevEpisode,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Episode",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Progress Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = RubyRed,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }

                // Next button
                IconButton(
                    onClick = onNextEpisode,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Episode",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Time Indicator (e.g. 01:14 / 01:30)
                Text(
                    text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QuickUtilityButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) RubyRed.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.45f))
            .border(
                1.dp,
                if (active) RubyRed else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) Color.White else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SidebarActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
