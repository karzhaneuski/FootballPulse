package com.artsiom.footballpulse.ui.matches

class MatchesActions(
    val onLeagueSelected: (String) -> Unit,
    val onPreviousMatchday: () -> Unit,
    val onNextMatchday: () -> Unit,
    val onRetry: () -> Unit,
    val onMatchClick: (Int) -> Unit
)
