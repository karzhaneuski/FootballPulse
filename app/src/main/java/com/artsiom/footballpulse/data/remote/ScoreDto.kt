package com.artsiom.footballpulse.data.remote

import com.google.gson.annotations.SerializedName

data class ScoreDto(
    @SerializedName("fullTime") val fullTime: FullTimeDto,
    @SerializedName("halfTime") val halfTime: HalfTimeDto?
)

data class FullTimeDto(
    @SerializedName("home") val home: Int?,
    @SerializedName("away") val away: Int?
)

data class HalfTimeDto(
    @SerializedName("home") val home: Int?,
    @SerializedName("away") val away: Int?
)
