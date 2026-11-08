package com.artsiom.footballpulse.domain.model

data class MatchdayResult(
    val matchday: Int,
    val season: Int,
    val matches: List<Match>
)
