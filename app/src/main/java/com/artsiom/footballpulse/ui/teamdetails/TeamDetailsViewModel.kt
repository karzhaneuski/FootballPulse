package com.artsiom.footballpulse.ui.teamdetails

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import com.artsiom.footballpulse.ui.shell.Routes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artsiom.footballpulse.domain.model.TeamDetails
import com.artsiom.footballpulse.domain.repository.FootballRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface TeamDetailsUiState {
    object Loading : TeamDetailsUiState
    data class Success(
        val team: TeamDetails,
        val content: TeamDetailsUiModel = mapTeamDetails(team)
    ) : TeamDetailsUiState
    data class Error(val message: String) : TeamDetailsUiState
}

@HiltViewModel
class TeamDetailsViewModel(
    private val repository: FootballRepository,
    private val teamId: Int
) : ViewModel() {
    @Inject
    constructor(repository: FootballRepository, savedStateHandle: SavedStateHandle) :
        this(repository, checkNotNull(savedStateHandle.get<Int>(Routes.ARG_TEAM_ID)))


    private val _uiState = MutableStateFlow<TeamDetailsUiState>(TeamDetailsUiState.Loading)
    val uiState: StateFlow<TeamDetailsUiState> = _uiState

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = TeamDetailsUiState.Loading
            try {
                val team = repository.getTeamById(teamId)
                _uiState.value = TeamDetailsUiState.Success(team)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = TeamDetailsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
