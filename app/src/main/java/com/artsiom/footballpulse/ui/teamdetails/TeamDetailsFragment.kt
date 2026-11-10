package com.artsiom.footballpulse.ui.teamdetails

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
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme

class TeamDetailsFragment : Fragment() {
    companion object {
        private const val ARG_TEAM_ID = "team_id"

        fun newInstance(teamId: Int): TeamDetailsFragment = TeamDetailsFragment().apply {
            arguments = Bundle().apply { putInt(ARG_TEAM_ID, teamId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val teamId = requireArguments().getInt(ARG_TEAM_ID)
        val repository = (requireActivity().application as FootballPulseApp).container.repository
        val factory = viewModelFactory {
            initializer { TeamDetailsViewModel(repository, teamId) }
        }
        val viewModel = ViewModelProvider(this, factory)[TeamDetailsViewModel::class.java]
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                FootballPulseTheme {
                    TeamDetailsScreen(
                        state = state,
                        onRetry = viewModel::retry,
                        onBack = { requireActivity().supportFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }
}
