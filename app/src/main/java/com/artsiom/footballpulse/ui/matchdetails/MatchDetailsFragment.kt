package com.artsiom.footballpulse.ui.matchdetails

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

class MatchDetailsFragment : Fragment() {
    companion object {
        private const val ARG_MATCH_ID = "match_id"

        fun newInstance(matchId: Int): MatchDetailsFragment = MatchDetailsFragment().apply {
            arguments = Bundle().apply { putInt(ARG_MATCH_ID, matchId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val matchId = requireArguments().getInt(ARG_MATCH_ID)
        val repository = (requireActivity().application as FootballPulseApp).container.repository
        val factory = viewModelFactory {
            initializer { MatchDetailsViewModel(repository, matchId) }
        }
        val viewModel = ViewModelProvider(this, factory)[MatchDetailsViewModel::class.java]
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                FootballPulseTheme {
                    MatchDetailsScreen(
                        state = state,
                        onRetry = viewModel::retry,
                        onBack = { requireActivity().supportFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }
}
