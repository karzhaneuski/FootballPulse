package com.artsiom.footballpulse.ui.matches

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artsiom.footballpulse.domain.model.Leagues
import com.artsiom.footballpulse.domain.model.FootballSeason
import com.artsiom.footballpulse.domain.repository.FootballRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val uiState: StateFlow<MatchesUiState> = _uiState

    val leagues = Leagues.ALL

    private val _currentMatchday = MutableStateFlow(1)
    val currentMatchday: StateFlow<Int> = _currentMatchday

    private val _currentLeagueCode = MutableStateFlow("PL")
    val currentLeagueCode: StateFlow<String> = _currentLeagueCode

    val navigation: StateFlow<MatchdayNavigation> = combine(
        _currentMatchday, _currentLeagueCode, _uiState
    ) { matchday, code, state ->
        MatchdayNavigation(
            matchday = matchday,
            previousEnabled = state !is MatchesUiState.Loading && matchday > 1,
            nextEnabled = state !is MatchesUiState.Loading && matchday < Leagues.forCode(code).maxMatchdays
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MatchdayNavigation(1, false, false))

    private var lastRequest: (() -> Unit)? = null

    private var currentSeason: Int = FootballSeason.current() // replaced by the loaded season on success

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
        val max = Leagues.forCode(_currentLeagueCode.value).maxMatchdays
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

data class MatchdayNavigation(val matchday: Int, val previousEnabled: Boolean, val nextEnabled: Boolean)
