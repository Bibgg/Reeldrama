package com.example.data

import com.example.model.DramaSeries
import com.example.model.Episode
import com.example.model.SubtitleLine
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.Locale
import java.util.concurrent.TimeUnit

// ---------- सर्वर से आने वाला JSON (Node/Express + MongoDB) ----------

@JsonClass(generateAdapter = true)
data class SubtitleDto(
    val startSec: Float = 0f,
    val endSec: Float = 0f,
    val text: String = ""
)

@JsonClass(generateAdapter = true)
data class EpisodeDto(
    @Json(name = "_id") val id: String,
    val episodeNumber: Int = 0,
    val title: String = "",
    val videoUrl: String = "",
    val durationSec: Int = 90,
    val introHook: String = "",
    val thumbnailUrl: String = "",
    val likesCount: Long = 0L,
    val sharesCount: Long = 0L,
    val subtitles: List<SubtitleDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DramaDto(
    @Json(name = "_id") val id: String,
    val title: String = "",
    val synopsis: String = "",
    val genre: String = "",
    val tags: List<String> = emptyList(),
    val totalEpisodes: Int = 0,
    val rating: Double = 0.0,
    val viewsCount: Long = 0L,
    val posterColorHex: String = "#7A1426",
    val coverImageUrl: String = "",
    val isTrending: Boolean = false,
    val episodes: List<EpisodeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DramaListResponse(
    val success: Boolean = false,
    val data: List<DramaDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LikeRequest(val action: String) // "like" या "unlike"

@JsonClass(generateAdapter = true)
data class LikeResponse(
    val success: Boolean = false,
    val likesCount: Long = 0L
)

@JsonClass(generateAdapter = true)
data class ViewResponse(
    val success: Boolean = false,
    val viewsCount: Long = 0L
)

// ---------- Retrofit ----------

interface ReelApi {
    @GET("dramas")
    suspend fun getDramas(): DramaListResponse

    @POST("episodes/{id}/like")
    suspend fun likeEpisode(@Path("id") id: String, @Body body: LikeRequest): LikeResponse

    @POST("dramas/{id}/view")
    suspend fun addView(@Path("id") id: String): ViewResponse
}

object ReelApiFactory {
    /** baseUrl जैसे: https://video-server-w5se.onrender.com/api  (अंत में / हो या न हो, चलेगा) */
    fun create(baseUrl: String): ReelApi {
        val normalized = baseUrl.trim().trimEnd('/') + "/"
        // Render का free सर्वर सोया हो तो जागने में ~1 मिनट लगता है, इसलिए लंबा timeout
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(ReelApi::class.java)
    }
}

// ---------- सर्वर के डेटा को ऐप के मॉडल में बदलना ----------

private fun parseColor(hex: String): Long =
    try {
        hex.trim().removePrefix("#").toLong(16) or 0xFF000000L
    } catch (e: Exception) {
        0xFF7A1426L
    }

private fun formatCount(n: Long): String = when {
    n >= 1_000_000L -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000L -> String.format(Locale.US, "%.1fK", n / 1_000.0)
    else -> n.toString()
}

fun EpisodeDto.toDomain(dramaId: String): Episode = Episode(
    id = id,
    dramaId = dramaId,
    episodeNumber = episodeNumber,
    title = title,
    videoUrl = videoUrl,
    durationSec = durationSec,
    introHook = introHook,
    thumbnailUrl = thumbnailUrl,
    likesCount = likesCount,
    sharesCount = sharesCount,
    subtitles = subtitles.map { SubtitleLine(it.startSec, it.endSec, it.text) }
)

fun DramaDto.toDomain(): DramaSeries {
    val eps = episodes
        .filter { it.videoUrl.isNotBlank() }
        .sortedBy { it.episodeNumber }
        .map { it.toDomain(id) }
    return DramaSeries(
        id = id,
        title = title,
        synopsis = synopsis,
        genre = genre,
        tags = tags,
        totalEpisodes = maxOf(totalEpisodes, eps.size),
        rating = rating.toFloat(),
        viewsCountFormatted = formatCount(viewsCount),
        posterColorHex = parseColor(posterColorHex),
        coverImageUrl = coverImageUrl,
        isTrending = isTrending,
        episodes = eps
    )
}
