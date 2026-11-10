package com.artsiom.footballpulse.ui.standings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.artsiom.footballpulse.domain.model.LeagueZone
import com.artsiom.footballpulse.ui.matches.LeaguesAdapter
import kotlinx.coroutines.launch

class StandingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_standings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Scope ViewModel to Activity so data survives tab switches
        val repository = (requireActivity().application as FootballPulseApp).container.repository
        val factory = viewModelFactory {
            initializer { StandingsViewModel(repository) }
        }
        val viewModel = ViewModelProvider(requireActivity(), factory)[StandingsViewModel::class.java]

        val leaguesRecyclerView = view.findViewById<RecyclerView>(R.id.standingsLeaguesRecyclerView)
        val standingsRecyclerView = view.findViewById<RecyclerView>(R.id.standingsRecyclerView)
        val progressBar = view.findViewById<ProgressBar>(R.id.standingsProgressBar)
        val errorLayout = view.findViewById<View>(R.id.errorLayout)
        val errorText = view.findViewById<TextView>(R.id.standingsErrorText)
        val retryButton = view.findViewById<Button>(R.id.retryButton)
        val btnFull = view.findViewById<TextView>(R.id.btnFull)
        val btnShort = view.findViewById<TextView>(R.id.btnShort)
        val btnForm = view.findViewById<TextView>(R.id.btnForm)
        bindLegend(view.findViewById(R.id.standingsLegendText))

        // League selector (reuses existing LeaguesAdapter)
        val leaguesAdapter = LeaguesAdapter { league -> viewModel.loadLeague(league.code) }
        leaguesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        leaguesRecyclerView.adapter = leaguesAdapter
        leaguesAdapter.submitList(viewModel.leagues)
        val leagueIdx = viewModel.leagues.indexOfFirst { it.code == viewModel.currentLeagueCode.value }
        if (leagueIdx >= 0) leaguesAdapter.setSelectedPosition(leagueIdx)

        // Standings table
        val standingsAdapter = StandingsAdapter { teamId ->
            (requireActivity() as MainActivity).navigateToTeamDetails(teamId)
        }
        standingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        standingsRecyclerView.adapter = standingsAdapter

        // Mode toggle buttons
        btnFull.setOnClickListener { viewModel.setViewMode(ViewMode.FULL) }
        btnShort.setOnClickListener { viewModel.setViewMode(ViewMode.SHORT) }
        btnForm.setOnClickListener { viewModel.setViewMode(ViewMode.FORM) }

        retryButton.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is StandingsUiState.Loading -> {
                                progressBar.visibility = View.VISIBLE
                                standingsRecyclerView.visibility = View.GONE
                                errorLayout.visibility = View.GONE
                            }
                            is StandingsUiState.Success -> {
                                progressBar.visibility = View.GONE
                                standingsRecyclerView.visibility = View.VISIBLE
                                errorLayout.visibility = View.GONE
                                standingsAdapter.submitList(state.rows)
                            }
                            is StandingsUiState.Error -> {
                                progressBar.visibility = View.GONE
                                standingsRecyclerView.visibility = View.GONE
                                errorLayout.visibility = View.VISIBLE
                                errorText.text = state.message
                            }
                        }
                    }
                }

                launch {
                    viewModel.viewMode.collect { mode ->
                        standingsAdapter.mode = mode
                        updateModeButtons(btnFull, btnShort, btnForm, mode)
                    }
                }
            }
        }
    }

    private fun bindLegend(label: TextView) {
        val text = SpannableStringBuilder()
        listOf(
            LeagueZone.UCL to R.string.zone_ucl,
            LeagueZone.UEL to R.string.zone_uel,
            LeagueZone.UECL to R.string.zone_uecl,
            LeagueZone.RELEGATION to R.string.zone_relegation,
            LeagueZone.PLAYOFF to R.string.zone_playoff
        ).forEach { (zone, title) ->
            if (text.isNotEmpty()) text.append("  ")
            text.append("● ", ForegroundColorSpan(standingsZoneColor(requireContext(), zone)),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.append(getString(title))
        }
        label.text = text
    }

    private fun updateModeButtons(
        btnFull: TextView, btnShort: TextView, btnForm: TextView, mode: ViewMode
    ) {
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density

        listOf(btnFull to ViewMode.FULL, btnShort to ViewMode.SHORT, btnForm to ViewMode.FORM)
            .forEach { (btn, btnMode) ->
                val selected = btnMode == mode
                val bgColor = ContextCompat.getColor(
                    ctx,
                    if (selected) R.color.color_tab_selected_bg else R.color.color_tab_unselected_bg
                )
                val textColor = ContextCompat.getColor(
                    ctx,
                    if (selected) R.color.color_tab_selected_text else R.color.color_tab_unselected_text
                )
                btn.background = GradientDrawable().apply {
                    cornerRadius = 20f * density
                    setColor(bgColor)
                }
                btn.setTextColor(textColor)
            }
    }
}
