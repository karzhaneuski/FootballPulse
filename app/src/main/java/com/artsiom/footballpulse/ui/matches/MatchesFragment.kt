package com.artsiom.footballpulse.ui.matches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artsiom.footballpulse.FootballPulseApp
import com.artsiom.footballpulse.MainActivity
import com.artsiom.footballpulse.R
import kotlinx.coroutines.launch

class MatchesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_matches, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val errorLayout = view.findViewById<View>(R.id.errorLayout)
        val retryButton = view.findViewById<Button>(R.id.retryButton)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        val leaguesRecyclerView = view.findViewById<RecyclerView>(R.id.leaguesRecyclerView)
        val matchdayLabel = view.findViewById<TextView>(R.id.matchdayLabel)
        val btnPrev = view.findViewById<TextView>(R.id.btnPrevMatchday)
        val btnNext = view.findViewById<TextView>(R.id.btnNextMatchday)

        // Scope ViewModel to Activity so data survives tab switches
        val repository = (requireActivity().application as FootballPulseApp).container.repository
        val factory = viewModelFactory {
            initializer { MatchesViewModel(repository) }
        }
        val viewModel = ViewModelProvider(requireActivity(), factory)[MatchesViewModel::class.java]

        val matchesAdapter = MatchesAdapter { match ->
            (requireActivity() as MainActivity).navigateToMatchDetails(match.id)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = matchesAdapter

        val leaguesAdapter = LeaguesAdapter { league -> viewModel.loadLeague(league.code) }
        leaguesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        leaguesRecyclerView.adapter = leaguesAdapter
        leaguesAdapter.submitList(viewModel.leagues)
        val matchesLeagueIdx = viewModel.leagues.indexOfFirst { it.code == viewModel.currentLeagueCode.value }
        if (matchesLeagueIdx >= 0) leaguesAdapter.setSelectedPosition(matchesLeagueIdx)

        retryButton.setOnClickListener { viewModel.retry() }

        btnPrev.setOnClickListener { viewModel.goToPreviousMatchday() }
        btnNext.setOnClickListener { viewModel.goToNextMatchday() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.navigation.collect { navigation ->
                        matchdayLabel.text = getString(R.string.matchday_format, navigation.matchday)
                        btnPrev.isEnabled = navigation.previousEnabled
                        btnNext.isEnabled = navigation.nextEnabled
                        btnPrev.alpha = if (btnPrev.isEnabled) 1.0f else 0.3f
                        btnNext.alpha = if (btnNext.isEnabled) 1.0f else 0.3f
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is MatchesUiState.Loading -> {
                                progressBar.visibility = View.VISIBLE
                                recyclerView.visibility = View.GONE
                                errorLayout.visibility = View.GONE
                            }
                            is MatchesUiState.Success -> {
                                progressBar.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                                errorLayout.visibility = View.GONE
                                matchesAdapter.submitList(state.items)
                            }
                            is MatchesUiState.Error -> {
                                progressBar.visibility = View.GONE
                                recyclerView.visibility = View.GONE
                                errorLayout.visibility = View.VISIBLE
                                errorText.text = state.message
                            }
                        }
                    }
                }
            }
        }
    }
}
