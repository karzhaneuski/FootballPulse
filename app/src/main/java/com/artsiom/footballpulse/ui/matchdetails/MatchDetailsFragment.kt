package com.artsiom.footballpulse.ui.matchdetails

import android.graphics.drawable.GradientDrawable
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
import com.artsiom.footballpulse.domain.model.MatchDetail
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

class MatchDetailsFragment : Fragment() {

    companion object {
        private const val ARG_MATCH_ID = "match_id"

        fun newInstance(matchId: Int): MatchDetailsFragment {
            val fragment = MatchDetailsFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_MATCH_ID, matchId)
            }
            return fragment
        }

        private val INPUT_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private val DATE_FORMAT = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_match_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val matchId = requireArguments().getInt(ARG_MATCH_ID)
        val repository = (requireActivity().application as FootballPulseApp).container.repository
        val factory = viewModelFactory {
            initializer { MatchDetailsViewModel(repository, matchId) }
        }
        val viewModel = ViewModelProvider(this, factory)[MatchDetailsViewModel::class.java]

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val errorLayout = view.findViewById<View>(R.id.errorLayout)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        val retryButton = view.findViewById<Button>(R.id.retryButton)
        val contentLayout = view.findViewById<View>(R.id.contentLayout)
        val btnBack = view.findViewById<TextView>(R.id.btnBack)

        val homeTeamName = view.findViewById<TextView>(R.id.homeTeamName)
        val awayTeamName = view.findViewById<TextView>(R.id.awayTeamName)
        val homeTeamCrest = view.findViewById<ImageView>(R.id.homeTeamCrest)
        val awayTeamCrest = view.findViewById<ImageView>(R.id.awayTeamCrest)
        val scoreOrTime = view.findViewById<TextView>(R.id.scoreOrTime)
        val matchDate = view.findViewById<TextView>(R.id.matchDate)
        val matchStatus = view.findViewById<TextView>(R.id.matchStatus)
        val scoreContainer = view.findViewById<LinearLayout>(R.id.scoreContainer)

        btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        retryButton.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MatchDetailsUiState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                            errorLayout.visibility = View.GONE
                            contentLayout.visibility = View.GONE
                        }
                        is MatchDetailsUiState.Error -> {
                            progressBar.visibility = View.GONE
                            errorLayout.visibility = View.VISIBLE
                            contentLayout.visibility = View.GONE
                            errorText.text = state.message
                        }
                        is MatchDetailsUiState.Success -> {
                            progressBar.visibility = View.GONE
                            errorLayout.visibility = View.GONE
                            contentLayout.visibility = View.VISIBLE
                            bindMatch(
                                state.match,
                                homeTeamName, awayTeamName, homeTeamCrest, awayTeamCrest,
                                scoreOrTime, matchDate, matchStatus,
                                scoreContainer
                            )
                        }
                    }
                }
            }
        }
    }

    private fun bindMatch(
        match: MatchDetail,
        homeTeamName: TextView,
        awayTeamName: TextView,
        homeTeamCrest: ImageView,
        awayTeamCrest: ImageView,
        scoreOrTime: TextView,
        matchDate: TextView,
        matchStatus: TextView,
        scoreContainer: LinearLayout
    ) {
        homeTeamName.text = match.homeTeam
        awayTeamName.text = match.awayTeam

        homeTeamCrest.load(match.homeTeamCrest) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder_crest)
            error(R.drawable.ic_placeholder_crest)
        }
        awayTeamCrest.load(match.awayTeamCrest) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder_crest)
            error(R.drawable.ic_placeholder_crest)
        }

        val parsedDate = try { INPUT_FORMAT.parse(match.date) } catch (_: Exception) { null }

        scoreOrTime.text = when {
            match.homeScore != null && match.awayScore != null ->
                "${match.homeScore} - ${match.awayScore}"
            parsedDate != null -> TIME_FORMAT.format(parsedDate)
            else -> "- : -"
        }

        matchDate.text = if (parsedDate != null) DATE_FORMAT.format(parsedDate) else match.date

        matchStatus.text = match.status
        val context = requireContext()
        val density = context.resources.displayMetrics.density
        val bgDrawable = GradientDrawable().apply { cornerRadius = 4f * density }
        when (match.status.uppercase()) {
            "FINISHED" -> {
                bgDrawable.setColor(ContextCompat.getColor(context, R.color.color_status_finished_bg))
                matchStatus.setTextColor(ContextCompat.getColor(context, R.color.color_status_finished_text))
            }
            "IN_PLAY", "LIVE", "PAUSED" -> {
                bgDrawable.setColor(ContextCompat.getColor(context, R.color.color_status_live_bg))
                matchStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
            else -> {
                bgDrawable.setColor(ContextCompat.getColor(context, R.color.color_status_scheduled_bg))
                matchStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
        }
        matchStatus.background = bgDrawable

        populateScoreRows(scoreContainer, match)
    }

    private fun populateScoreRows(container: LinearLayout, match: MatchDetail) {
        container.removeAllViews()
        val context = requireContext()
        val density = context.resources.displayMetrics.density
        val hPad = (12 * density).toInt()
        val vPad = (10 * density).toInt()

        val statusUpper = match.status.uppercase()
        if (statusUpper == "SCHEDULED" || statusUpper == "TIMED") {
            val tv = TextView(context)
            tv.text = "Match not started yet"
            tv.gravity = Gravity.CENTER
            tv.textSize = 13f
            tv.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, vPad, 0, vPad)
            tv.layoutParams = params
            container.addView(tv)
            return
        }

        val halfHome = match.halfTimeHome
        val halfAway = match.halfTimeAway
        if (halfHome != null && halfAway != null) {
            container.addView(buildScoreRow(context, density, hPad, vPad, "First Half", halfHome, halfAway))
            val divider = View(context)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.coerceAtLeast((0.5f * density).toInt())
            )
            divider.setBackgroundColor(ContextCompat.getColor(context, R.color.color_divider))
            container.addView(divider)
        }

        val ftHome = match.fullTimeHome
        val ftAway = match.fullTimeAway
        val homeDisplay = ftHome?.toString() ?: "-"
        val awayDisplay = ftAway?.toString() ?: "-"
        container.addView(buildScoreRow(context, density, hPad, vPad, "Full Time", homeDisplay, awayDisplay))
    }

    private fun buildScoreRow(
        context: android.content.Context,
        density: Float,
        hPad: Int,
        vPad: Int,
        label: String,
        homeScore: Any,
        awayScore: Any
    ): LinearLayout {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(hPad, vPad, hPad, vPad)

        val labelTv = TextView(context)
        labelTv.text = label
        labelTv.textSize = 13f
        labelTv.setTextColor(ContextCompat.getColor(context, R.color.color_text_hint))
        labelTv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(labelTv)

        val homeTv = TextView(context)
        homeTv.text = homeScore.toString()
        homeTv.textSize = 14f
        homeTv.setTypeface(null, android.graphics.Typeface.BOLD)
        homeTv.gravity = Gravity.END
        homeTv.minWidth = (28 * density).toInt()
        row.addView(homeTv)

        val dashTv = TextView(context)
        dashTv.text = "  -  "
        dashTv.textSize = 14f
        dashTv.gravity = Gravity.CENTER
        row.addView(dashTv)

        val awayTv = TextView(context)
        awayTv.text = awayScore.toString()
        awayTv.textSize = 14f
        awayTv.setTypeface(null, android.graphics.Typeface.BOLD)
        awayTv.gravity = Gravity.START
        awayTv.minWidth = (28 * density).toInt()
        row.addView(awayTv)

        return row
    }
}
