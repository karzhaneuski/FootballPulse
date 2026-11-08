package com.artsiom.footballpulse.domain.model

data class MatchDetail(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val date: String,
    val status: String,
    val halfTimeHome: Int?,
    val halfTimeAway: Int?,
    val fullTimeHome: Int?,
    val fullTimeAway: Int?,
    val homeTeamCrest: String?,
    val awayTeamCrest: String?
)
