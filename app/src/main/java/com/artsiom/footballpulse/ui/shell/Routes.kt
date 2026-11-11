package com.artsiom.footballpulse.ui.shell

import androidx.navigation.NavType
import androidx.navigation.navArgument

internal object Routes {
    const val MATCHES = "matches"
    const val STANDINGS = "standings"
    const val ARG_MATCH_ID = "matchId"
    const val ARG_TEAM_ID = "teamId"
    const val MATCH_DETAILS = "match/{$ARG_MATCH_ID}"
    const val TEAM_DETAILS = "team/{$ARG_TEAM_ID}"

    fun matchDetails(matchId: Int) = "match/$matchId"
    fun teamDetails(teamId: Int) = "team/$teamId"

    val matchIdArgument = navArgument(ARG_MATCH_ID) { type = NavType.IntType }
    val teamIdArgument = navArgument(ARG_TEAM_ID) { type = NavType.IntType }

}
