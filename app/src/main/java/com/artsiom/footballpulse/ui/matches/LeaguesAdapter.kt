package com.artsiom.footballpulse.ui.matches

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.League

class LeaguesAdapter(private val onLeagueClick: (League) -> Unit) : RecyclerView.Adapter<LeaguesAdapter.LeaguesViewHolder>() {

    class LeaguesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val leagueName: TextView = view.findViewById(R.id.leagueName)
    }

    private var leagues: List<League> = emptyList()
    private var selectedPosition: Int = 0

    fun submitList(list: List<League>) {
        leagues = list
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        if (position == selectedPosition) return
        val previous = selectedPosition
        selectedPosition = position
        notifyItemChanged(previous)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaguesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_league, parent, false)
        return LeaguesViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaguesViewHolder, position: Int) {
        val league = leagues[position]
        holder.leagueName.text = league.shortName

        val context = holder.itemView.context
        val density = context.resources.displayMetrics.density
        val bgDrawable = GradientDrawable().apply {
            cornerRadius = 20f * density
        }

        if (position == selectedPosition) {
            bgDrawable.setColor(ContextCompat.getColor(context, R.color.color_tab_selected_bg))
            holder.leagueName.setTextColor(ContextCompat.getColor(context, R.color.color_tab_selected_text))
        } else {
            bgDrawable.setColor(ContextCompat.getColor(context, R.color.color_tab_unselected_bg))
            holder.leagueName.setTextColor(ContextCompat.getColor(context, R.color.color_tab_unselected_text))
        }
        holder.leagueName.background = bgDrawable

        holder.leagueName.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onLeagueClick(league)
        }
    }

    override fun getItemCount(): Int = leagues.size
}
