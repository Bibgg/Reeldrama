package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DramaRepository
import com.example.model.Comment
import com.example.model.DramaSeries
import com.example.model.Episode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ActiveSheet {
    NONE,
    EPISODES,
    SERIES_BROWSER,
    COMMENTS,
    DRAMA_INFO,
    BACKEND_CONFIG
}

data class PlayerUiState(
    val currentDrama: DramaSeries,
    val currentEpisode: Episode,
    val isPlaying: Boolean = true,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 85000L,
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
    val subtitlesEnabled: Boolean = true,
    val currentSubtitleText: String = "",
    val activeSheet: ActiveSheet = ActiveSheet.NONE,
    val isBookmarked: Boolean = true,
    val availableDramas: List<DramaSeries> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val backendUrl: String = ""
)

class ReelPlayerViewModel(
    private val repository: DramaRepository = DramaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            currentDrama = repository.getCurrentDrama(),
            currentEpisode = repository.getCurrentEpisode(),
            availableDramas = repository.dramas.value,
            isBookmarked = repository.bookmarkedSeriesIds.value.contains(repository.getCurrentDrama().id),
            comments = repository.getCommentsForEpisode(repository.getCurrentEpisode().id),
            backendUrl = repository.backendApiUrl.value
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _heartAnimationEvent = MutableSharedFlow<Pair<Float, Float>>()
    val heartAnimationEvent: SharedFlow<Pair<Float, Float>> = _heartAnimationEvent.asSharedFlow()

    private val _userFeedbackMessage = MutableSharedFlow<String>()
    val userFeedbackMessage: SharedFlow<String> = _userFeedbackMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.dramas.collect { dramas ->
                val curr = repository.getCurrentDrama()
                val ep = repository.getCurrentEpisode()
                _uiState.value = _uiState.value.copy(
                    availableDramas = dramas,
                    currentDrama = curr,
                    currentEpisode = ep,
                    isBookmarked = repository.bookmarkedSeriesIds.value.contains(curr.id)
                )
            }
        }
        viewModelScope.launch {
            repository.bookmarkedSeriesIds.collect { bookmarkedIds ->
                val isBookmarked = bookmarkedIds.contains(_uiState.value.currentDrama.id)
                _uiState.value = _uiState.value.copy(isBookmarked = isBookmarked)
            }
        }
        // ऐप खुलते ही सर्वर से असली ड्रामा लाओ
        refreshFromServer()
    }

    /** सर्वर से ड्रामा दोबारा लाता है। सर्वर न मिले तो सैंपल डेटा चलता रहता है। */
    fun refreshFromServer() {
        viewModelScope.launch {
            repository.refreshFromServer()
                .onSuccess { count ->
                    val drama = repository.getCurrentDrama()
                    val episode = repository.getCurrentEpisode()
                    _uiState.value = _uiState.value.copy(
                        availableDramas = repository.dramas.value,
                        currentDrama = drama,
                        currentEpisode = episode,
                        currentPositionMs = 0L,
                        isPlaying = true,
                        isBookmarked = repository.bookmarkedSeriesIds.value.contains(drama.id),
                        comments = repository.getCommentsForEpisode(episode.id)
                    )
                    emitFeedback("Connected: $count series loaded from server")
                }
                .onFailure { error ->
                    emitFeedback("Offline mode: ${error.message ?: "server unreachable"}. Showing sample dramas")
                }
        }
    }

    fun selectDrama(dramaId: String, startEpisodeIndex: Int = 0) {
        repository.setCurrentDrama(dramaId, startEpisodeIndex)
        val drama = repository.getCurrentDrama()
        val episode = repository.getCurrentEpisode()
        _uiState.value = _uiState.value.copy(
            currentDrama = drama,
            currentEpisode = episode,
            currentPositionMs = 0L,
            isPlaying = true,
            isBookmarked = repository.bookmarkedSeriesIds.value.contains(drama.id),
            comments = repository.getCommentsForEpisode(episode.id),
            activeSheet = ActiveSheet.NONE
        )
        emitFeedback("Switched to ${drama.title}")
        viewModelScope.launch { repository.registerView(drama.id) }
    }

    fun selectEpisode(index: Int) {
        val drama = repository.getCurrentDrama()
        if (index in 0 until drama.episodes.size) {
            repository.setCurrentEpisodeIndex(index)
            val episode = drama.episodes[index]
            _uiState.value = _uiState.value.copy(
                currentEpisode = episode,
                currentPositionMs = 0L,
                isPlaying = true,
                comments = repository.getCommentsForEpisode(episode.id),
                activeSheet = ActiveSheet.NONE
            )
            emitFeedback("Playing Episode ${episode.episodeNumber}")
        }
    }

    fun nextEpisode() {
        val currentDrama = repository.getCurrentDrama()
        val nextIndex = repository.currentEpisodeIndex.value + 1
        if (nextIndex < currentDrama.episodes.size) {
            selectEpisode(nextIndex)
        } else {
            // Find next drama
            val allDramas = repository.dramas.value
            val currentDramaIndex = allDramas.indexOfFirst { it.id == currentDrama.id }
            val nextDramaIndex = (currentDramaIndex + 1) % allDramas.size
            selectDrama(allDramas[nextDramaIndex].id, 0)
            emitFeedback("Starting next series: ${allDramas[nextDramaIndex].title}")
        }
    }

    fun prevEpisode() {
        val prevIndex = repository.currentEpisodeIndex.value - 1
        if (prevIndex >= 0) {
            selectEpisode(prevIndex)
        } else {
            emitFeedback("Already at Episode 1")
        }
    }

    fun togglePlayPause() {
        val nextState = !_uiState.value.isPlaying
        _uiState.value = _uiState.value.copy(isPlaying = nextState)
    }

    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun setBuffering(buffering: Boolean) {
        _uiState.value = _uiState.value.copy(isBuffering = buffering)
    }

    fun updatePlaybackProgress(positionMs: Long, durationMs: Long) {
        val duration = if (durationMs > 0) durationMs else _uiState.value.durationMs
        val currentSub = computeSubtitle(positionMs / 1000f)
        _uiState.value = _uiState.value.copy(
            currentPositionMs = positionMs,
            durationMs = duration,
            currentSubtitleText = currentSub
        )
    }

    private fun computeSubtitle(currentSec: Float): String {
        if (!_uiState.value.subtitlesEnabled) return ""
        val subs = _uiState.value.currentEpisode.subtitles
        val line = subs.firstOrNull { currentSec in it.startSec..it.endSec }
        return line?.text ?: ""
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        _uiState.value = _uiState.value.copy(isMuted = newMute)
        emitFeedback(if (newMute) "Muted" else "Sound On")
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
        val current = _uiState.value.playbackSpeed
        val next = when (current) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _uiState.value = _uiState.value.copy(playbackSpeed = next)
        emitFeedback("Speed ${next}x")
    }

    fun toggleSubtitles() {
        val enabled = !_uiState.value.subtitlesEnabled
        _uiState.value = _uiState.value.copy(subtitlesEnabled = enabled)
        emitFeedback(if (enabled) "Subtitles ON" else "Subtitles OFF")
    }

    fun onDoubleTapLike(tapX: Float, tapY: Float) {
        viewModelScope.launch {
            _heartAnimationEvent.emit(Pair(tapX, tapY))
        }
        val dramaId = _uiState.value.currentDrama.id
        val episodeId = _uiState.value.currentEpisode.id
        if (!_uiState.value.currentEpisode.isLiked) {
            repository.toggleLikeEpisode(dramaId, episodeId)
            _uiState.value = _uiState.value.copy(
                currentEpisode = repository.getCurrentEpisode()
            )
            viewModelScope.launch { repository.syncLike(episodeId, true) }
        }
    }

    fun toggleLikeCurrentEpisode() {
        val dramaId = _uiState.value.currentDrama.id
        val episodeId = _uiState.value.currentEpisode.id
        val isNowLiked = repository.toggleLikeEpisode(dramaId, episodeId)
        _uiState.value = _uiState.value.copy(
            currentEpisode = repository.getCurrentEpisode()
        )
        emitFeedback(if (isNowLiked) "Added to Liked Reels" else "Removed Like")
        viewModelScope.launch { repository.syncLike(episodeId, isNowLiked) }
    }

    fun toggleBookmarkCurrentSeries() {
        val dramaId = _uiState.value.currentDrama.id
        val isBookmarked = repository.toggleBookmarkSeries(dramaId)
        _uiState.value = _uiState.value.copy(isBookmarked = isBookmarked)
        emitFeedback(if (isBookmarked) "Added to My Watchlist" else "Removed from Watchlist")
    }

    fun setActiveSheet(sheet: ActiveSheet) {
        _uiState.value = _uiState.value.copy(activeSheet = sheet)
    }

    fun updateBackendUrl(newUrl: String) {
        repository.setBackendApiUrl(newUrl)
        _uiState.value = _uiState.value.copy(backendUrl = newUrl.trim())
        refreshFromServer()
    }

    private fun emitFeedback(message: String) {
        viewModelScope.launch {
            _userFeedbackMessage.emit(message)
        }
    }
}
