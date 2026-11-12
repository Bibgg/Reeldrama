package com.bibgg.kplayer

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
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
    var gestureIndicator by remember { mutableStateOf("") }
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }
    val musicRingTransition = rememberInfiniteTransition(label = "music-ring")
    val musicRingRotation by musicRingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "music-ring-rotation"
    )
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
            player.seekTo((player.currentPosition - 300L).coerceAtLeast(0L))
            delay(100)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!backgroundMusicEnabled) {
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

        }
        if (!backgroundMusicEnabled) {
            Box(
                modifier = Modifier.fillMaxSize().pointerInput(activity) {
                    detectDragGestures(onDragStart = { seekDeltaMs = 0L }) { change, dragAmount ->
                        val leftHalf = change.position.x < size.width / 2f
                        change.consume()
                        if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                            val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                            val delta = (dragAmount.x * 80f).toLong()
                            player.seekTo((player.currentPosition + delta).coerceIn(0L, end))
                            seekDeltaMs += delta
                            gestureIndicator = if (seekDeltaMs >= 0L) "SEEK FORWARD" else "SEEK BACKWARD"
                            gestureValue = (player.currentPosition.toFloat() / end.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        } else if (leftHalf) {
                            val current = activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
                            val updated = (current - dragAmount.y / size.height).coerceIn(0.01f, 1f)
                            val params = activity?.window?.attributes
                            if (params != null) { params.screenBrightness = updated; activity?.window?.attributes = params }
                            gestureIndicator = "BRIGHTNESS"
                            gestureValue = updated
                        } else {
                            val audio = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
                            val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val updated = (current + if (dragAmount.y < 0f) 1 else -1).coerceIn(0, maxVolume)
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, updated, 0)
                            gestureIndicator = "VOLUME"
                            gestureValue = updated.toFloat() / maxVolume.coerceAtLeast(1)
                        }
                    }
                }
            )
            if (gestureIndicator.isNotEmpty()) {
                LaunchedEffect(gestureIndicator, gestureValue) { delay(900); gestureIndicator = "" }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(color = Color(0xCC171717), shape = MaterialTheme.shapes.large) {
                        Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (gestureIndicator.startsWith("SEEK")) "$gestureIndicator  ${if (seekDeltaMs >= 0L) "+" else ""}${formatTime(kotlin.math.abs(seekDeltaMs))}" else gestureIndicator, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { gestureValue.coerceIn(0f, 1f) }, color = Color(0xFFB8F36B), trackColor = Color(0xFF666666))
                            Text((gestureValue * 100).toInt().toString() + "%", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        LaunchedEffect(backgroundMusicEnabled, player) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, backgroundMusicEnabled)
                .build()
        }
        if (backgroundMusicEnabled) {
            FullScreenMusicVisualizer(onExit = { backgroundMusicEnabled = false })
        }
        if (controlsVisible && !backgroundMusicEnabled) {
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
                    IconButton(onClick = { backgroundMusicEnabled = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Open full-screen music visualizer", tint = Color.White, modifier = Modifier.size(23.dp))
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


@Composable
private fun FullScreenMusicVisualizer(onExit: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "full-screen-music-visualizer")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rotation")
    val pulse by transition.animateFloat(0.88f, 1.08f, infiniteRepeatable(tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "pulse")
    val rainbow = listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFFF00), Color(0xFF00E676), Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFFD500F9), Color(0xFFFF1744))
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF241039), Color(0xFF080612), Color.Black))), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.27f
            val brush = Brush.sweepGradient(rainbow)
            for (i in 0..4) {
                drawCircle(
                    brush = brush,
                    center = c,
                    radius = r + i * 15.dp.toPx(),
                    style = Stroke(width = if (i == 0) 9.dp.toPx() else 3.dp.toPx()),
                    alpha = if (i == 0) 0.95f else 0.5f
                )
            }
            for (i in 0 until 72) {
                val angle = Math.toRadians(i * 5.0 + rotation)
                val inner = r + 78.dp.toPx()
                val length = (8 + (i % 7) * 2).dp.toPx()
                drawLine(rainbow[i % (rainbow.size - 1)], androidx.compose.ui.geometry.Offset(c.x + kotlin.math.cos(angle).toFloat() * inner, c.y + kotlin.math.sin(angle).toFloat() * inner), androidx.compose.ui.geometry.Offset(c.x + kotlin.math.cos(angle).toFloat() * (inner + length), c.y + kotlin.math.sin(angle).toFloat() * (inner + length)), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(142.dp).graphicsLayer { scaleX = pulse; scaleY = pulse; rotationZ = -rotation }.background(Brush.radialGradient(listOf(Color(0xFF542078), Color(0xFF160D25), Color.Black)), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, "Music playing", tint = Color.White, modifier = Modifier.size(76.dp))
            }
            Spacer(Modifier.height(34.dp))
            Text("NOW PLAYING", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 5.sp, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Text("K PLAYER", color = Color(0xFFE8C8FF), fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp, fontSize = 21.sp)
            Spacer(Modifier.height(34.dp))
            FilledTonalButton(onClick = onExit, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF2D1B3D), contentColor = Color.White)) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("BACK TO VIDEO")
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
