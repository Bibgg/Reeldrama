package com.example.data

import com.example.model.Comment
import com.example.model.DramaSeries
import com.example.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DramaRepository {

    companion object {
        // Render पर सर्वर deploy होने के बाद यहाँ अपना URL डालें। अंत में /api ज़रूर रखें।
        // जैसे: "https://reeldrama-api.onrender.com/api"
        const val DEFAULT_BACKEND_URL = "https://reeldrama-api.onrender.com/api"
    }

    // true तब होता है जब ड्रामा असली सर्वर से आए हों (तभी likes/views सर्वर को भेजे जाते हैं)
    @Volatile
    private var loadedFromServer = false

    private var cachedApiUrl: String = ""
    private var cachedApi: ReelApi? = null

    private fun api(): ReelApi {
        val url = _backendApiUrl.value
        val existing = cachedApi
        if (existing != null && cachedApiUrl == url) return existing
        return ReelApiFactory.create(url).also {
            cachedApi = it
            cachedApiUrl = url
        }
    }

    // Default fast public streaming CDN & Cloudinary video URLs
    private val videoStreamUrls = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://res.cloudinary.com/rgsshthg/video/upload/v1794084108/Messenger_creation_2575CBA0-D92C-44F1-9E55-0D4795C49A72.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://res.cloudinary.com/demo/video/upload/sea-turtle.mp4",
        "https://res.cloudinary.com/demo/video/upload/dog.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    )

    private val _dramas = MutableStateFlow<List<DramaSeries>>(emptyList())
    val dramas: StateFlow<List<DramaSeries>> = _dramas.asStateFlow()

    private val _currentDramaId = MutableStateFlow<String>("drama_billionaire_bride")
    val currentDramaId: StateFlow<String> = _currentDramaId.asStateFlow()

    private val _currentEpisodeIndex = MutableStateFlow<Int>(0)
    val currentEpisodeIndex: StateFlow<Int> = _currentEpisodeIndex.asStateFlow()

    private val _bookmarkedSeriesIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedSeriesIds: StateFlow<Set<String>> = _bookmarkedSeriesIds.asStateFlow()

    private val _backendApiUrl = MutableStateFlow<String>(DEFAULT_BACKEND_URL)
    val backendApiUrl: StateFlow<String> = _backendApiUrl.asStateFlow()

    fun setCurrentDrama(dramaId: String, startEpisodeIndex: Int = 0) {
        _currentDramaId.value = dramaId
        _currentEpisodeIndex.value = startEpisodeIndex
    }

    fun setCurrentEpisodeIndex(index: Int) {
        val currentDrama = getCurrentDrama()
        if (index in 0 until currentDrama.episodes.size) {
            _currentEpisodeIndex.value = index
            markEpisodeWatched(currentDrama.id, index)
        }
    }

    fun getCurrentDrama(): DramaSeries {
        return _dramas.value.firstOrNull { it.id == _currentDramaId.value }
            ?: _dramas.value.firstOrNull()
            ?: emptyDrama()
    }

    fun getCurrentEpisode(): Episode {
        val drama = getCurrentDrama()
        val index = _currentEpisodeIndex.value.coerceIn(0, (drama.episodes.size - 1).coerceAtLeast(0))
        return drama.episodes.getOrNull(index) ?: emptyEpisode()
    }

    fun toggleLikeEpisode(dramaId: String, episodeId: String): Boolean {
        var newLikedState = false
        _dramas.value = _dramas.value.map { drama ->
            if (drama.id == dramaId) {
                drama.copy(
                    episodes = drama.episodes.map { ep ->
                        if (ep.id == episodeId) {
                            newLikedState = !ep.isLiked
                            ep.copy(
                                isLiked = newLikedState,
                                likesCount = if (newLikedState) ep.likesCount + 1 else ep.likesCount - 1
                            )
                        } else ep
                    }
                )
            } else drama
        }
        return newLikedState
    }

    fun toggleBookmarkSeries(dramaId: String): Boolean {
        val currentSet = _bookmarkedSeriesIds.value.toMutableSet()
        val isNowBookmarked = if (currentSet.contains(dramaId)) {
            currentSet.remove(dramaId)
            false
        } else {
            currentSet.add(dramaId)
            true
        }
        _bookmarkedSeriesIds.value = currentSet
        return isNowBookmarked
    }

    fun markEpisodeWatched(dramaId: String, episodeIndex: Int) {
        _dramas.value = _dramas.value.map { drama ->
            if (drama.id == dramaId) {
                drama.copy(
                    episodes = drama.episodes.mapIndexed { idx, ep ->
                        if (idx == episodeIndex) ep.copy(isWatched = true) else ep
                    }
                )
            } else drama
        }
    }

    fun setBackendApiUrl(url: String) {
        _backendApiUrl.value = url.trim()
    }

    /**
     * सर्वर से ड्रामा + एपिसोड लाकर लिस्ट बदल देता है।
     * सर्वर न मिले, या वहाँ डेटा खाली हो, तो पुराना (सैंपल) डेटा वैसा ही रहता है और Failure लौटता है।
     * सफल होने पर कितने ड्रामा आए, वह गिनती लौटाता है।
     */
    suspend fun refreshFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val base = _backendApiUrl.value
            require(base.isNotBlank() && !base.contains("example.com")) {
                "Backend URL not set"
            }
            val remote = api().getDramas().data
                .map { it.toDomain() }
                .filter { it.episodes.isNotEmpty() }
            loadedFromServer = true
            _dramas.value = remote
            _currentDramaId.value = remote.firstOrNull()?.id.orEmpty()
            _currentEpisodeIndex.value = 0
            remote.size
        }
    }

    /** Like/Unlike सर्वर पर भेजता है। फेल हो जाए तो चुपचाप छोड़ देता है (ऐप में like फिर भी दिखेगा)। */
    suspend fun syncLike(episodeId: String, liked: Boolean) {
        if (!loadedFromServer) return
        withContext(Dispatchers.IO) {
            runCatching {
                api().likeEpisode(episodeId, LikeRequest(if (liked) "like" else "unlike"))
            }
        }
    }

    /** ड्रामा खोलने पर सर्वर में view count +1। */
    suspend fun registerView(dramaId: String) {
        if (!loadedFromServer) return
        withContext(Dispatchers.IO) {
            runCatching { api().addView(dramaId) }
        }
    }

    fun getCommentsForEpisode(episodeId: String): List<Comment> = emptyList()

    private fun emptyDrama(): DramaSeries = DramaSeries(
        id = "no_content",
        title = "अभी कोई ड्रामा उपलब्ध नहीं है",
        synopsis = "नए ड्रामा जल्द जोड़े जाएँगे।",
        genre = "",
        tags = emptyList(),
        totalEpisodes = 0,
        rating = 0f,
        viewsCountFormatted = "0",
        director = "",
        cast = emptyList(),
        isTrending = false,
        episodes = emptyList()
    )

    private fun emptyEpisode(): Episode = Episode(
        id = "no_episode",
        dramaId = "no_content",
        episodeNumber = 0,
        title = "कोई एपिसोड उपलब्ध नहीं",
        videoUrl = "",
        durationSec = 0,
        introHook = "",
        likesCount = 0L,
        commentsCount = 0L,
        sharesCount = 0L
    )
}
