package com.artsiom.footballpulse.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.artsiom.footballpulse.data.FootballRepository
import com.artsiom.footballpulse.domain.model.Leagues

class MatchesViewModel : ViewModel() {

    private val repository = FootballRepository()
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val uiState: StateFlow<MatchesUiState> = _uiState

    val leagues = Leagues.ALL

    val leagueMaxMatchdays = mapOf(
        "PL"  to 38,
        "PD"  to 38,
        "BL1" to 34,
        "FL1" to 34,
        "SA"  to 38
    )

    private val _currentMatchday = MutableStateFlow(1)
    val currentMatchday: StateFlow<Int> = _currentMatchday

    private val _currentLeagueCode = MutableStateFlow("PL")
    val currentLeagueCode: StateFlow<String> = _currentLeagueCode

    private var lastRequest: (() -> Unit)? = null

    private var currentSeason: Int = 2025 // updated on first successful load

    init {
        loadLeague("PL")
    }

    /** Called when the user taps a league tab. */
    fun loadLeague(leagueCode: String) {
        lastRequest = { loadLeague(leagueCode) }
        _currentLeagueCode.value = leagueCode
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MatchesUiState.Loading
            try {
                val result = repository.getCurrentMatchdayResult(leagueCode)
                currentSeason = result.season
                _currentMatchday.value = result.matchday
                _uiState.value = MatchesUiState.Success(result.matches)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = MatchesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun retry() {
        lastRequest?.invoke()
    }

    fun goToPreviousMatchday() {
        val prev = (_currentMatchday.value - 1).coerceAtLeast(1)
        if (prev != _currentMatchday.value) {
            _currentMatchday.value = prev
            fetchMatchday(prev)
        }
    }

    fun goToNextMatchday() {
        val max = leagueMaxMatchdays[_currentLeagueCode.value] ?: 38
        val next = (_currentMatchday.value + 1).coerceAtMost(max)
        if (next != _currentMatchday.value) {
            _currentMatchday.value = next
            fetchMatchday(next)
        }
    }

    private fun fetchMatchday(
        matchday: Int,
        leagueCode: String = _currentLeagueCode.value,
        season: Int = currentSeason
    ) {
        lastRequest = { fetchMatchday(matchday, leagueCode, season) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MatchesUiState.Loading
            try {
                val matches = repository.getMatchesByMatchday(
                    leagueCode, matchday, season
                )
                _uiState.value = MatchesUiState.Success(matches)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = MatchesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
