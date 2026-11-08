package com.artsiom.footballpulse.data

import com.artsiom.footballpulse.data.remote.FullTimeDto
import com.artsiom.footballpulse.data.remote.MatchDto
import com.artsiom.footballpulse.data.remote.ScoreDto
import com.artsiom.footballpulse.data.remote.TeamDto
import org.junit.Assert.assertEquals
import org.junit.Test

class FormCalculationTest {
    @Test
    fun `keeps the last five results in chronological order`() {
        val matches = listOf(
            match(1, "2025-01-01T12:00:00Z", 2, 0),
            match(2, "2025-01-02T12:00:00Z", 0, 1),
            match(3, "2025-01-03T12:00:00Z", 1, 1),
            match(4, "2025-01-04T12:00:00Z", 3, 0),
            match(5, "2025-01-05T12:00:00Z", 1, 2),
            match(6, "2025-01-06T12:00:00Z", 2, 0)
        ).reversed()

        assertEquals(mapOf(1 to "LDWLW", 2 to "WDLWL"), computeForm(matches))
    }

    @Test
    fun `includes a postponed early matchday among the most recent results`() {
        val matches = (21..25).map { day ->
            match(day, "2025-01-${day}T12:00:00Z", 2, 0)
        } + match(10, "2025-02-01T12:00:00Z", 0, 1)

        assertEquals(mapOf(1 to "WWWWL", 2 to "LLLLW"), computeForm(matches))
    }

    @Test
    fun `keeps all results when a team has fewer than five matches`() {
        assertEquals(
            mapOf(1 to "W", 2 to "L"),
            computeForm(listOf(match(1, "2025-01-01T12:00:00Z", 1, 0)))
        )
    }

    @Test
    fun `records draws and opposing wins and losses`() {
        val matches = listOf(
            match(1, "2025-01-01T12:00:00Z", 0, 0),
            match(2, "2025-01-02T12:00:00Z", 0, 1),
            match(3, "2025-01-03T12:00:00Z", 2, 0)
        )

        assertEquals(mapOf(1 to "DLW", 2 to "DWL"), computeForm(matches))
    }

    @Test
    fun `breaks equal timestamp ties by matchday`() {
        val matches = listOf(
            match(30, "2025-01-01T12:00:00Z", 1, 0),
            match(10, "2025-01-01T12:00:00Z", 0, 1)
        )

        assertEquals(mapOf(1 to "LW", 2 to "WL"), computeForm(matches))
    }

    @Test
    fun `compares actual instants across timestamp offsets`() {
        val matches = listOf(
            match(1, "2025-01-01T12:00:00Z", 1, 0),
            match(2, "2025-01-01T13:00:00+02:00", 0, 1)
        )

        assertEquals(mapOf(1 to "LW", 2 to "WL"), computeForm(matches))
    }

    @Test
    fun `ignores unfinished matches and missing full time scores`() {
        val matches = listOf(
            match(1, "2025-01-01T12:00:00Z", 1, 0),
            match(2, "2025-01-02T12:00:00Z", 0, 2).copy(status = "TIMED"),
            match(3, "2025-01-03T12:00:00Z", null, 2),
            match(4, "2025-01-04T12:00:00Z", 1, null)
        )

        assertEquals(mapOf(1 to "W", 2 to "L"), computeForm(matches))
        assertEquals(emptyMap<Int, String>(), computeForm(emptyList()))
    }

    private fun match(matchday: Int, date: String, home: Int?, away: Int?) = MatchDto(
        id = matchday,
        matchday = matchday,
        utcDate = date,
        status = "FINISHED",
        homeTeam = TeamDto(1, "Home", null),
        awayTeam = TeamDto(2, "Away", null),
        score = ScoreDto(FullTimeDto(home, away), null)
    )
}
