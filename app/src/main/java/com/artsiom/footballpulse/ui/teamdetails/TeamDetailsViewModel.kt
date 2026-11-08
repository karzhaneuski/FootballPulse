package com.artsiom.footballpulse.ui.teamdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.artsiom.footballpulse.data.FootballRepository
import com.artsiom.footballpulse.domain.model.TeamDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface TeamDetailsUiState {
    object Loading : TeamDetailsUiState
    data class Success(val team: TeamDetails) : TeamDetailsUiState
    data class Error(val message: String) : TeamDetailsUiState
}

class TeamDetailsViewModel(private val teamId: Int) : ViewModel() {

    private val repository = FootballRepository()

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

class TeamDetailsViewModelFactory(private val teamId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TeamDetailsViewModel(teamId) as T
    }
}
