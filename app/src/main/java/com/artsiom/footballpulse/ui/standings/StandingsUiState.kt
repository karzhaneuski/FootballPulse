package com.artsiom.footballpulse.ui.standings

import com.artsiom.footballpulse.domain.model.Standing

sealed interface StandingsUiState {
    object Loading : StandingsUiState
    data class Success(
        val standings: List<Standing>,
        val rows: List<StandingUiModel> = mapStandings(standings)
    ) : StandingsUiState
    data class Error(val message: String) : StandingsUiState
}
