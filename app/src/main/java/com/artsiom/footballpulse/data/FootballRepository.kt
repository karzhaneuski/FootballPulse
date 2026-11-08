package com.artsiom.footballpulse.data

import com.artsiom.footballpulse.data.remote.MatchDto
import com.artsiom.footballpulse.data.remote.RetrofitInstance
import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.domain.model.MatchDetail
import com.artsiom.footballpulse.domain.model.Player
import com.artsiom.footballpulse.domain.model.SquadPlayer
import com.artsiom.footballpulse.domain.model.Standing
import com.artsiom.footballpulse.domain.model.TeamDetails
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.util.Calendar

data class MatchdayResult(
    val matchday: Int,
    val season: Int,
    val matches: List<Match>
)

class FootballRepository {

    suspend fun getCurrentMatchdayResult(leagueCode: String): MatchdayResult {
        val season = computeCurrentSeason()
        val response = RetrofitInstance.api.getMatches(leagueCode, season)
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.message()}")
        }
        val allMatches = response.body()?.matches ?: emptyList()

        // Prefer an active/upcoming match to determine the current matchday
        val pivotMatch = allMatches.firstOrNull {
            it.status == "IN_PLAY" || it.status == "PAUSED" || it.status == "TIMED" || it.status == "SCHEDULED"
        }
        val currentMatchday = pivotMatch?.matchday
            ?: allMatches.mapNotNull { it.matchday }.maxOrNull()
            ?: 1

        val domainMatches = allMatches
            .filter { it.matchday == currentMatchday }
            .map { dto ->
                Match(
                    id = dto.id,
                    homeTeam = dto.homeTeam.name,
                    awayTeam = dto.awayTeam.name,
                    date = dto.utcDate,
                    status = dto.status,
                    homeScore = dto.score.fullTime.home,
                    awayScore = dto.score.fullTime.away,
                    homeTeamCrest = dto.homeTeam.crest,
                    awayTeamCrest = dto.awayTeam.crest
                )
            }

        return MatchdayResult(currentMatchday, season, domainMatches)
    }

    suspend fun getMatchesByMatchday(leagueCode: String, matchday: Int, season: Int): List<Match> {
        val response = RetrofitInstance.api.getMatchesByMatchday(leagueCode, matchday, season)
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.message()}")
        }
        return (response.body()?.matches ?: emptyList()).map { dto ->
            Match(
                id = dto.id,
                homeTeam = dto.homeTeam.name,
                awayTeam = dto.awayTeam.name,
                date = dto.utcDate,
                status = dto.status,
                homeScore = dto.score.fullTime.home,
                awayScore = dto.score.fullTime.away,
                homeTeamCrest = dto.homeTeam.crest,
                awayTeamCrest = dto.awayTeam.crest
            )
        }
    }

    suspend fun getStandings(leagueCode: String): List<Standing> {
        val standingsResponse = RetrofitInstance.api.getStandings(leagueCode)
        if (!standingsResponse.isSuccessful) {
            throw Exception("HTTP ${standingsResponse.code()}: ${standingsResponse.message()}")
        }
        val table = standingsResponse.body()?.standings?.firstOrNull()?.table ?: emptyList()

        // football-data.org free tier returns form=null in standings.
        // Compute it from the finished matches endpoint instead.
        val formByTeamId: Map<Int, String> = try {
            val matchesResponse = RetrofitInstance.api.getMatches(leagueCode, computeCurrentSeason())
            if (matchesResponse.isSuccessful) {
                computeForm(matchesResponse.body()?.matches ?: emptyList())
            } else emptyMap()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyMap()
        }

        return table.map { dto ->
            Standing(
                position = dto.position,
                teamId = dto.team.id,
                teamName = dto.team.name,
                crest = dto.team.crest,
                playedGames = dto.playedGames,
                won = dto.won,
                draw = dto.draw,
                lost = dto.lost,
                points = dto.points,
                goalsFor = dto.goalsFor,
                goalsAgainst = dto.goalsAgainst,
                goalDifference = dto.goalDifference,
                // Prefer API form if ever non-null, else fall back to computed value
                form = dto.form ?: formByTeamId[dto.team.id]
            )
        }
    }

    /**
     * Derives the last-5-match form string (e.g. "WDLWW") for every team from
     * the list of all competition matches. Results are in chronological order
     * (oldest on the left, most recent on the right), matching the standard
     * standings display convention.
     */
    suspend fun getMatchDetail(matchId: Int): MatchDetail {
        val response = RetrofitInstance.api.getMatchDetail(matchId)
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.message()}")
        }
        val dto = response.body() ?: throw Exception("Empty response")

        fun mapPlayers(list: List<com.artsiom.footballpulse.data.remote.PlayerDto>?) =
            (list ?: emptyList())
                .map { Player(it.name, it.shirtNumber, it.position) }
                .sortedBy { it.shirtNumber ?: Int.MAX_VALUE }

        return MatchDetail(
            id = dto.id,
            homeTeam = dto.homeTeam.name,
            awayTeam = dto.awayTeam.name,
            homeScore = dto.score.fullTime.home,
            awayScore = dto.score.fullTime.away,
            date = dto.utcDate,
            status = dto.status,
            homeLineup = mapPlayers(dto.homeTeam.lineup),
            awayLineup = mapPlayers(dto.awayTeam.lineup),
            homeBench = mapPlayers(dto.homeTeam.bench),
            awayBench = mapPlayers(dto.awayTeam.bench),
            halfTimeHome = dto.score.halfTime?.home,
            halfTimeAway = dto.score.halfTime?.away,
            fullTimeHome = dto.score.fullTime.home,
            fullTimeAway = dto.score.fullTime.away,
            homeTeamCrest = dto.homeTeam.crest,
            awayTeamCrest = dto.awayTeam.crest
        )
    }

    suspend fun getTeamById(id: Int): TeamDetails {
        val response = RetrofitInstance.api.getTeam(id)
        if (!response.isSuccessful) throw Exception("HTTP ${response.code()}: ${response.message()}")
        val dto = response.body() ?: throw Exception("Empty response")
        return TeamDetails(
            id = dto.id,
            name = dto.name,
            shortName = dto.shortName,
            tla = dto.tla,
            crest = dto.crest,
            founded = dto.founded,
            venue = dto.venue,
            clubColors = dto.clubColors,
            squad = (dto.squad ?: emptyList()).map { p ->
                SquadPlayer(p.id, p.name, p.position, p.nationality, p.shirtNumber, p.dateOfBirth)
            }
        )
    }

    private fun computeCurrentSeason(): Int {
        val cal = Calendar.getInstance()
        // European leagues start in August (month index 7)
        return if (cal.get(Calendar.MONTH) >= 7) {
            cal.get(Calendar.YEAR)
        } else {
            cal.get(Calendar.YEAR) - 1
        }
    }
}

internal fun computeForm(matches: List<MatchDto>): Map<Int, String> {
    // Postponed fixtures must count when played, regardless of their original matchday.
    val finishedSorted = matches
        .filter { it.status == "FINISHED" }
        .sortedWith(
            compareBy<MatchDto> { Instant.parse(it.utcDate) }
                .thenBy { it.matchday ?: Int.MAX_VALUE }
        )

    val resultsByTeam = mutableMapOf<Int, MutableList<String>>()

    for (match in finishedSorted) {
        val homeGoals = match.score.fullTime.home ?: continue
        val awayGoals = match.score.fullTime.away ?: continue

        val homeResult = when {
            homeGoals > awayGoals -> "W"
            homeGoals < awayGoals -> "L"
            else -> "D"
        }
        val awayResult = when (homeResult) {
            "W" -> "L"
            "L" -> "W"
            else -> "D"
        }

        resultsByTeam.getOrPut(match.homeTeam.id) { mutableListOf() }.add(homeResult)
        resultsByTeam.getOrPut(match.awayTeam.id) { mutableListOf() }.add(awayResult)
    }

    // Take the last 5 results per team and join without separator
    return resultsByTeam.mapValues { (_, results) ->
        results.takeLast(5).joinToString("")
    }
}
