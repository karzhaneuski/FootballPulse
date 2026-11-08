package com.artsiom.footballpulse.ui.matchdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.artsiom.footballpulse.data.FootballRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchDetailsViewModel(private val matchId: Int) : ViewModel() {

    private val repository = FootballRepository()

    private val _uiState = MutableStateFlow<MatchDetailsUiState>(MatchDetailsUiState.Loading)
    val uiState: StateFlow<MatchDetailsUiState> = _uiState

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = MatchDetailsUiState.Loading
            try {
                val match = repository.getMatchDetail(matchId)
                _uiState.value = MatchDetailsUiState.Success(match)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = MatchDetailsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

class MatchDetailsViewModelFactory(private val matchId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MatchDetailsViewModel(matchId) as T
    }
}
