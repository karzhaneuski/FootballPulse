package com.artsiom.footballpulse.testing

import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.domain.model.MatchDetail
import com.artsiom.footballpulse.domain.model.Standing
import com.artsiom.footballpulse.domain.model.TeamDetails

val match = Match(42, "Home", "Away", 2, 1, "2025-08-10T12:00:00Z", "FINISHED", "home.png", null)
val standing = Standing(1, 7, "Home", "home.png", 3, 2, 1, 0, 7, 5, 1, 4, "WDW")
val matchDetail = MatchDetail(
    42, "Home", "Away", 2, 1, "2025-08-10T12:00:00Z", "FINISHED", 1, 0, 2, 1, "home.png", null
)
val team = TeamDetails(7, "Home", "H", "HOM", "home.png", 1900, "Stadium", "Blue", emptyList())
