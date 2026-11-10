package com.artsiom.footballpulse.domain.model

object Leagues {
    private const val LARGE_LEAGUE_SIZE = 20
    private const val SMALL_LEAGUE_SIZE = 18
    private const val SHORT_SEASON_MATCHDAYS = 34
    private const val UCL_LAST_POSITION = 4
    private const val UEL_POSITION = 5
    private const val UECL_POSITION = 6
    private const val SMALL_LEAGUE_RELEGATIONS = 2
    private const val LARGE_LEAGUE_RELEGATIONS = 3
    private const val PLAYOFF_POSITION = 16

    val ALL = listOf(
        League("PL", "Premier League", "PL", DEFAULT_MAX_MATCHDAYS,
            standardZones(LARGE_LEAGUE_SIZE, conferencePosition = null)),
        League("PD", "La Liga", "La Liga", DEFAULT_MAX_MATCHDAYS, standardZones(LARGE_LEAGUE_SIZE)),
        League("BL1", "Bundesliga", "BL", SHORT_SEASON_MATCHDAYS, standardZones(SMALL_LEAGUE_SIZE)),
        // Fourth place is a Champions League qualifying berth, included in the UCL zone.
        League("FL1", "Ligue 1", "L1", SHORT_SEASON_MATCHDAYS, standardZones(SMALL_LEAGUE_SIZE)),
        League("SA", "Serie A", "SA", DEFAULT_MAX_MATCHDAYS, standardZones(LARGE_LEAGUE_SIZE))
    )
    val DEFAULT: League = ALL.first()

    fun forCode(code: String): League = ALL.firstOrNull { it.code == code } ?: DEFAULT

    // Base league places only: cup winners, redistribution and UEFA performance spots need season overrides.
    // England's Conference League berth belongs to the EFL Cup winner, not a fixed league position.
    private fun standardZones(
        teamCount: Int,
        conferencePosition: Int? = UECL_POSITION
    ): Map<Int, LeagueZone> = buildMap {
        for (position in 1..UCL_LAST_POSITION) put(position, LeagueZone.UCL)
        put(UEL_POSITION, LeagueZone.UEL)
        conferencePosition?.let { put(it, LeagueZone.UECL) }
        val directRelegations = if (teamCount == SMALL_LEAGUE_SIZE) {
            SMALL_LEAGUE_RELEGATIONS
        } else LARGE_LEAGUE_RELEGATIONS
        for (position in teamCount - directRelegations + 1..teamCount) {
            put(position, LeagueZone.RELEGATION)
        }
        if (teamCount == SMALL_LEAGUE_SIZE) put(PLAYOFF_POSITION, LeagueZone.PLAYOFF)
    }
}
