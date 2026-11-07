package com.example

import com.example.data.DramaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun `repository contains initial drama series with episodes`() {
    val repo = DramaRepository()
    val dramas = repo.dramas.value
    assertTrue("Repository should have drama series", dramas.isNotEmpty())
    val currentDrama = repo.getCurrentDrama()
    assertNotNull("Current drama should not be null", currentDrama)
    assertTrue("Current drama should have episodes", currentDrama.episodes.isNotEmpty())
    assertEquals(1, currentDrama.episodes.first().episodeNumber)
  }

  @Test
  fun `toggle like episode increments count`() {
    val repo = DramaRepository()
    val currentDrama = repo.getCurrentDrama()
    val firstEpisode = currentDrama.episodes.first()
    val initialLikes = firstEpisode.likesCount

    val isLiked = repo.toggleLikeEpisode(currentDrama.id, firstEpisode.id)
    assertTrue("Episode should now be liked", isLiked)

    val updatedEpisode = repo.getCurrentDrama().episodes.first()
    assertEquals(initialLikes + 1, updatedEpisode.likesCount)
  }

  @Test
  fun `toggle bookmark series updates bookmark set`() {
    val repo = DramaRepository()
    val dramaId = "drama_undercover_tycoon"
    val initiallyBookmarked = repo.bookmarkedSeriesIds.value.contains(dramaId)

    val newState = repo.toggleBookmarkSeries(dramaId)
    assertEquals(!initiallyBookmarked, newState)
    assertEquals(newState, repo.bookmarkedSeriesIds.value.contains(dramaId))
  }
}
