package com.artsiom.footballpulse.ui.standings

class StandingsActions(
    val onLeagueSelected: (String) -> Unit,
    val onModeSelected: (ViewMode) -> Unit,
    val onRetry: () -> Unit,
    val onTeamClick: (Int) -> Unit
)

