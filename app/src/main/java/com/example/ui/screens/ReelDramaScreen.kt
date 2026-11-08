package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.RubyRed
import com.example.ui.theme.TextSecondary
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.ActiveSheet
import com.example.ui.components.BackendConfigDialog
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.EpisodesBottomSheet
import com.example.ui.components.ReelOverlayControls
import com.example.ui.components.SeriesBrowserSheet
import com.example.ui.components.VideoSurfacePlayer
import com.example.ui.theme.OnyxBlack
import com.example.viewmodel.ReelPlayerViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * ReelDrama's native Tube/Reels-style main screen.
 *
 * The supplied TubeScreen sample used FeedPost/PostFormat and OmniInlineVideoPlayer,
 * which are not part of this repository. This version adapts its vertical-reel
 * experience to the app's existing Drama/Episode models and VideoSurfacePlayer,
 * preserving backend loading, episode navigation, likes, comments and bookmarks.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelDramaScreen(
    viewModel: ReelPlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Home, 1 = Reels

    val episodes = uiState.currentDrama.episodes
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { episodes.size.coerceAtLeast(1) }
    )

    LaunchedEffect(viewModel) {
        viewModel.userFeedbackMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(uiState.currentEpisode.id, episodes.size) {
        val targetIndex = episodes.indexOfFirst { it.id == uiState.currentEpisode.id }
        if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { pageIndex ->
            if (pageIndex >= 0 && pageIndex < episodes.size) {
                viewModel.selectEpisode(pageIndex)
            }
        }
    }

    BackHandler(enabled = uiState.activeSheet != ActiveSheet.NONE || selectedTab != 0) {
        if (uiState.activeSheet != ActiveSheet.NONE) {
            viewModel.setActiveSheet(ActiveSheet.NONE)
        } else {
            selectedTab = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnyxBlack)
            .testTag("reel_drama_main_screen")
    ) {
        if (selectedTab == 0) {
            ReelDramaHomeScreen(
                dramas = uiState.availableDramas,
                onPlayDrama = { dramaId ->
                    viewModel.selectDrama(dramaId, 0)
                    selectedTab = 1
                },
                onBrowseAll = { viewModel.setActiveSheet(ActiveSheet.SERIES_BROWSER) }
            )
        } else {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> episodes.getOrNull(index)?.id ?: index.toString() }
        ) { page ->
            val pageEpisode = episodes.getOrNull(page) ?: uiState.currentEpisode
            val isCurrentPage = page == pagerState.currentPage

            Box(modifier = Modifier.fillMaxSize()) {
                VideoSurfacePlayer(
                    videoUrl = pageEpisode.videoUrl,
                    isPlaying = isCurrentPage && uiState.isPlaying,
                    isMuted = uiState.isMuted,
                    playbackSpeed = uiState.playbackSpeed,
                    onProgressUpdate = { pos, dur ->
                        if (isCurrentPage) viewModel.updatePlaybackProgress(pos, dur)
                    },
                    onTogglePlayPause = {
                        if (isCurrentPage) viewModel.togglePlayPause()
                    },
                    onDoubleTapHeart = { x, y ->
                        if (isCurrentPage) viewModel.onDoubleTapLike(x, y)
                    },
                    onVideoCompleted = {
                        if (isCurrentPage) viewModel.nextEpisode()
                    }
                )

                if (isCurrentPage) {
                    ReelOverlayControls(
                        drama = uiState.currentDrama,
                        episode = pageEpisode,
                        currentPositionMs = uiState.currentPositionMs,
                        durationMs = uiState.durationMs,
                        isMuted = uiState.isMuted,
                        playbackSpeed = uiState.playbackSpeed,
                        subtitlesEnabled = uiState.subtitlesEnabled,
                        currentSubtitleText = uiState.currentSubtitleText,
                        isBookmarked = uiState.isBookmarked,
                        onToggleMute = { viewModel.toggleMute() },
                        onCycleSpeed = { viewModel.cyclePlaybackSpeed() },
                        onToggleSubtitles = { viewModel.toggleSubtitles() },
                        onToggleLike = { viewModel.toggleLikeCurrentEpisode() },
                        onToggleBookmark = { viewModel.toggleBookmarkCurrentSeries() },
                        onOpenEpisodes = { viewModel.setActiveSheet(ActiveSheet.EPISODES) },
                        onOpenSeriesBrowser = { viewModel.setActiveSheet(ActiveSheet.SERIES_BROWSER) },
                        onOpenComments = { viewModel.setActiveSheet(ActiveSheet.COMMENTS) },
                        onOpenBackendConfig = { viewModel.setActiveSheet(ActiveSheet.BACKEND_CONFIG) },
                        onNextEpisode = { viewModel.nextEpisode() },
                        onPrevEpisode = { viewModel.prevEpisode() },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Watch '${uiState.currentDrama.title}' Episode ${pageEpisode.episodeNumber} on ReelDrama! Stream now: https://reeldrama.app/watch/${uiState.currentDrama.id}/${pageEpisode.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Episode"))
                        }
                    )
                }
            }
        }

        // Tube-style title badge from the supplied design, adapted for ReelDrama.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 42.dp)
                .testTag("reeldrama_reels_header"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ReelDrama",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REELS",
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .background(Color(0xFFB45CFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }

        }

        NavigationBar(
            containerColor = DarkSurface,
            contentColor = Color.White
        ) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Reels") },
                label = { Text("Reels") }
            )
            NavigationBarItem(
                selected = uiState.activeSheet == ActiveSheet.SERIES_BROWSER,
                onClick = { viewModel.setActiveSheet(ActiveSheet.SERIES_BROWSER) },
                icon = { Icon(Icons.Default.Bookmark, contentDescription = "Series") },
                label = { Text("Series") }
            )
        }

        when (uiState.activeSheet) {
            ActiveSheet.EPISODES -> {
                EpisodesBottomSheet(
                    drama = uiState.currentDrama,
                    currentEpisodeIndex = pagerState.currentPage,
                    onEpisodeSelected = { index -> viewModel.selectEpisode(index) },
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.NONE) }
                )
            }
            ActiveSheet.SERIES_BROWSER -> {
                SeriesBrowserSheet(
                    dramas = uiState.availableDramas,
                    currentDramaId = uiState.currentDrama.id,
                    onSelectDrama = { dramaId -> viewModel.selectDrama(dramaId, 0) },
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.NONE) }
                )
            }
            ActiveSheet.COMMENTS -> {
                CommentsBottomSheet(
                    comments = uiState.comments,
                    episodeTitle = "Ep.${uiState.currentEpisode.episodeNumber}: ${uiState.currentEpisode.title}",
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.NONE) }
                )
            }
            ActiveSheet.BACKEND_CONFIG -> {
                BackendConfigDialog(
                    currentUrl = uiState.backendUrl,
                    onSaveUrl = { newUrl -> viewModel.updateBackendUrl(newUrl) },
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.NONE) }
                )
            }
            ActiveSheet.NONE, ActiveSheet.DRAMA_INFO -> Unit
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .navigationBarsPadding()
        )
    }
}
