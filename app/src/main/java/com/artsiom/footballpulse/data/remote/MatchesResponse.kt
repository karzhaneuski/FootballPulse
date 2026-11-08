package com.artsiom.footballpulse.data.remote

import com.google.gson.annotations.SerializedName

data class MatchesResponse(
    @SerializedName("matches") val matches: List<MatchDto>
)
