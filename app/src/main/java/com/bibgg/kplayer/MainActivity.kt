package com.bibgg.kplayer

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

data class LocalVideo(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFB8F36B),
                    secondary = Color(0xFF8FD3C7),
                    background = Color(0xFF090B0A),
                    surface = Color(0xFF151916)
                )
            ) {
                var selectedVideo by remember { mutableStateOf<Uri?>(null) }
                var localVideos by remember { mutableStateOf(emptyList<LocalVideo>()) }
                var hasVideoAccess by remember { mutableStateOf(hasVideoPermission()) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasVideoAccess = hasVideoPermission()
                    localVideos = if (hasVideoAccess) loadLocalVideos() else emptyList()
                }

                val videoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: SecurityException) {
                            // Some document providers do not offer persistable grants.
                        }
                        selectedVideo = uri
                    }
                }

                LaunchedEffect(Unit) {
                    hasVideoAccess = hasVideoPermission()
                    if (hasVideoAccess) {
                        localVideos = loadLocalVideos()
                    } else {
                        permissionLauncher.launch(videoPermissionNames())
                    }
                }

                if (selectedVideo == null) {
                    HomeScreen(
                        videos = localVideos,
                        hasVideoAccess = hasVideoAccess,
                        onRequestVideoAccess = {
                            permissionLauncher.launch(videoPermissionNames())
                        },
                        onOpenLocalVideo = { selectedVideo = it },
                        onOpenVideo = {
                            videoPicker.launch(arrayOf("video/*"))
                        }
                    )
                } else {
                    PlayerScreen(
                        videoUri = selectedVideo!!,
                        onBack = { selectedVideo = null }
                    )
                }
            }
        }
    }

    private fun videoPermissionNames(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private fun hasVideoPermission(): Boolean {
        val permissions = videoPermissionNames()
        return permissions.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun loadLocalVideos(): List<LocalVideo> {
        if (!hasVideoPermission()) return emptyList()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        val videos = mutableListOf<LocalVideo>()
        contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                videos += LocalVideo(
                    uri = ContentUris.withAppendedId(collection, id),
                    name = cursor.getString(nameColumn) ?: "Untitled video",
                    durationMs = cursor.getLong(durationColumn),
                    sizeBytes = cursor.getLong(sizeColumn)
                )
            }
        }
        return videos
    }
}
