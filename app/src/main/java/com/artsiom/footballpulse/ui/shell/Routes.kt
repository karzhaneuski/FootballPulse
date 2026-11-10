package com.artsiom.footballpulse.ui.shell

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

internal object Routes {
    const val MATCHES = "matches"
    const val STANDINGS = "standings"
    private const val ARG_MATCH_ID = "matchId"
    private const val ARG_TEAM_ID = "teamId"
    const val MATCH_DETAILS = "match/{$ARG_MATCH_ID}"
    const val TEAM_DETAILS = "team/{$ARG_TEAM_ID}"

    fun matchDetails(matchId: Int) = "match/$matchId"
    fun teamDetails(teamId: Int) = "team/$teamId"

    val matchIdArgument = navArgument(ARG_MATCH_ID) { type = NavType.IntType }
    val teamIdArgument = navArgument(ARG_TEAM_ID) { type = NavType.IntType }

    fun matchId(entry: NavBackStackEntry): Int = requireNotNull(entry.arguments).getInt(ARG_MATCH_ID)
    fun teamId(entry: NavBackStackEntry): Int = requireNotNull(entry.arguments).getInt(ARG_TEAM_ID)
}
