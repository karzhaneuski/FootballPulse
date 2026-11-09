package com.artsiom.footballpulse.testing

import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.domain.model.MatchDetail
import com.artsiom.footballpulse.domain.model.MatchdayResult
import com.artsiom.footballpulse.domain.model.Standing
import com.artsiom.footballpulse.domain.model.TeamDetails
import com.artsiom.footballpulse.domain.repository.FootballRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext

class PendingRequest<T>(val arguments: List<Any>) {
    val result = CompletableDeferred<T>()
    lateinit var job: Job
    var cancelled = false
        private set

    suspend fun await(): T {
        job = checkNotNull(currentCoroutineContext()[Job])
        return try {
            result.await()
        } catch (e: CancellationException) {
            cancelled = true
            throw e
        }
    }
}

class FakeFootballRepository : FootballRepository {
    val leagues = mutableListOf<PendingRequest<MatchdayResult>>()
    val matchdays = mutableListOf<PendingRequest<List<Match>>>()
    val standings = mutableListOf<PendingRequest<List<Standing>>>()
    val matches = mutableListOf<PendingRequest<MatchDetail>>()
    val teams = mutableListOf<PendingRequest<TeamDetails>>()

    override suspend fun getCurrentMatchdayResult(leagueCode: String): MatchdayResult =
        PendingRequest<MatchdayResult>(listOf(leagueCode)).also { leagues.add(it) }.await()

    override suspend fun getMatchesByMatchday(leagueCode: String, matchday: Int, season: Int): List<Match> =
        PendingRequest<List<Match>>(listOf(leagueCode, matchday, season)).also { matchdays.add(it) }.await()

    override suspend fun getStandings(leagueCode: String): List<Standing> =
        PendingRequest<List<Standing>>(listOf(leagueCode)).also { standings.add(it) }.await()

    override suspend fun getMatchDetail(matchId: Int): MatchDetail =
        PendingRequest<MatchDetail>(listOf(matchId)).also { matches.add(it) }.await()

    override suspend fun getTeamById(id: Int): TeamDetails =
        PendingRequest<TeamDetails>(listOf(id)).also { teams.add(it) }.await()
}
