package com.example.data

import com.example.model.Comment
import com.example.model.DramaSeries
import com.example.model.Episode
import com.example.model.SubtitleLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DramaRepository {

    // Default fast public streaming CDN & Cloudinary video URLs
    private val videoStreamUrls = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://res.cloudinary.com/demo/video/upload/sea-turtle.mp4",
        "https://res.cloudinary.com/demo/video/upload/dog.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    )

    private val _dramas = MutableStateFlow<List<DramaSeries>>(createInitialDramas())
    val dramas: StateFlow<List<DramaSeries>> = _dramas.asStateFlow()

    private val _currentDramaId = MutableStateFlow<String>("drama_billionaire_bride")
    val currentDramaId: StateFlow<String> = _currentDramaId.asStateFlow()

    private val _currentEpisodeIndex = MutableStateFlow<Int>(0)
    val currentEpisodeIndex: StateFlow<Int> = _currentEpisodeIndex.asStateFlow()

    private val _bookmarkedSeriesIds = MutableStateFlow<Set<String>>(setOf("drama_billionaire_bride"))
    val bookmarkedSeriesIds: StateFlow<Set<String>> = _bookmarkedSeriesIds.asStateFlow()

    private val _backendApiUrl = MutableStateFlow<String>("https://api.reeldrama.example.com/api")
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
            ?: _dramas.value.first()
    }

    fun getCurrentEpisode(): Episode {
        val drama = getCurrentDrama()
        val index = _currentEpisodeIndex.value.coerceIn(0, (drama.episodes.size - 1).coerceAtLeast(0))
        return drama.episodes[index]
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

    fun getCommentsForEpisode(episodeId: String): List<Comment> {
        return listOf(
            Comment("c1", "Elena_R", "ER", "Wait she actually signed the contract without reading clause 4?! 😱", "2m ago", 243),
            Comment("c2", "DramaAddict99", "DA", "Bro had no idea his wife owns the penthouse above his! The slap was so satisfying!! 🔥", "14m ago", 189),
            Comment("c3", "Marcus Cole", "MC", "Episode 3 ending left me breathless. Best short drama on here by far.", "1h ago", 95),
            Comment("c4", "Chloe_V", "CV", "He was looking for his childhood savior for 15 years and she was right in front of him! 😭", "2h ago", 412),
            Comment("c5", "Zenith", "ZN", "The soundtrack when he steps out of the black limo gives me chills every time.", "4h ago", 67)
        )
    }

    private fun createInitialDramas(): List<DramaSeries> {
        val brideEpisodes = listOf(
            Episode(
                id = "ep_bb_1",
                dramaId = "drama_billionaire_bride",
                episodeNumber = 1,
                title = "The Disowned Heiress Returns",
                videoUrl = videoStreamUrls[0],
                durationSec = 82,
                introHook = "Five years after being framed and cast out, Amelia steps into the grand gala unnoticed...",
                likesCount = 48200L,
                commentsCount = 3120L,
                sharesCount = 1200L,
                subtitles = listOf(
                    SubtitleLine(1f, 4f, "They thought I was gone for good."),
                    SubtitleLine(5f, 9f, "Tonight, every debt in this ballroom will be paid in full."),
                    SubtitleLine(10f, 15f, "Who invited that beggar to the President's table?!")
                )
            ),
            Episode(
                id = "ep_bb_2",
                dramaId = "drama_billionaire_bride",
                episodeNumber = 2,
                title = "Contract Marriage at Midnight",
                videoUrl = videoStreamUrls[1],
                durationSec = 95,
                introHook = "CEO Ethan Vance hands her a pen. 'Marry me for one year, and I'll crush your enemies.'",
                likesCount = 52100L,
                commentsCount = 4450L,
                sharesCount = 2100L,
                subtitles = listOf(
                    SubtitleLine(1f, 5f, "Sign here, Miss Amelia."),
                    SubtitleLine(6f, 10f, "One year. No emotional attachment. Unlimited power."),
                    SubtitleLine(11f, 15f, "Deal, Mr. Vance.")
                )
            ),
            Episode(
                id = "ep_bb_3",
                dramaId = "drama_billionaire_bride",
                episodeNumber = 3,
                title = "Slap in the VIP Banquet",
                videoUrl = videoStreamUrls[2],
                durationSec = 78,
                introHook = "The arrogant step-sister tries to throw wine at Amelia—until security bows at Amelia's feet.",
                likesCount = 67800L,
                commentsCount = 6200L,
                sharesCount = 3800L,
                subtitles = listOf(
                    SubtitleLine(2f, 6f, "You dare show your face in high society?!"),
                    SubtitleLine(7f, 11f, "Apologize to Madam Amelia immediately or lose your franchise!"),
                    SubtitleLine(12f, 16f, "Madam?! Since when is she married to...")
                )
            ),
            Episode(
                id = "ep_bb_4",
                dramaId = "drama_billionaire_bride",
                episodeNumber = 4,
                title = "The Hidden Stockholder",
                videoUrl = videoStreamUrls[3],
                durationSec = 88,
                introHook = "The board tries to oust Ethan—until the mysterious 30% majority owner walks into the boardroom.",
                likesCount = 71200L,
                commentsCount = 5900L,
                sharesCount = 4200L
            ),
            Episode(
                id = "ep_bb_5",
                dramaId = "drama_billionaire_bride",
                episodeNumber = 5,
                title = "Unmasked in the Rain",
                videoUrl = videoStreamUrls[4],
                durationSec = 91,
                introHook = "Ethan finally notices the silver butterfly pendant around her neck: 'It was you all along.'",
                likesCount = 89400L,
                commentsCount = 8740L,
                sharesCount = 6500L
            )
        )

        val empressEpisodes = listOf(
            Episode(
                id = "ep_emp_1",
                dramaId = "drama_empress_revenge",
                episodeNumber = 1,
                title = "Reborn in the Cold Palace",
                videoUrl = videoStreamUrls[5],
                durationSec = 74,
                introHook = "Opening her eyes, she realizes she has traveled back to the day before the royal conspiracy.",
                likesCount = 39800L,
                commentsCount = 2800L,
                sharesCount = 1450L
            ),
            Episode(
                id = "ep_emp_2",
                dramaId = "drama_empress_revenge",
                episodeNumber = 2,
                title = "The Traitor's Tea",
                videoUrl = videoStreamUrls[6],
                durationSec = 80,
                introHook = "Instead of drinking the poisoned cup, she calmly smiles and switches the jade saucers.",
                likesCount = 43200L,
                commentsCount = 3600L,
                sharesCount = 1980L
            ),
            Episode(
                id = "ep_emp_3",
                dramaId = "drama_empress_revenge",
                episodeNumber = 3,
                title = "The Emperor's Second Glance",
                videoUrl = videoStreamUrls[7],
                durationSec = 86,
                introHook = "Her sudden icy intelligence shocks the palace. For the first time, His Majesty is intrigued.",
                likesCount = 58900L,
                commentsCount = 4900L,
                sharesCount = 2800L
            )
        )

        val tycoonEpisodes = listOf(
            Episode(
                id = "ep_tyc_1",
                dramaId = "drama_undercover_tycoon",
                episodeNumber = 1,
                title = "The Humble Delivery Son-in-Law",
                videoUrl = videoStreamUrls[2],
                durationSec = 68,
                introHook = "They threw his scooter helmet on the ground. He quietly took out an encrypted black titanium card.",
                likesCount = 92000L,
                commentsCount = 7600L,
                sharesCount = 5900L
            ),
            Episode(
                id = "ep_tyc_2",
                dramaId = "drama_undercover_tycoon",
                episodeNumber = 2,
                title = "One Hundred Black Sedans",
                videoUrl = videoStreamUrls[3],
                durationSec = 84,
                introHook = "The city's most feared warlord arrives in person and kneels before the young delivery man.",
                likesCount = 104000L,
                commentsCount = 9800L,
                sharesCount = 8200L
            ),
            Episode(
                id = "ep_tyc_3",
                dramaId = "drama_undercover_tycoon",
                episodeNumber = 3,
                title = "Buying the Skyscraper",
                videoUrl = videoStreamUrls[4],
                durationSec = 77,
                introHook = "'You said I could never step foot in this tower? Fine. I just bought the deed.'",
                likesCount = 112000L,
                commentsCount = 11400L,
                sharesCount = 9700L
            )
        )

        val mafiaEpisodes = listOf(
            Episode(
                id = "ep_maf_1",
                dramaId = "drama_mafia_husband",
                episodeNumber = 1,
                title = "The Gentle Art Teacher",
                videoUrl = videoStreamUrls[0],
                durationSec = 75,
                introHook = "He paints soft watercolors at noon. At midnight, the underworld bows to his whispers.",
                likesCount = 61200L,
                commentsCount = 5100L,
                sharesCount = 3300L
            ),
            Episode(
                id = "ep_maf_2",
                dramaId = "drama_mafia_husband",
                episodeNumber = 2,
                title = "Who Touched My Wife?",
                videoUrl = videoStreamUrls[1],
                durationSec = 82,
                introHook = "A corrupt developer threatens her gallery. Ten armed guards surround the building in seconds.",
                likesCount = 79400L,
                commentsCount = 6800L,
                sharesCount = 4900L
            )
        )

        return listOf(
            DramaSeries(
                id = "drama_billionaire_bride",
                title = "Billionaire's Secret Bride",
                synopsis = "Amelia was framed and discarded by her corrupt family. Five years later, she returns as a clandestine financial titan and enters a strategic contract marriage with Ethan Vance, the city's most ruthless CEO. Neither expects the secrets they both hide.",
                genre = "CEO Romance",
                tags = listOf("CEO", "Contract Marriage", "Secret Identity", "Revenge", "Binge"),
                totalEpisodes = 45,
                rating = 4.9f,
                viewsCountFormatted = "28.4M",
                posterColorHex = 0xFF8A1325,
                isTrending = true,
                episodes = brideEpisodes
            ),
            DramaSeries(
                id = "drama_undercover_tycoon",
                title = "Hidden Dragon: The Undercover Tycoon",
                synopsis = "Posing as an ordinary son-in-law to fulfill a sacred promise, Leo endured three years of humiliation. When his wife is cornered by arrogant oligarchs, the Supreme Commander finally reveals his terrifying global empire.",
                genre = "Urban Action",
                tags = listOf("Underdog", "Secret Billionaire", "Revenge", "Action", "High Stakes"),
                totalEpisodes = 50,
                rating = 4.8f,
                viewsCountFormatted = "34.1M",
                posterColorHex = 0xFF142C54,
                isTrending = true,
                episodes = tycoonEpisodes
            ),
            DramaSeries(
                id = "drama_empress_revenge",
                title = "Revenge of the Disgraced Empress",
                synopsis = "Betrayed and poisoned in the cold palace, Empress Shen awakes five years earlier. Armed with knowledge of every betrayal, she plays the grand chessboard of the court with icy, flawless precision.",
                genre = "Historical Revenge",
                tags = listOf("Rebirth", "Palace Intrigue", "Scheming", "Drama"),
                totalEpisodes = 60,
                rating = 4.9f,
                viewsCountFormatted = "19.7M",
                posterColorHex = 0xFF591C3F,
                isTrending = true,
                episodes = empressEpisodes
            ),
            DramaSeries(
                id = "drama_mafia_husband",
                title = "My Mafia Husband's Double Life",
                synopsis = "Clara thinks she married an ordinary, soft-spoken painter. But when danger looms, her husband's gentle gaze turns obsidian dark, commanding an empire that rules the city from the shadows.",
                genre = "Dark Romance",
                tags = listOf("Double Life", "Mafia", "Protective Husband", "Suspense"),
                totalEpisodes = 38,
                rating = 4.7f,
                viewsCountFormatted = "16.5M",
                posterColorHex = 0xFF2A1C3B,
                isTrending = false,
                episodes = mafiaEpisodes
            )
        )
    }
}
