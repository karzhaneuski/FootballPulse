package com.artsiom.footballpulse.domain.model

internal const val DEFAULT_MAX_MATCHDAYS = 38

enum class LeagueZone { NONE, UCL, UEL, UECL, RELEGATION, PLAYOFF }

data class League(
    val code: String,
    val name: String,
    val shortName: String = code,
    val maxMatchdays: Int = DEFAULT_MAX_MATCHDAYS,
    val zones: Map<Int, LeagueZone> = emptyMap(),
    val seasonZoneOverrides: Map<Int, Map<Int, LeagueZone>> = emptyMap()
) {
    // An explicit NONE clears a standard place; unspecified positions retain their standard zone.
    fun zoneAt(position: Int, season: Int? = null): LeagueZone =
        seasonZoneOverrides[season]?.get(position) ?: zones[position] ?: LeagueZone.NONE
}
