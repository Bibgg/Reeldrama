package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.OnyxBlack
import com.example.ui.theme.RubyRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class FloatingHeart(val id: Long, val offset: Offset, val rotation: Float)

/** Media3/ExoPlayer handles HLS (.m3u8), adaptive quality and expiring stream URLs. */
@Composable
fun VideoSurfacePlayer(
    videoUrl: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSingleTap: () -> Unit = onTogglePlayPause,
    onDoubleTapHeart: (x: Float, y: Float) -> Unit,
    onVideoCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestIsPlaying = rememberUpdatedState(isPlaying)
    val latestCompleted = rememberUpdatedState(onVideoCompleted)
    val latestProgress = rememberUpdatedState(onProgressUpdate)
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var buffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    val hearts = remember { mutableStateListOf<FloatingHeart>() }

    DisposableEffect(videoUrl) {
        buffering = true
        playerError = null
        if (videoUrl.isBlank()) {
            player = null
            buffering = false
            onDispose { }
        } else {
            val exo = ExoPlayer.Builder(context).build()
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    buffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_ENDED) latestCompleted.value()
                }
                override fun onPlayerError(error: PlaybackException) {
                    buffering = false
                    playerError = error.errorCodeName
                }
            }
            exo.addListener(listener)
            exo.setMediaItem(MediaItem.fromUri(videoUrl))
            exo.prepare()
            exo.volume = if (isMuted) 0f else 1f
            exo.playWhenReady = latestIsPlaying.value
            player = exo
            onDispose {
                exo.removeListener(listener)
                exo.release()
                if (player === exo) player = null
            }
        }
    }

    LaunchedEffect(player, isPlaying) {
        player?.let { it.playWhenReady = isPlaying }
    }
    LaunchedEffect(player, isMuted) {
        player?.volume = if (isMuted) 0f else 1f
    }
    LaunchedEffect(player, playbackSpeed) {
        player?.setPlaybackSpeed(playbackSpeed.coerceIn(0.25f, 2f))
    }
    LaunchedEffect(player) {
        while (isActive) {
            player?.let { exo ->
                latestProgress.value(exo.currentPosition.coerceAtLeast(0L), exo.duration.takeIf { it > 0 } ?: 0L)
            }
            delay(500L)
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(OnyxBlack)
            .testTag("video_player_container")
            .pointerInput(videoUrl) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        val heart = FloatingHeart(System.currentTimeMillis(), offset, (-20..20).random().toFloat())
                        hearts.add(heart)
                        onDoubleTapHeart(offset.x, offset.y)
                        scope.launch { delay(900L); hearts.remove(heart) }
                    }
                )
            }
    ) {
        if (player != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        player = this@VideoSurfacePlayer.let { null }
                    }
                },
                update = { view -> view.player = player }
            )
        }

        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)
        ))))

        if (buffering && playerError == null && videoUrl.isNotBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = RubyRed, strokeWidth = 3.dp)
            }
        }
        if (playerError != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("वीडियो नहीं चल पाया: $playerError", color = Color.White)
            }
        }
        if (videoUrl.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("इस एपिसोड का वीडियो लिंक उपलब्ध नहीं है", color = Color.White)
            }
        }
        AnimatedVisibility(
            visible = !isPlaying && player != null,
            enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Paused", tint = Color.White, modifier = Modifier.size(64.dp))
        }
        hearts.forEach { heart ->
            key(heart.id) { AnimatedHeart(heart.offset, heart.rotation) }
        }
    }
}

@Composable
private fun AnimatedHeart(offset: Offset, rotation: Float) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1.6f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)) }
        launch { offsetY.animateTo(-120f, spring(stiffness = Spring.StiffnessLow)) }
        launch { delay(400L); alpha.animateTo(0f, spring(stiffness = Spring.StiffnessVeryLow)) }
    }
    Box(Modifier.offset { IntOffset((offset.x - 40f).roundToInt(), (offset.y - 40f + offsetY.value).roundToInt()) }.scale(scale.value)) {
        Icon(Icons.Default.Favorite, contentDescription = "Liked", tint = RubyRed.copy(alpha = alpha.value), modifier = Modifier.size(64.dp))
    }
}
