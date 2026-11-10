package com.artsiom.footballpulse.ui.standings

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.LeagueZone
import com.artsiom.footballpulse.ui.presentation.FormResult

class StandingsAdapter(
    private val onTeamClick: (teamId: Int) -> Unit = {}
) : RecyclerView.Adapter<StandingsAdapter.StandingViewHolder>() {

    private var standings: List<StandingUiModel> = emptyList()

    var mode: ViewMode = ViewMode.FULL
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun submitList(list: List<StandingUiModel>) {
        standings = list
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }

    override fun getItemViewType(position: Int) = if (position == 0) TYPE_HEADER else TYPE_ROW

    // +1 for the sticky header row at position 0
    override fun getItemCount() = standings.size + 1

    inner class StandingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val leftBorder: View = view.findViewById(R.id.leftBorder)
        val tvPosition: TextView = view.findViewById(R.id.tvPosition)
        val imgTeamCrest: ImageView = view.findViewById(R.id.imgTeamCrest)
        val tvTeamName: TextView = view.findViewById(R.id.tvTeamName)
        val tvMp: TextView = view.findViewById(R.id.tvMp)
        val tvWins: TextView = view.findViewById(R.id.tvWins)
        val tvDraws: TextView = view.findViewById(R.id.tvDraws)
        val tvLosses: TextView = view.findViewById(R.id.tvLosses)
        val tvGoals: TextView = view.findViewById(R.id.tvGoals)
        val tvGD: TextView = view.findViewById(R.id.tvGD)
        val formContainer: LinearLayout = view.findViewById(R.id.formContainer)
        val tvPoints: TextView = view.findViewById(R.id.tvPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_standing, parent, false)
        return StandingViewHolder(view)
    }

    override fun onBindViewHolder(holder: StandingViewHolder, position: Int) {
        if (position == 0) bindHeader(holder) else bindRow(holder, standings[position - 1])
    }

    /** Apply column visibility based on current mode. */
    private fun applyModeVisibility(holder: StandingViewHolder) {
        when (mode) {
            ViewMode.FULL -> {
                holder.tvMp.visibility = View.VISIBLE
                holder.tvWins.visibility = View.VISIBLE
                holder.tvDraws.visibility = View.VISIBLE
                holder.tvLosses.visibility = View.VISIBLE
                holder.tvGoals.visibility = View.VISIBLE
                holder.tvGD.visibility = View.GONE
                holder.formContainer.visibility = View.GONE
            }
            ViewMode.SHORT -> {
                holder.tvMp.visibility = View.VISIBLE
                holder.tvWins.visibility = View.GONE
                holder.tvDraws.visibility = View.GONE
                holder.tvLosses.visibility = View.GONE
                holder.tvGoals.visibility = View.GONE
                holder.tvGD.visibility = View.VISIBLE
                holder.formContainer.visibility = View.GONE
            }
            ViewMode.FORM -> {
                holder.tvMp.visibility = View.GONE
                holder.tvWins.visibility = View.GONE
                holder.tvDraws.visibility = View.GONE
                holder.tvLosses.visibility = View.GONE
                holder.tvGoals.visibility = View.GONE
                holder.tvGD.visibility = View.GONE
                holder.formContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun bindHeader(holder: StandingViewHolder) {
        val ctx = holder.itemView.context
        applyModeVisibility(holder)

        holder.itemView.setBackgroundColor(
            ContextCompat.getColor(ctx, R.color.color_standings_header_bg)
        )
        holder.leftBorder.setBackgroundColor(Color.TRANSPARENT)

        val headerColor = ContextCompat.getColor(ctx, R.color.color_date_header_text)

        holder.imgTeamCrest.visibility = View.INVISIBLE
        holder.tvPosition.apply { text = "#"; setTextColor(headerColor); setTypeface(null, Typeface.NORMAL) }
        holder.tvTeamName.apply { text = "Team"; setTextColor(headerColor); setTypeface(null, Typeface.NORMAL) }
        holder.tvMp.apply { text = "MP"; setTextColor(headerColor) }
        holder.tvWins.apply { text = "W"; setTextColor(headerColor) }
        holder.tvDraws.apply { text = "D"; setTextColor(headerColor) }
        holder.tvLosses.apply { text = "L"; setTextColor(headerColor) }
        holder.tvGoals.apply { text = "GF:GA"; setTextColor(headerColor) }
        holder.tvGD.apply { text = "GD"; setTextColor(headerColor) }
        holder.tvPoints.apply { text = "Pts"; setTextColor(headerColor); setTypeface(null, Typeface.NORMAL) }

        if (mode == ViewMode.FORM) {
            holder.formContainer.removeAllViews()
            val label = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "Form"
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(headerColor)
            }
            holder.formContainer.addView(label)
        }
    }

    private fun bindRow(holder: StandingViewHolder, row: StandingUiModel) {
        val standing = row.standing
        val ctx = holder.itemView.context
        applyModeVisibility(holder)
        holder.itemView.setOnClickListener { onTeamClick(standing.teamId) }

        // Alternating row background
        val rowBg = if (standing.position % 2 == 0) {
            ContextCompat.getColor(ctx, R.color.color_standings_row_odd)
        } else {
            ContextCompat.getColor(ctx, R.color.color_standings_row_even)
        }
        holder.itemView.setBackgroundColor(rowBg)

        // Left border by competition zone
        val borderColor = standingsZoneColor(ctx, row.zone)
        holder.leftBorder.setBackgroundColor(borderColor)

        val defaultColor = ContextCompat.getColor(ctx, R.color.color_text_primary)

        holder.tvPosition.apply { text = row.position; setTextColor(defaultColor); setTypeface(null, Typeface.NORMAL) }
        holder.imgTeamCrest.visibility = View.VISIBLE
        holder.imgTeamCrest.load(standing.crest) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder_crest)
            error(R.drawable.ic_placeholder_crest)
        }
        holder.tvTeamName.apply { text = standing.teamName; setTextColor(defaultColor); setTypeface(null, Typeface.NORMAL) }
        holder.tvPoints.apply { text = row.points; setTextColor(defaultColor); setTypeface(null, Typeface.BOLD) }

        bindStatistics(holder, row, defaultColor)
    }

    private fun bindStatistics(holder: StandingViewHolder, row: StandingUiModel, defaultColor: Int) {
        when (mode) {
            ViewMode.FULL -> {
                holder.tvMp.apply { text = row.playedGames; setTextColor(defaultColor) }
                holder.tvWins.apply { text = row.won; setTextColor(defaultColor) }
                holder.tvDraws.apply { text = row.draw; setTextColor(defaultColor) }
                holder.tvLosses.apply { text = row.lost; setTextColor(defaultColor) }
                holder.tvGoals.apply {
                    text = row.goals
                    setTextColor(defaultColor)
                }
            }
            ViewMode.SHORT -> {
                holder.tvMp.apply { text = row.playedGames; setTextColor(defaultColor) }
                holder.tvGD.apply {
                    text = row.goalDifference
                    setTextColor(defaultColor)
                }
            }
            ViewMode.FORM -> {
                buildFormBadges(holder, row.form)
            }
        }
    }

    private fun buildFormBadges(holder: StandingViewHolder, results: List<FormResult?>) {
        val ctx = holder.itemView.context
        holder.formContainer.removeAllViews()

        val density = ctx.resources.displayMetrics.density
        val badgeSize = (28 * density).toInt()
        val badgeGap = (3 * density).toInt()
        // 14dp = half of 28dp badge → fully circular. Named distinctly to avoid
        // shadowing GradientDrawable.cornerRadius inside the apply{} block below.
        val badgeCornerRadius = 14f * density

        results.forEachIndexed { index, result ->
            val params = LinearLayout.LayoutParams(badgeSize, badgeSize).apply {
                if (index < results.lastIndex) marginEnd = badgeGap
            }
            val badge = TextView(ctx).apply {
                layoutParams = params
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
            }

            if (result == null) {
                badge.visibility = View.INVISIBLE
            } else {
                val bgColor = when (result) {
                    FormResult.WIN -> ContextCompat.getColor(ctx, R.color.color_form_win_bg)
                    FormResult.DRAW -> ContextCompat.getColor(ctx, R.color.color_form_draw_bg)
                    else -> ContextCompat.getColor(ctx, R.color.color_form_loss_bg)
                }
                badge.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    // Use badgeCornerRadius (outer local) — NOT GradientDrawable.cornerRadius,
                    // which would cause a self-assignment (0 = 0) and skip rounding entirely.
                    cornerRadius = badgeCornerRadius
                    setColor(bgColor)
                }
                badge.text = result.label
            }

            holder.formContainer.addView(badge)
        }
    }
}

internal fun standingsZoneColor(ctx: android.content.Context, zone: LeagueZone): Int = when (zone) {
    LeagueZone.UCL -> ContextCompat.getColor(ctx, R.color.color_standings_cl_border)
    LeagueZone.UEL -> ContextCompat.getColor(ctx, R.color.color_standings_el_border)
    LeagueZone.PLAYOFF -> ContextCompat.getColor(ctx, R.color.color_form_draw_bg)
    LeagueZone.UECL -> ContextCompat.getColor(ctx, R.color.color_form_win_bg)
    LeagueZone.RELEGATION -> ContextCompat.getColor(ctx, R.color.color_standings_rel_border)
    LeagueZone.NONE -> Color.TRANSPARENT
}
