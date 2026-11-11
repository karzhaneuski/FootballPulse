package com.artsiom.footballpulse.ui.standings

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artsiom.footballpulse.domain.model.Leagues
import com.artsiom.footballpulse.domain.repository.FootballRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StandingsViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private var loadJob: Job? = null

    val leagues = Leagues.ALL

    private val _currentLeagueCode = MutableStateFlow("PL")
    val currentLeagueCode: StateFlow<String> = _currentLeagueCode

    private val _uiState = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val uiState: StateFlow<StandingsUiState> = _uiState

    private val _viewMode = MutableStateFlow(ViewMode.FULL)
    val viewMode: StateFlow<ViewMode> = _viewMode

    init {
        fetchStandings()
    }

    /** Switch league tab. Re-fetches only if league changed or data not yet loaded. */
    fun loadLeague(code: String) {
        if (_currentLeagueCode.value == code && _uiState.value is StandingsUiState.Success) return
        _currentLeagueCode.value = code
        fetchStandings()
    }

    /** Switch view mode without re-fetching data. */
    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun retry() {
        fetchStandings()
    }

    private fun fetchStandings() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = StandingsUiState.Loading
            try {
                val standings = repository.getStandings(_currentLeagueCode.value)
                _uiState.value = StandingsUiState.Success(
                    standings, mapStandings(standings, Leagues.forCode(_currentLeagueCode.value))
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = StandingsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
