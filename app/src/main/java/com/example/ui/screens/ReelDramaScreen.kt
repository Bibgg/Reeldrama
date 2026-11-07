package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelDramaScreen(
    viewModel: ReelPlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Sync Pager state with current episode index
    val episodes = uiState.currentDrama.episodes
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { episodes.size }
    )

    // Listen to ViewModel user messages
    LaunchedEffect(viewModel) {
        viewModel.userFeedbackMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // When episode changes from outside (e.g. from Episode drawer), scroll pager
    LaunchedEffect(uiState.currentEpisode.id) {
        val targetIndex = episodes.indexOfFirst { it.id == uiState.currentEpisode.id }
        if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    // When user swipes pager up/down, update ViewModel's selected episode
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { pageIndex ->
            if (pageIndex in episodes.indices) {
                viewModel.selectEpisode(pageIndex)
            }
        }
    }

    // Android back handler
    BackHandler(enabled = uiState.activeSheet != ActiveSheet.NONE) {
        viewModel.setActiveSheet(ActiveSheet.NONE)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnyxBlack)
            .testTag("reel_drama_main_screen")
    ) {
        // Vertical Pager for full-screen Reels
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> episodes.getOrNull(index)?.id ?: index.toString() }
        ) { page ->
            val pageEpisode = episodes.getOrNull(page) ?: uiState.currentEpisode
            val isCurrentPage = page == pagerState.currentPage

            Box(modifier = Modifier.fillMaxSize()) {
                // Video Player
                VideoSurfacePlayer(
                    videoUrl = pageEpisode.videoUrl,
                    isPlaying = isCurrentPage && uiState.isPlaying,
                    isMuted = uiState.isMuted,
                    playbackSpeed = uiState.playbackSpeed,
                    onProgressUpdate = { pos, dur ->
                        if (isCurrentPage) {
                            viewModel.updatePlaybackProgress(pos, dur)
                        }
                    },
                    onTogglePlayPause = {
                        if (isCurrentPage) {
                            viewModel.togglePlayPause()
                        }
                    },
                    onDoubleTapHeart = { x, y ->
                        if (isCurrentPage) {
                            viewModel.onDoubleTapLike(x, y)
                        }
                    },
                    onVideoCompleted = {
                        if (isCurrentPage) {
                            viewModel.nextEpisode()
                        }
                    }
                )

                // Overlay Controls (only active on current visible page)
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

        // Active Bottom Sheets & Dialogs
        when (uiState.activeSheet) {
            ActiveSheet.EPISODES -> {
                EpisodesBottomSheet(
                    drama = uiState.currentDrama,
                    currentEpisodeIndex = pagerState.currentPage,
                    onEpisodeSelected = { index ->
                        viewModel.selectEpisode(index)
                    },
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.NONE) }
                )
            }
            ActiveSheet.SERIES_BROWSER -> {
                SeriesBrowserSheet(
                    dramas = uiState.availableDramas,
                    currentDramaId = uiState.currentDrama.id,
                    onSelectDrama = { dramaId ->
                        viewModel.selectDrama(dramaId, 0)
                    },
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
                    onSaveUrl = { newUrl ->
                        viewModel.updateBackendUrl(newUrl)
                    },
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.NONE) }
                )
            }
            ActiveSheet.NONE, ActiveSheet.DRAMA_INFO -> {}
        }

        // Snackbar Host for feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .navigationBarsPadding()
        )
    }
}
