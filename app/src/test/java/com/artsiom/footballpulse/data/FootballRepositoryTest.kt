package com.artsiom.footballpulse.data

import com.artsiom.footballpulse.data.remote.FootballApiService
import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.domain.model.MatchDetail
import com.artsiom.footballpulse.domain.model.SquadPlayer
import com.artsiom.footballpulse.domain.model.Standing
import com.artsiom.footballpulse.domain.model.TeamDetails
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FootballRepositoryTest {
    private val server = MockWebServer()
    private lateinit var api: FootballApiService
    private lateinit var repository: FootballRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FootballApiService::class.java)
        repository = repositoryAt("2025-08-01T00:00:00Z")
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `current matchday uses first active fixture and maps only its matchday`() = runTest {
        matches(fixture(1, 10, "FINISHED"), fixture(2, 4, "IN_PLAY"), fixture(3, 4, "TIMED"))
        val result = repository.getCurrentMatchdayResult("PL")
        assertEquals(4, result.matchday)
        assertEquals(listOf(expectedMatch(2, "IN_PLAY"), expectedMatch(3, "TIMED")), result.matches)
        assertPath("/v4/competitions/PL/matches?season=2025")
    }

    @Test
    fun `paused timed and scheduled fixtures each determine current matchday`() = runTest {
        for (status in listOf("PAUSED", "TIMED", "SCHEDULED")) {
            matches(fixture(1, 10, "FINISHED"), fixture(2, 4, status))
            assertEquals(4, repository.getCurrentMatchdayResult("PL").matchday)
        }
    }

    @Test
    fun `finished season selects largest non-null matchday`() = runTest {
        matches(fixture(1, 7), fixture(2, null), fixture(3, 38), fixture(4, 2))
        val result = repository.getCurrentMatchdayResult("PD")
        assertEquals(38, result.matchday)
        assertEquals(listOf(expectedMatch(3)), result.matches)
    }

    @Test
    fun `empty competition defaults to matchday one`() = runTest {
        matches()
        val result = repository.getCurrentMatchdayResult("PL")
        assertEquals(1, result.matchday)
        assertTrue(result.matches.isEmpty())
    }

    @Test
    fun `July belongs to previous season using injected date`() = runTest {
        repository = repositoryAt("2025-07-31T23:59:59Z")
        matches()
        assertEquals(2024, repository.getCurrentMatchdayResult("PL").season)
        assertPath("/v4/competitions/PL/matches?season=2024")
    }

    @Test
    fun `August starts new season using injected date`() = runTest {
        matches()
        assertEquals(2025, repository.getCurrentMatchdayResult("PL").season)
        assertPath("/v4/competitions/PL/matches?season=2025")
    }

    @Test
    fun `season follows clock timezone at August boundary`() = runTest {
        repository = FootballRepositoryImpl(api, Clock.fixed(
            Instant.parse("2025-07-31T23:30:00Z"), ZoneId.of("Europe/Warsaw")
        ))
        matches()
        assertEquals(2025, repository.getCurrentMatchdayResult("PL").season)
    }

    @Test
    fun `selected matchday maps null scores and crests and sends exact filters`() = runTest {
        matches(fixture(42, 17, "TIMED", "null", "null"))
        val result = repository.getMatchesByMatchday("BL1", 17, 2024)
        assertEquals(listOf(expectedMatch(42, "TIMED").copy(homeScore = null, awayScore = null)), result)
        assertPath("/v4/competitions/BL1/matches?matchday=17&season=2024")
    }

    @Test
    fun `standings map every field and compute missing form from match results`() = runTest {
        respond(standingsJson())
        matches(fixture(42, 1))
        assertEquals(listOf(Standing(1, 7, "Home", "home.png", 3, 2, 1, 0, 7, 5, 1, 4, "W")),
            repository.getStandings("SA"))
        assertPath("/v4/competitions/SA/standings")
        assertPath("/v4/competitions/SA/matches?season=2025")
    }

    @Test
    fun `API form takes precedence over calculated form`() = runTest {
        respond(standingsJson("\"LDW\""))
        matches(fixture(42, 1))
        assertEquals("LDW", repository.getStandings("PL").single().form)
    }

    @Test
    fun `failed form HTTP request does not discard standings`() = runTest {
        respond(standingsJson())
        server.enqueue(MockResponse().setResponseCode(429))
        assertEquals(listOf(Standing(1, 7, "Home", "home.png", 3, 2, 1, 0, 7, 5, 1, 4, null)),
            repository.getStandings("PL"))
    }

    @Test
    fun `empty standings response maps to empty domain list`() = runTest {
        respond("""{"standings": []}""")
        matches()
        assertTrue(repository.getStandings("PL").isEmpty())
    }

    @Test
    fun `match detail maps full and half time scores without lineup data`() = runTest {
        respond(fixture(42, 1))
        assertEquals(MatchDetail(42, "Home", "Away", 2, 1, "2025-08-10T12:00:00Z", "FINISHED",
            1, 0, 2, 1, "home.png", null), repository.getMatchDetail(42))
        assertPath("/v4/matches/42")
    }

    @Test
    fun `match detail preserves absent scores and half time`() = runTest {
        respond(fixture(42, 1, "SCHEDULED", "null", "null").replace(
            "\"halfTime\": {\"home\": 1, \"away\": 0}", "\"halfTime\": null"
        ))
        assertEquals(MatchDetail(42, "Home", "Away", null, null, "2025-08-10T12:00:00Z", "SCHEDULED",
            null, null, null, null, "home.png", null), repository.getMatchDetail(42))
    }

    @Test
    fun `team maps club information and all squad fields including nullable values`() = runTest {
        respond("""
            {"id":7,"name":"Home","shortName":"H","tla":"HOM","crest":"home.png",
             "founded":1900,"venue":"Stadium","clubColors":"Blue",
             "squad":[{"id":9,"name":"Player","position":"Forward","nationality":"Spain",
                       "shirtNumber":10,"dateOfBirth":"2000-01-02"},
                      {"id":null,"name":"Youth","position":null,"nationality":null,
                       "shirtNumber":null,"dateOfBirth":null}]}
        """.trimIndent())
        assertEquals(TeamDetails(7, "Home", "H", "HOM", "home.png", 1900, "Stadium", "Blue", listOf(
            SquadPlayer(9, "Player", "Forward", "Spain", 10, "2000-01-02"),
            SquadPlayer(null, "Youth", null, null, null, null)
        )), repository.getTeamById(7))
        assertPath("/v4/teams/7")
    }

    @Test
    fun `team with missing optional metadata and squad returns empty squad`() = runTest {
        respond("""{"id":7,"name":"Home"}""")
        assertEquals(TeamDetails(7, "Home", null, null, null, null, null, null, emptyList()),
            repository.getTeamById(7))
    }

    @Test
    fun `current matchday HTTP failure is propagated`() = runTest {
        assertHttpError(403) { repository.getCurrentMatchdayResult("PL") }
    }

    @Test
    fun `selected matchday HTTP failure is propagated`() = runTest {
        assertHttpError(429) { repository.getMatchesByMatchday("PL", 1, 2025) }
    }

    @Test
    fun `standings HTTP failure is propagated without requesting form`() = runTest {
        assertHttpError(500) { repository.getStandings("PL") }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `match detail HTTP failure is propagated`() = runTest {
        assertHttpError(404) { repository.getMatchDetail(42) }
    }

    @Test
    fun `team HTTP failure is propagated`() = runTest {
        assertHttpError(401) { repository.getTeamById(7) }
    }

    @Test
    fun `empty match detail body is rejected`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        assertFailure("Empty response") { repository.getMatchDetail(42) }
    }

    @Test
    fun `empty team body is rejected`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        assertFailure("Empty response") { repository.getTeamById(7) }
    }

    private fun repositoryAt(instant: String) = FootballRepositoryImpl(
        api, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
    )

    private fun respond(json: String) {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(json))
    }

    private fun matches(vararg fixtures: String) = respond("{\"matches\":[${fixtures.joinToString()}]}")

    private fun assertPath(expected: String) {
        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull("Expected an HTTP request", request)
        assertEquals(expected, request?.path)
    }

    private suspend fun assertHttpError(code: Int, action: suspend () -> Any) {
        server.enqueue(MockResponse().setResponseCode(code))
        assertFailure("HTTP $code:", action)
    }

    private suspend fun assertFailure(message: String, action: suspend () -> Any) {
        try {
            action()
            fail("Expected failure containing $message")
        } catch (e: Exception) {
            assertTrue("Unexpected failure: $e", e.message.orEmpty().contains(message))
        }
    }

    private fun expectedMatch(id: Int, status: String = "FINISHED") =
        Match(id, "Home", "Away", 2, 1, "2025-08-10T12:00:00Z", status, "home.png", null)

    private fun fixture(
        id: Int, matchday: Int?, status: String = "FINISHED", home: String = "2", away: String = "1"
    ) = """
        {"id":$id,"matchday":$matchday,"utcDate":"2025-08-10T12:00:00Z","status":"$status",
         "homeTeam":{"id":7,"name":"Home","crest":"home.png"},
         "awayTeam":{"id":8,"name":"Away","crest":null},
         "score":{"fullTime":{"home":$home,"away":$away},"halfTime": {"home": 1, "away": 0}}}
    """.trimIndent()

    private fun standingsJson(form: String = "null") = """
        {"standings":[{"table":[{"position":1,"team":{"id":7,"name":"Home","crest":"home.png"},
         "playedGames":3,"won":2,"draw":1,"lost":0,"points":7,"goalsFor":5,"goalsAgainst":1,
         "goalDifference":4,"form":$form}]}]}
    """.trimIndent()
}
