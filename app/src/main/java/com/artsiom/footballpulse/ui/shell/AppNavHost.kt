package com.artsiom.footballpulse.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artsiom.footballpulse.ui.matchdetails.MatchDetailsScreen
import com.artsiom.footballpulse.ui.matchdetails.MatchDetailsViewModel
import com.artsiom.footballpulse.ui.matches.MatchesActions
import com.artsiom.footballpulse.ui.matches.MatchesScreen
import com.artsiom.footballpulse.ui.matches.MatchesViewModel
import com.artsiom.footballpulse.ui.standings.StandingsActions
import com.artsiom.footballpulse.ui.standings.StandingsScreen
import com.artsiom.footballpulse.ui.standings.StandingsViewModel
import com.artsiom.footballpulse.ui.teamdetails.TeamDetailsScreen
import com.artsiom.footballpulse.ui.teamdetails.TeamDetailsViewModel

@Composable
internal fun AppNavHost(navController: NavHostController, modifier: Modifier) {
    // Tab ViewModels live in the activity so loaded data survives tab switches and details screens.
    val activityOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val onBack: () -> Unit = {
        // Ignore a second tap while the pop animation runs; popping the start destination would blank the app.
        if (navController.previousBackStackEntry != null) navController.popBackStack()
    }
    NavHost(navController, startDestination = Routes.MATCHES, modifier = modifier) {
        matchesDestination(navController, activityOwner)
        standingsDestination(navController, activityOwner)
        matchDetailsDestination(onBack)
        teamDetailsDestination(onBack)
    }
}

private fun NavGraphBuilder.matchesDestination(
    navController: NavHostController,
    owner: ViewModelStoreOwner
) = composable(Routes.MATCHES) {
    val viewModel: MatchesViewModel = hiltViewModel(viewModelStoreOwner = owner)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leagueCode by viewModel.currentLeagueCode.collectAsStateWithLifecycle()
    val navigation by viewModel.navigation.collectAsStateWithLifecycle()
    MatchesScreen(
        state = state,
        leagues = viewModel.leagues,
        selectedLeagueCode = leagueCode,
        navigation = navigation,
        actions = MatchesActions(
            onLeagueSelected = viewModel::loadLeague,
            onPreviousMatchday = viewModel::goToPreviousMatchday,
            onNextMatchday = viewModel::goToNextMatchday,
            onRetry = viewModel::retry,
            onMatchClick = { navController.navigate(Routes.matchDetails(it)) }
        )
    )
}

private fun NavGraphBuilder.standingsDestination(
    navController: NavHostController,
    owner: ViewModelStoreOwner
) = composable(Routes.STANDINGS) {
    val viewModel: StandingsViewModel = hiltViewModel(viewModelStoreOwner = owner)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leagueCode by viewModel.currentLeagueCode.collectAsStateWithLifecycle()
    val mode by viewModel.viewMode.collectAsStateWithLifecycle()
    StandingsScreen(
        state = state,
        leagues = viewModel.leagues,
        selectedLeagueCode = leagueCode,
        mode = mode,
        actions = StandingsActions(
            onLeagueSelected = viewModel::loadLeague,
            onModeSelected = viewModel::setViewMode,
            onRetry = viewModel::retry,
            onTeamClick = { navController.navigate(Routes.teamDetails(it)) }
        )
    )
}

private fun NavGraphBuilder.matchDetailsDestination(onBack: () -> Unit) =
    composable(Routes.MATCH_DETAILS, arguments = listOf(Routes.matchIdArgument)) { entry ->
        // Scoped to the back stack entry: cleared when the details screen is popped.
        val viewModel: MatchDetailsViewModel = hiltViewModel(entry)
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        MatchDetailsScreen(state = state, onRetry = viewModel::retry, onBack = onBack)
    }

private fun NavGraphBuilder.teamDetailsDestination(onBack: () -> Unit) =
    composable(Routes.TEAM_DETAILS, arguments = listOf(Routes.teamIdArgument)) { entry ->
        val viewModel: TeamDetailsViewModel = hiltViewModel(entry)
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        TeamDetailsScreen(state = state, onRetry = viewModel::retry, onBack = onBack)
    }
