package com.artsiom.footballpulse.data.remote

import com.google.gson.annotations.SerializedName

data class TeamDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("crest") val crest: String?
)
