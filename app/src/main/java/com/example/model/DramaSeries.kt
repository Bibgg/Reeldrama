package com.example.model

data class DramaSeries(
    val id: String,
    val title: String,
    val synopsis: String,
    val genre: String,
    val tags: List<String>,
    val totalEpisodes: Int,
    val rating: Float,
    val viewsCountFormatted: String,
    val director: String = "Zhang Wei",
    val cast: List<String> = listOf("Alexander Vance", "Selena Moore", "Dominic Reed"),
    val posterColorHex: Long = 0xFF7A1426,
    val coverImageUrl: String = "",
    val isTrending: Boolean = true,
    val episodes: List<Episode>
)
