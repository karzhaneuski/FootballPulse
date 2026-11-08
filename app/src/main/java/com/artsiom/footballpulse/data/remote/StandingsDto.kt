package com.artsiom.footballpulse.data.remote

import com.google.gson.annotations.SerializedName

data class TableEntryDto(
    @SerializedName("position") val position: Int,
    @SerializedName("team") val team: TeamDto,
    @SerializedName("playedGames") val playedGames: Int,
    @SerializedName("won") val won: Int,
    @SerializedName("draw") val draw: Int,
    @SerializedName("lost") val lost: Int,
    @SerializedName("points") val points: Int,
    @SerializedName("goalsFor") val goalsFor: Int,
    @SerializedName("goalsAgainst") val goalsAgainst: Int,
    @SerializedName("goalDifference") val goalDifference: Int,
    @SerializedName("form") val form: String?
)

data class StandingDto(
    @SerializedName("table") val table: List<TableEntryDto>
)

data class StandingsResponse(
    @SerializedName("standings") val standings: List<StandingDto>
)

