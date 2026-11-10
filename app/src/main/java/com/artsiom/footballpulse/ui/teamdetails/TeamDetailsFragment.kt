package com.artsiom.footballpulse.ui.teamdetails

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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
import coil.load
import com.artsiom.footballpulse.FootballPulseApp
import com.artsiom.footballpulse.R
import kotlinx.coroutines.launch

class TeamDetailsFragment : Fragment() {

    companion object {
        private const val ARG_TEAM_ID = "team_id"

        fun newInstance(teamId: Int): TeamDetailsFragment {
            val fragment = TeamDetailsFragment()
            fragment.arguments = Bundle().apply { putInt(ARG_TEAM_ID, teamId) }
            return fragment
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_team_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val teamId = requireArguments().getInt(ARG_TEAM_ID)
        val repository = (requireActivity().application as FootballPulseApp).container.repository
        val factory = viewModelFactory {
            initializer { TeamDetailsViewModel(repository, teamId) }
        }
        val viewModel = ViewModelProvider(this, factory)[TeamDetailsViewModel::class.java]

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val errorLayout = view.findViewById<View>(R.id.errorLayout)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        val retryButton = view.findViewById<Button>(R.id.retryButton)
        val contentLayout = view.findViewById<View>(R.id.contentLayout)
        val btnBack = view.findViewById<TextView>(R.id.btnBack)

        val binding = TeamDetailsViews(view)

        btnBack.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        retryButton.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TeamDetailsUiState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                            errorLayout.visibility = View.GONE
                            contentLayout.visibility = View.GONE
                        }
                        is TeamDetailsUiState.Error -> {
                            progressBar.visibility = View.GONE
                            errorLayout.visibility = View.VISIBLE
                            contentLayout.visibility = View.GONE
                            errorText.text = state.message
                        }
                        is TeamDetailsUiState.Success -> {
                            progressBar.visibility = View.GONE
                            errorLayout.visibility = View.GONE
                            contentLayout.visibility = View.VISIBLE
                            bindTeam(state.content, binding)
                        }
                    }
                }
            }
        }
    }

    private fun bindTeam(model: TeamDetailsUiModel, binding: TeamDetailsViews) = with(binding) {
        val team = model.team
        crestView.load(team.crest) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder_crest)
            error(R.drawable.ic_placeholder_crest)
        }

        nameView.text = team.name

        shortNameView.text = model.shortLabel
        shortNameView.visibility = if (model.shortLabel.isBlank()) View.GONE else View.VISIBLE

        populateInfoRows(infoContainer, model.infoRows)
        infoCard.visibility = View.VISIBLE

        if (model.showSquad) {
            squadCard.visibility = View.VISIBLE
            populateSquad(squadContainer, model.squadGroups)
        } else {
            squadCard.visibility = View.GONE
        }
    }

    private fun populateInfoRows(container: LinearLayout, rows: List<InfoRowUiModel>) {
        container.removeAllViews()
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density
        val hPad = (12 * density).toInt()
        val vPad = (10 * density).toInt()

        rows.forEachIndexed { index, item ->
            val label = getString(item.label)
            val value = item.value
            if (index > 0) {
                val divider = View(ctx)
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                divider.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_divider))
                container.addView(divider)
            }
            container.addView(buildInfoRow(ctx, hPad, vPad, label, value))
        }
    }

    private fun buildInfoRow(
        ctx: android.content.Context,
        hPad: Int,
        vPad: Int,
        label: String,
        value: String
    ): LinearLayout {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(hPad, vPad, hPad, vPad)

        val labelTv = TextView(ctx)
        labelTv.text = label
        labelTv.textSize = 13f
        labelTv.setTextColor(ContextCompat.getColor(ctx, R.color.color_text_hint))
        labelTv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(labelTv)

        val valueTv = TextView(ctx)
        valueTv.text = value
        valueTv.textSize = 14f
        valueTv.setTextColor(ContextCompat.getColor(ctx, R.color.color_text_primary))
        valueTv.gravity = Gravity.END
        row.addView(valueTv)

        return row
    }

    private fun populateSquad(container: LinearLayout, groups: List<SquadGroupUiModel>) {
        container.removeAllViews()
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density
        val hPad = (12 * density).toInt()
        val vPad = (8 * density).toInt()

        groups.forEach { group ->
            // Position group header
            val headerTv = TextView(ctx)
            headerTv.setText(group.label)
            headerTv.textSize = 12f
            headerTv.setTypeface(null, Typeface.BOLD)
            headerTv.setTextColor(ContextCompat.getColor(ctx, R.color.color_text_secondary))
            headerTv.setPadding(hPad, (12 * density).toInt(), hPad, (4 * density).toInt())
            container.addView(headerTv)

            val divider = View(ctx)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            divider.setBackgroundColor(ContextCompat.getColor(ctx, R.color.color_divider))
            container.addView(divider)

            group.players.forEach { player ->
                container.addView(buildPlayerRow(ctx, hPad, vPad, player))
            }
        }

    }

    private fun buildPlayerRow(
        ctx: android.content.Context,
        hPad: Int,
        vPad: Int,
        player: SquadPlayerUiModel
    ): LinearLayout {
        val density = ctx.resources.displayMetrics.density
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(hPad, vPad, hPad, vPad)

        val numberTv = TextView(ctx)
        numberTv.text = player.number
        numberTv.textSize = 13f
        numberTv.setTextColor(ContextCompat.getColor(ctx, R.color.color_text_hint))
        numberTv.gravity = Gravity.CENTER
        numberTv.minWidth = (28 * density).toInt()
        numberTv.layoutParams = LinearLayout.LayoutParams(
            (32 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT
        )
        row.addView(numberTv)

        val nameTv = TextView(ctx)
        nameTv.text = player.name
        nameTv.textSize = 14f
        nameTv.setTextColor(ContextCompat.getColor(ctx, R.color.color_text_primary))
        nameTv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(nameTv)

        val nationalityTv = TextView(ctx)
        nationalityTv.text = player.nationality
        nationalityTv.textSize = 12f
        nationalityTv.setTextColor(ContextCompat.getColor(ctx, R.color.color_text_hint))
        nationalityTv.gravity = Gravity.END
        row.addView(nationalityTv)

        val age = player.age
        if (age != null) {
            val ageTv = TextView(ctx)
            ageTv.text = age
            ageTv.textSize = 13f
            ageTv.setTextColor(ContextCompat.getColor(ctx, R.color.color_text_hint))
            ageTv.gravity = Gravity.END
            ageTv.setPadding((8 * density).toInt(), 0, 0, 0)
            row.addView(ageTv)
        }

        return row
    }


    private class TeamDetailsViews(view: View) {
        val crestView = view.findViewById<ImageView>(R.id.teamCrest)
        val nameView = view.findViewById<TextView>(R.id.teamName)
        val shortNameView = view.findViewById<TextView>(R.id.teamShortName)
        val infoContainer = view.findViewById<LinearLayout>(R.id.infoContainer)
        val infoCard = view.findViewById<View>(R.id.infoCard)
        val squadCard = view.findViewById<View>(R.id.squadCard)
        val squadContainer = view.findViewById<LinearLayout>(R.id.squadContainer)
    }
}
