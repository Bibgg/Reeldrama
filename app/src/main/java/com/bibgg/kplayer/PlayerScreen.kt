package com.bibgg.kplayer

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(videoUri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
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

    SideEffect {
        // Allow all four natural device orientations while the video is open:
        // portrait, reverse portrait, landscape, and reverse landscape.
        if (!orientationLocked) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(player, duration) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { point ->
                            if (point.x < size.width / 2f) {
                                player.seekTo((player.currentPosition - 1_000L).coerceAtLeast(0L))
                            } else {
                                val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                player.seekTo((player.currentPosition + 1_000L).coerceAtMost(end))
                            }
                            controlsVisible = true
                        }
                    )
                },
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    setPlayer(player)
                    useController = false
                    resizeMode = zoomMode
                    setKeepContentOnPlayerReset(true)
                }
            },
            update = { view ->
                view.setPlayer(player)
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
                    TextButton(onClick = { player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L)) }) {
                        Text("-10s", color = Color.White, fontSize = 12.sp)
                    }
                    TextButton(onClick = { player.seekTo((player.currentPosition - 5_000L).coerceAtLeast(0L)) }) {
                        Text("-5s", color = Color.White, fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(52.dp)
                            .pointerInput(player) {
                                awaitEachGesture {
                                    awaitFirstDown()
                                    var longPressStarted = false
                                    val repeatJob = launch {
                                        delay(350)
                                        longPressStarted = true
                                        while (true) {
                                            player.seekTo((player.currentPosition - 1_000L).coerceAtLeast(0L))
                                            delay(250)
                                        }
                                    }
                                    val released = waitForUpOrCancellation()
                                    repeatJob.cancel()
                                    if (released != null && !longPressStarted) {
                                        player.seekTo((player.currentPosition - 1_000L).coerceAtLeast(0L))
                                    }
                                }
                            }
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = "Back 1 second; hold to keep rewinding", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    FilledIconButton(
                        onClick = {
                            if (player.isPlaying) player.pause() else player.play()
                        },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFB8F36B),
                            contentColor = Color(0xFF10150F)
                        )
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(52.dp)
                            .pointerInput(player) {
                                awaitEachGesture {
                                    awaitFirstDown()
                                    var longPressStarted = false
                                    val repeatJob = launch {
                                        delay(350)
                                        longPressStarted = true
                                        while (true) {
                                            val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                            player.seekTo((player.currentPosition + 1_000L).coerceAtMost(end))
                                            delay(250)
                                        }
                                    }
                                    val released = waitForUpOrCancellation()
                                    repeatJob.cancel()
                                    if (released != null && !longPressStarted) {
                                        val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                        player.seekTo((player.currentPosition + 1_000L).coerceAtMost(end))
                                    }
                                }
                            }
                    ) {
                        Icon(Icons.Default.Forward, contentDescription = "Forward 1 second; hold to keep seeking", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    TextButton(onClick = {
                        val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                        player.seekTo((player.currentPosition + 5_000L).coerceAtMost(end))
                    }) {
                        Text("+5s", color = Color.White, fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                        player.seekTo((player.currentPosition + 10_000L).coerceAtMost(end))
                    }) {
                        Text("+10s", color = Color.White, fontSize = 12.sp)
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
                        "Tap ±1s once, hold to move continuously; use ±5s or ±10s for quick jumps",
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
