package com.example.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
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
import com.example.ui.theme.OnyxBlack
import com.example.ui.theme.RubyRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Guarantees that only one MediaPlayer in the reels pager can produce audio/video.
 * Adjacent pager pages may be composed at the same time, so each player must claim
 * playback ownership before starting.
 */
private object ReelPlaybackCoordinator {
    private var activePlayer: MediaPlayer? = null

    @Synchronized
    fun claim(player: MediaPlayer) {
        val previous = activePlayer
        if (previous !== player) {
            try {
                if (previous?.isPlaying == true) previous.pause()
            } catch (_: Exception) {}
            activePlayer = player
        }
    }

    @Synchronized
    fun release(player: MediaPlayer) {
        if (activePlayer === player) activePlayer = null
    }
}

data class FloatingHeart(
    val id: Long,
    val offset: Offset,
    val rotation: Float
)

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
    val coroutineScope = rememberCoroutineScope()
    // Read the latest page state inside asynchronous MediaPlayer callbacks.
    val latestIsPlaying = rememberUpdatedState(isPlaying)
    val latestIsMuted = rememberUpdatedState(isMuted)
    val latestPlaybackSpeed = rememberUpdatedState(playbackSpeed)

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var isReady by remember { mutableStateOf(false) }
    var surfaceTextureRef by remember { mutableStateOf<SurfaceTexture?>(null) }
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }

    // Floating heart particle animation on double tap
    val hearts = remember { mutableStateListOf<FloatingHeart>() }

    // Play/Pause indicator flash animation
    var showPlayPauseFlash by remember { mutableStateOf(false) }

    // Monitor position periodically
    LaunchedEffect(mediaPlayer, isReady) {
        while (isActive) {
            val player = mediaPlayer
            if (player != null && isReady) {
                try {
                    val current = player.currentPosition.toLong()
                    val total = player.duration.toLong()
                    onProgressUpdate(current, total)
                } catch (_: Exception) {}
            }
            delay(250)
        }
    }

    // React to isPlaying changes
    LaunchedEffect(isPlaying, isReady) {
        val player = mediaPlayer
        if (player != null && isReady) {
            try {
                if (isPlaying) {
                    ReelPlaybackCoordinator.claim(player)
                    if (!player.isPlaying) player.start()
                } else {
                    if (player.isPlaying) player.pause()
                    ReelPlaybackCoordinator.release(player)
                }
            } catch (_: Exception) {}
        }
    }

    // React to isMuted changes
    LaunchedEffect(isMuted, isReady) {
        val player = mediaPlayer
        if (player != null && isReady) {
            try {
                val vol = if (isMuted) 0f else 1f
                player.setVolume(vol, vol)
            } catch (_: Exception) {}
        }
    }

    // React to playback speed changes
    LaunchedEffect(playbackSpeed, isReady) {
        val player = mediaPlayer
        if (player != null && isReady && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val params = player.playbackParams
                params.speed = playbackSpeed
                player.playbackParams = params
            } catch (_: Exception) {}
        }
    }

    // Release player when URL changes or unmounted
    DisposableEffect(videoUrl) {
        isBuffering = true
        isReady = false

        val player = MediaPlayer().apply {
            setOnPreparedListener { mp ->
                isBuffering = false
                isReady = true
                surfaceRef?.let { mp.setSurface(it) }
                val vol = if (latestIsMuted.value) 0f else 1f
                mp.setVolume(vol, vol)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    try {
                        val p = mp.playbackParams
                        p.speed = latestPlaybackSpeed.value
                        mp.playbackParams = p
                    } catch (_: Exception) {}
                }
                // A page may have stopped being current while prepareAsync was running.
                if (latestIsPlaying.value) {
                    ReelPlaybackCoordinator.claim(mp)
                    mp.start()
                } else {
                    try { mp.pause() } catch (_: Exception) {}
                }
            }
            setOnBufferingUpdateListener { _, percent ->
                if (percent < 100 && !isPlaying) {
                    isBuffering = true
                } else {
                    isBuffering = false
                }
            }
            setOnCompletionListener {
                onVideoCompleted()
            }
            setOnErrorListener { _, _, _ ->
                isBuffering = false
                true
            }
        }

        try {
            player.setDataSource(context, Uri.parse(videoUrl))
            player.prepareAsync()
        } catch (_: Exception) {
            isBuffering = false
        }

        mediaPlayer = player

        onDispose {
            ReelPlaybackCoordinator.release(player)
            try {
                if (player.isPlaying) player.stop()
                player.reset()
                player.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OnyxBlack)
            .testTag("video_player_container")
            .pointerInput(videoUrl) {
                detectTapGestures(
                    onTap = {
                        onSingleTap()
                    },
                    onDoubleTap = { offset ->
                        val heart = FloatingHeart(
                            id = System.currentTimeMillis(),
                            offset = offset,
                            rotation = (-20..20).random().toFloat()
                        )
                        hearts.add(heart)
                        onDoubleTapHeart(offset.x, offset.y)
                        coroutineScope.launch {
                            delay(900)
                            hearts.remove(heart)
                        }
                    }
                )
            }
    ) {
        // Video Viewport using TextureView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            surfaceTextureRef = st
                            val s = Surface(st)
                            surfaceRef = s
                            mediaPlayer?.setSurface(s)
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            surfaceRef?.release()
                            surfaceRef = null
                            surfaceTextureRef = null
                            mediaPlayer?.setSurface(null)
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            },
            update = { textureView ->
                surfaceRef?.let { s ->
                    mediaPlayer?.setSurface(s)
                }
            }
        )

        // Subtle cinematic vignetting & gradients (for captions & overlay legibility)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Loading indicator
        if (isBuffering && !isReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = RubyRed,
                    strokeWidth = 3.dp
                )
            }
        }

        // Pause indicator stays visible while paused; single tap is reserved for showing/hiding controls.
        AnimatedVisibility(
            visible = !isPlaying && isReady,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Black.copy(alpha = 0.55f), shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Blooming double-tap hearts
        hearts.forEach { heart ->
            key(heart.id) {
                AnimatedHeart(offset = heart.offset, rotation = heart.rotation)
            }
        }
    }
}

@Composable
private fun AnimatedHeart(
    offset: Offset,
    rotation: Float
) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1.6f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)
            )
        }
        launch {
            offsetY.animateTo(-120f, animationSpec = spring(stiffness = Spring.StiffnessLow))
        }
        launch {
            delay(400)
            alpha.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessVeryLow))
        }
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (offset.x - 40f).roundToInt(),
                    y = (offset.y - 40f + offsetY.value).roundToInt()
                )
            }
            .scale(scale.value)
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Liked",
            tint = RubyRed.copy(alpha = alpha.value),
            modifier = Modifier.size(64.dp)
        )
    }
}
