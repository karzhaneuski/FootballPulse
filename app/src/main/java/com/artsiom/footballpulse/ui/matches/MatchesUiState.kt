package com.artsiom.footballpulse.ui.matches

import com.artsiom.footballpulse.domain.model.Match

sealed interface MatchesUiState {
    object Loading : MatchesUiState
    data class Success(val matches: List<Match>, val items: List<MatchListItem> = mapMatches(matches)) : MatchesUiState
    data class Error(val message: String) : MatchesUiState

}