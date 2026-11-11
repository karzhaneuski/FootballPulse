package com.artsiom.footballpulse.ui.matchdetails

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.SavedStateHandle
import com.artsiom.footballpulse.ui.shell.Routes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artsiom.footballpulse.domain.repository.FootballRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MatchDetailsViewModel(
    private val repository: FootballRepository,
    private val matchId: Int
) : ViewModel() {
    @Inject
    constructor(repository: FootballRepository, savedStateHandle: SavedStateHandle) :
        this(repository, checkNotNull(savedStateHandle.get<Int>(Routes.ARG_MATCH_ID)))


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
