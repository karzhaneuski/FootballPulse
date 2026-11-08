package com.artsiom.footballpulse.data

import com.artsiom.footballpulse.data.remote.FootballApiService
import com.artsiom.footballpulse.data.remote.FullTimeDto
import com.artsiom.footballpulse.data.remote.MatchDetailDto
import com.artsiom.footballpulse.data.remote.MatchDto
import com.artsiom.footballpulse.data.remote.MatchesResponse
import com.artsiom.footballpulse.data.remote.ScoreDto
import com.artsiom.footballpulse.data.remote.StandingDto
import com.artsiom.footballpulse.data.remote.StandingsResponse
import com.artsiom.footballpulse.data.remote.TableEntryDto
import com.artsiom.footballpulse.data.remote.TeamDetailsDto
import com.artsiom.footballpulse.data.remote.TeamDto
import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.domain.repository.FootballRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

class FootballRepositoryInjectionTest {
    @Test
    fun `injected API receives matchday selection and returns domain matches`() = runBlocking {
        val api = object : FakeApi() {
            override suspend fun getMatchesByMatchday(
                leagueCode: String, matchday: Int, season: Int
            ): Response<MatchesResponse> {
                assertEquals("BL1", leagueCode)
                assertEquals(17, matchday)
                assertEquals(2024, season)
                return Response.success(
                    MatchesResponse(listOf(
                        MatchDto(
                            id = 42,
                            matchday = 17,
                            utcDate = "2025-01-01T12:00:00Z",
                            status = "FINISHED",
                            homeTeam = TeamDto(1, "Home", "home.png"),
                            awayTeam = TeamDto(2, "Away", null),
                            score = ScoreDto(FullTimeDto(2, 1), null)
                        )
                    ))
                )
            }
        }
        val repository: FootballRepository = FootballRepositoryImpl(api)

        assertEquals(
            listOf(Match(
                id = 42,
                homeTeam = "Home",
                awayTeam = "Away",
                homeScore = 2,
                awayScore = 1,
                date = "2025-01-01T12:00:00Z",
                status = "FINISHED",
                homeTeamCrest = "home.png",
                awayTeamCrest = null
            )),
            repository.getMatchesByMatchday("BL1", 17, 2024)
        )
    }

    @Test
    fun `standings still load when form request fails`() = runBlocking {
        val repository: FootballRepository = FootballRepositoryImpl(object : FakeApi() {
            override suspend fun getMatches(leagueCode: String, season: Int): Response<MatchesResponse> {
                throw IllegalStateException("Offline")
            }
        })

        val standings = repository.getStandings("PL")

        assertEquals(1, standings.size)
        assertEquals("Home", standings.single().teamName)
        assertEquals(3, standings.single().points)
        assertNull(standings.single().form)
    }

    @Test
    fun `standings form request cancellation propagates`() = runBlocking {
        val cancellation = CancellationException("Superseded")
        val repository: FootballRepository = FootballRepositoryImpl(object : FakeApi() {
            override suspend fun getMatches(leagueCode: String, season: Int): Response<MatchesResponse> {
                throw cancellation
            }
        })

        try {
            repository.getStandings("PL")
            fail("Cancellation must propagate instead of returning standings")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }

    private open class FakeApi : FootballApiService {
        override suspend fun getMatches(leagueCode: String, season: Int): Response<MatchesResponse> =
            error("Unexpected matches request")

        override suspend fun getMatchesByMatchday(
            leagueCode: String, matchday: Int, season: Int
        ): Response<MatchesResponse> = error("Unexpected matchday request")

        override suspend fun getStandings(leagueCode: String): Response<StandingsResponse> =
            Response.success(StandingsResponse(listOf(StandingDto(listOf(
                TableEntryDto(1, TeamDto(1, "Home", null), 1, 1, 0, 0, 3, 2, 1, 1, null)
            )))))

        override suspend fun getMatchDetail(matchId: Int): Response<MatchDetailDto> =
            error("Unexpected match detail request")

        override suspend fun getTeam(id: Int): Response<TeamDetailsDto> =
            error("Unexpected team request")
    }
}
