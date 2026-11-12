package com.bibgg.kplayer

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(videoUri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) {
        var current = context
        while (current is ContextWrapper && current !is Activity) {
            current = current.baseContext
        }
        current as? Activity
    }
    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var zoomMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var orientationLocked by remember { mutableStateOf(false) }
    var rewindHold by remember { mutableStateOf(false) }
    var backgroundMusicEnabled by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var wasPlayingBeforeBackground by remember { mutableStateOf(false) }

    SideEffect {
        // Allow all four natural device orientations while the video is open:
        // portrait, reverse portrait, landscape, and reverse landscape.
        if (!orientationLocked) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
    }

    DisposableEffect(lifecycleOwner, player, backgroundMusicEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    wasPlayingBeforeBackground = player.isPlaying
                    if (backgroundMusicEnabled) {
                        // Keep audio playing, but detach the video surface.
                        playerViewRef?.setPlayer(null)
                    } else {
                        // Background playback disabled: stop both picture and sound.
                        player.pause()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    playerViewRef?.setPlayer(player)
                    if (wasPlayingBeforeBackground) player.play()
                    wasPlayingBeforeBackground = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // ExoPlayer does not support negative playback speed. Simulate 2x rewind
    // by seeking backward twice as fast as real time while the left side is held.
    LaunchedEffect(rewindHold) {
        while (rewindHold) {
            player.seekTo((player.currentPosition - 200L).coerceAtLeast(0L))
            delay(100)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(player, duration, playbackSpeed) {
                    detectTapGestures(
                        onPress = { point ->
                            val isLeftSide = point.x < size.width / 2f
                            val released = tryAwaitRelease()
                            if (rewindHold) rewindHold = false
                            player.setPlaybackSpeed(playbackSpeed)
                            if (!released) {
                                rewindHold = false
                            }
                        },
                        onLongPress = { point ->
                            if (point.x < size.width / 2f) {
                                // Hold left side: rewind at an effective 2x rate.
                                rewindHold = true
                            } else {
                                // Hold right side: play forward at 2x speed.
                                rewindHold = false
                                player.setPlaybackSpeed(2f)
                            }
                        },
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { point ->
                            // Double-tap skips 5 seconds in either direction.
                            if (point.x < size.width / 2f) {
                                rewindHold = false
                                player.setPlaybackSpeed(playbackSpeed)
                                player.seekTo((player.currentPosition - 5_000L).coerceAtLeast(0L))
                            } else {
                                val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                player.seekTo((player.currentPosition + 5_000L).coerceAtMost(end))
                            }
                            controlsVisible = true
                        }
                    )
                },
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    setPlayer(player)
                    playerViewRef = this
                    useController = false
                    resizeMode = zoomMode
                    setKeepContentOnPlayerReset(true)
                }
            },
            update = { view ->
                playerViewRef = view
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    view.setPlayer(player)
                }
                view.resizeMode = zoomMode
            }
        )

        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "K PLAYER",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { backgroundMusicEnabled = !backgroundMusicEnabled }
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = if (backgroundMusicEnabled) "Background audio on" else "Background audio off",
                            tint = if (backgroundMusicEnabled) Color(0xFFB8F36B) else Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            if (orientationLocked) {
                                orientationLocked = false
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                            } else {
                                val rotation = activity?.windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
                                val lockedOrientation = when (rotation) {
                                    Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                                orientationLocked = true
                                activity?.requestedOrientation = lockedOrientation
                            }
                        }
                    ) {
                        Icon(
                            if (orientationLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (orientationLocked) "Unlock screen rotation" else "Lock current screen orientation",
                            tint = if (orientationLocked) Color(0xFFB8F36B) else Color.White
                        )
                    }
                    Box {
                        TextButton(onClick = { speedMenuOpen = true }) {
                            Text("${playbackSpeed}x", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = speedMenuOpen,
                            onDismissRequest = { speedMenuOpen = false }
                        ) {
                            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    onClick = {
                                        playbackSpeed = speed
                                        player.setPlaybackSpeed(speed)
                                        speedMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { player.seekTo((player.currentPosition - 1_000L).coerceAtLeast(0L)) },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = "Back 1 second", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.width(28.dp))
                    FilledIconButton(
                        onClick = {
                            if (player.isPlaying) player.pause() else player.play()
                        },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFB8F36B),
                            contentColor = Color(0xFF10150F)
                        )
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(Modifier.width(28.dp))
                    IconButton(
                        onClick = {
                            val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                            player.seekTo((player.currentPosition + 1_000L).coerceAtMost(end))
                        },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(Icons.Default.Forward, contentDescription = "Forward 1 second", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }

                Column {
                    Slider(
                        value = position.toFloat().coerceIn(0f, duration.coerceAtLeast(1L).toFloat()),
                        onValueChange = { player.seekTo(it.toLong()) },
                        valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFB8F36B),
                            activeTrackColor = Color(0xFFB8F36B),
                            inactiveTrackColor = Color(0xFF777777)
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(position), color = Color.White, fontSize = 12.sp)
                        Text(" / ${formatTime(duration)}", color = Color(0xFFB6B6B6), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            zoomMode = if (zoomMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Fit or fill", tint = Color.White)
                        }
                        Text(
                            if (zoomMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) "FIT" else "FILL",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Background audio: ${if (backgroundMusicEnabled) "ON" else "OFF"}  •  Hold left/right: 2x rewind/play  •  Double tap: skip 5 seconds",
                        color = Color(0xFFB6B6B6),
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
