package com.artsiom.footballpulse.ui.standings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.artsiom.footballpulse.FootballPulseApp
import com.artsiom.footballpulse.MainActivity
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme

class StandingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val repository = (requireActivity().application as FootballPulseApp).container.repository
        val factory = viewModelFactory {
            initializer { StandingsViewModel(repository) }
        }
        // Keep the activity scope so loaded data and mode survive tab switches.
        val viewModel = ViewModelProvider(requireActivity(), factory)[StandingsViewModel::class.java]
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val leagueCode by viewModel.currentLeagueCode.collectAsStateWithLifecycle()
                val mode by viewModel.viewMode.collectAsStateWithLifecycle()
                FootballPulseTheme {
                    StandingsScreen(
                        state = state,
                        leagues = viewModel.leagues,
                        selectedLeagueCode = leagueCode,
                        mode = mode,
                        actions = StandingsActions(
                            onLeagueSelected = viewModel::loadLeague,
                            onModeSelected = viewModel::setViewMode,
                            onRetry = viewModel::retry,
                            onTeamClick = { (requireActivity() as MainActivity).navigateToTeamDetails(it) }
                        )
                    )
                }
            }
        }
    }
}
