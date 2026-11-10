package com.artsiom.footballpulse.ui.matches

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.ui.presentation.MatchStatusStyle

class MatchesAdapter(private val onMatchClick: (Match) -> Unit = {}) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_MATCH_CARD = 1


    }

    private var items: List<MatchListItem> = emptyList()

    fun submitList(list: List<MatchListItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is MatchListItem.DateHeader -> VIEW_TYPE_DATE_HEADER
        is MatchListItem.MatchCard -> VIEW_TYPE_MATCH_CARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val view = inflater.inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_match, parent, false)
                MatchCardViewHolder(view, onMatchClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MatchListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is MatchListItem.MatchCard -> (holder as MatchCardViewHolder).bind(item.card)
        }
    }

    override fun getItemCount(): Int = items.size

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateText: TextView = view.findViewById(R.id.dateHeaderText)

        fun bind(header: MatchListItem.DateHeader) {
            dateText.text = header.dateLabel ?: itemView.context.getString(R.string.unknown_date)
        }
    }

    class MatchCardViewHolder(view: View, private val onMatchClick: (Match) -> Unit) : RecyclerView.ViewHolder(view) {
        private val homeTeam: TextView = view.findViewById(R.id.homeTeam)
        private val awayTeam: TextView = view.findViewById(R.id.awayTeam)
        private val matchDate: TextView = view.findViewById(R.id.matchDate)
        private val matchScore: TextView = view.findViewById(R.id.matchScore)
        private val matchStatus: TextView = view.findViewById(R.id.matchStatus)
        private val homeTeamCrest: ImageView = view.findViewById(R.id.homeTeamCrest)
        private val awayTeamCrest: ImageView = view.findViewById(R.id.awayTeamCrest)

        fun bind(card: MatchCardUiModel) {
            val match = card.match
            itemView.setOnClickListener { onMatchClick(match) }
            homeTeam.text = match.homeTeam
            awayTeam.text = match.awayTeam

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

            matchDate.text = card.timeLabel
            matchScore.text = card.scoreLabel

            matchStatus.text = match.status

            val context = itemView.context
            val density = context.resources.displayMetrics.density
            val bgDrawable = GradientDrawable().apply {
                cornerRadius = 4f * density
            }

            when (card.statusStyle) {
                MatchStatusStyle.FINISHED -> {
                    bgDrawable.setColor(ContextCompat.getColor(context, R.color.color_status_finished_bg))
                    matchStatus.setTextColor(ContextCompat.getColor(context, R.color.color_status_finished_text))
                }
                MatchStatusStyle.LIVE -> {
                    bgDrawable.setColor(ContextCompat.getColor(context, R.color.color_status_live_bg))
                    matchStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                MatchStatusStyle.SCHEDULED -> {
                    bgDrawable.setColor(ContextCompat.getColor(context, R.color.color_status_scheduled_bg))
                    matchStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }
            matchStatus.background = bgDrawable
        }
    }
}
