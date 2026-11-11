package com.example.data

import com.example.model.Comment
import com.example.model.DramaSeries
import com.example.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private fun formatShortMaxCount(value: Long): String = when {
    value >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun shortMaxMap(value: Any?): Map<*, *>? = value as? Map<*, *>

private val shortMaxRecordKeys = setOf(
    "id", "code", "dramaid", "drama_id", "bookid", "book_id",
    "seriesid", "series_id", "shortplayid", "shortplay_id", "vid",
    "episode", "episodenumber", "episode_number", "chapter", "chapterno",
    "chapter_no", "videourl", "video_url", "playurl", "play_url", "video"
)

private fun Map<*, *>.looksLikeShortMaxRecord(): Boolean =
    keys.any { key -> key?.toString()?.lowercase(java.util.Locale.ROOT)?.let { it in shortMaxRecordKeys } == true }

private fun shortMaxList(value: Any?, depth: Int = 0): List<Map<*, *>> {
    if (depth > 7 || value == null) return emptyList()

    if (value is List<*>) {
        val maps = value.mapNotNull { it as? Map<*, *> }
        val records = maps.filter { it.looksLikeShortMaxRecord() }
        if (records.isNotEmpty()) return records
        for (child in value) {
            val found = shortMaxList(child, depth + 1)
            if (found.isNotEmpty()) return found
        }
        return emptyList()
    }

    val map = shortMaxMap(value) ?: return emptyList()

    // ShortMax /alleps wraps the episode array in a drama object:
    // { code, cover, episodes: [...] }. Do not mistake that wrapper for
    // a single episode merely because it also contains a "code" field.
    val nestedEpisodes = map["episodes"] as? List<*>
    if (nestedEpisodes != null) {
        val found = shortMaxList(nestedEpisodes, depth + 1)
        if (found.isNotEmpty()) return found
    }

    if (map.looksLikeShortMaxRecord()) return listOf(map)

    // Provider responses are not always wrapped in the same object. Check common
    // catalogue keys first, then recursively inspect nested response objects.
    val preferredKeys = listOf(
        "data", "items", "list", "dramas", "results", "episodes", "records",
        "rankList", "rankingList", "popularList", "recommendList", "bookList",
        "videoList", "rows", "content", "result", "payload", "homeData"
    )
    for (key in preferredKeys) {
        val found = shortMaxList(map[key], depth + 1)
        if (found.isNotEmpty()) return found
    }
    for (child in map.values) {
        val found = shortMaxList(child, depth + 1)
        if (found.isNotEmpty()) return found
    }
    return emptyList()
}

private fun Map<*, *>.smText(vararg keys: String): String =
    keys.firstNotNullOfOrNull { this[it]?.toString()?.takeIf(String::isNotBlank) }.orEmpty()

private fun Map<*, *>.smLong(vararg keys: String): Long =
    keys.firstNotNullOfOrNull { key ->
        when (val value = this[key]) {
            is Number -> value.toLong()
            is String -> value.replace(",", "").trim().toLongOrNull()
            else -> null
        }
    } ?: 0L

private fun Map<*, *>.smTags(): List<String> {
    val raw = this["tags"] ?: this["genres"] ?: this["tag"]
    return when (raw) {
        is List<*> -> raw.mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
        is String -> raw.split(",", "|").map(String::trim).filter(String::isNotBlank)
        else -> emptyList()
    }
}

private fun Map<*, *>.smVideoUrl(): String {
    val video = this["video"]
    val videoMap = video as? Map<*, *>
    return (video as? String)?.takeIf { it.startsWith("https://") }
        ?: videoMap?.smText("video_1080", "video1080", "url_1080", "url", "playUrl", "video_720", "video720", "video_480", "video480")
        ?: smText("video_1080", "video1080", "url_1080", "videoUrl", "video_url", "url")
}

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

    private val _dramas = MutableStateFlow<List<DramaSeries>>(emptyList())
    val dramas: StateFlow<List<DramaSeries>> = _dramas.asStateFlow()

    private val _currentDramaId = MutableStateFlow<String>("")
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
            val remoteMongo = api().getDramas().data
                .map { it.toDomain() }
                .filter { it.episodes.isNotEmpty() }
            // Prefer admin-published MongoDB content; use ShortMax as catalogue fallback.
            val remote = if (remoteMongo.isNotEmpty()) remoteMongo else {
                shortMaxList(api().getShortMaxPopular(lang = "hi", page = 1).data).take(12).mapNotNull { item ->
                    val code = item.smText("code", "id", "dramaId", "drama_id", "bookId", "book_id", "seriesId", "series_id", "shortPlayId", "shortplay_id", "vid")
                        .takeIf { it.matches(Regex("^[0-9]{1,20}$")) } ?: return@mapNotNull null
                    val cover = item.smText("cover", "coverUrl", "cover_url", "poster", "posterUrl", "poster_url", "image", "imageUrl", "thumb", "thumbnail")
                    val rawEpisodes = api().getShortMaxEpisodes(code).data
                    val eps = shortMaxList(rawEpisodes).mapNotNull { ep ->
                        val videoUrl = ep.smVideoUrl().takeIf { it.startsWith("https://") }
                            ?: return@mapNotNull null
                        val episodeNo = ep.smLong("episode", "episodeNumber", "episode_number", "ep", "chapter", "chapterNo", "chapter_no").toInt()
                        if (episodeNo <= 0) return@mapNotNull null
                        Episode(
                            id = "shortmax-" + code + "-" + episodeNo,
                            dramaId = "shortmax-" + code,
                            episodeNumber = episodeNo,
                            title = ep.smText("title", "name", "episodeTitle", "episode_title", "chapterName").ifBlank { "Episode " + episodeNo },
                            videoUrl = videoUrl,
                            durationSec = ep.smLong("duration", "durationSec", "duration_sec", "durationSeconds", "duration_seconds").toInt().coerceAtLeast(1),
                            thumbnailUrl = ep.smText("cover", "thumbnail", "thumbnailUrl", "thumbnail_url").ifBlank { cover }
                        )
                    }.sortedBy { it.episodeNumber }
                    if (eps.isEmpty()) return@mapNotNull null
                    val tags = item.smTags()
                    DramaSeries(
                        id = "shortmax-" + code,
                        title = item.smText("name", "title", "dramaName", "drama_name", "bookName", "book_name", "seriesName").ifBlank { "ShortMax " + code },
                        synopsis = item.smText("summary", "description", "synopsis", "intro", "desc"),
                        genre = tags.firstOrNull().orEmpty(),
                        tags = tags,
                        totalEpisodes = maxOf(item.smLong("episodes", "totalEpisodes", "episodeCount", "episode_count", "chapterCount", "chapter_count", "totalEpisode", "epCount").toInt(), eps.size),
                        rating = 0f,
                        viewsCountFormatted = formatShortMaxCount(item.smLong("views", "viewCount", "view_count", "playCount", "play_count")),
                        coverImageUrl = cover,
                        isTrending = true,
                        episodes = eps
                    )
                }
            }
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
