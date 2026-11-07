package com.example.model

data class Episode(
    val id: String,
    val dramaId: String,
    val episodeNumber: Int,
    val title: String,
    val videoUrl: String,
    val durationSec: Int = 90,
    val introHook: String = "",
    val thumbnailUrl: String = "",
    var likesCount: Long = 12400L,
    var commentsCount: Long = 852L,
    var sharesCount: Long = 340L,
    var isLiked: Boolean = false,
    var isBookmarked: Boolean = false,
    var isWatched: Boolean = false,
    val subtitles: List<SubtitleLine> = emptyList()
)

data class SubtitleLine(
    val startSec: Float,
    val endSec: Float,
    val text: String
)
