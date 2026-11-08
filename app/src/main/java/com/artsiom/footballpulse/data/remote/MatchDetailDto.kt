package com.artsiom.footballpulse.data.remote

import com.google.gson.annotations.SerializedName

data class MatchDetailDto(
    @SerializedName("id")       val id: Int,
    @SerializedName("utcDate")  val utcDate: String,
    @SerializedName("status")   val status: String,
    @SerializedName("homeTeam") val homeTeam: TeamDto,
    @SerializedName("awayTeam") val awayTeam: TeamDto,
    @SerializedName("score")    val score: ScoreDto
)
