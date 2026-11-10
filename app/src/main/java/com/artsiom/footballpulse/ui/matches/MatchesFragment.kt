package com.artsiom.footballpulse.ui.matches

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

class MatchesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val repository = (requireActivity().application as FootballPulseApp).container.repository
        val factory = viewModelFactory {
            initializer { MatchesViewModel(repository) }
        }
        // Keep the activity scope so loaded data survives tab switches.
        val viewModel = ViewModelProvider(requireActivity(), factory)[MatchesViewModel::class.java]
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val leagueCode by viewModel.currentLeagueCode.collectAsStateWithLifecycle()
                val navigation by viewModel.navigation.collectAsStateWithLifecycle()
                FootballPulseTheme {
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
                            onMatchClick = { (requireActivity() as MainActivity).navigateToMatchDetails(it) }
                        )
                    )
                }
            }
        }
    }
}
