package com.bibgg.kplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

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

                if (selectedVideo == null) {
                    HomeScreen(onOpenVideo = {
                        videoPicker.launch(arrayOf("video/*"))
                    })
                } else {
                    PlayerScreen(
                        videoUri = selectedVideo!!,
                        onBack = { selectedVideo = null }
                    )
                }
            }
        }
    }
}
