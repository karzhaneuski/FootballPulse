package com.artsiom.footballpulse.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.League
import com.artsiom.footballpulse.domain.model.LeagueZone
import com.artsiom.footballpulse.ui.common.ErrorState
import com.artsiom.footballpulse.ui.common.LeagueSelector
import com.artsiom.footballpulse.ui.common.LoadingState
import com.artsiom.footballpulse.ui.matchdetails.matchDetailsAppearance
import com.artsiom.footballpulse.ui.matchdetails.matchTextStyle

@Composable
fun StandingsScreen(
    state: StandingsUiState,
    leagues: List<League>,
    selectedLeagueCode: String,
    mode: ViewMode,
    actions: StandingsActions
) {
    val appearance = matchDetailsAppearance()
    val typography = MaterialTheme.typography.copy(
        bodyLarge = matchTextStyle(14, appearance.colors.onSurfaceVariant, alignment = TextAlign.Center),
        labelLarge = matchTextStyle(14, appearance.colors.onPrimary, FontWeight.Medium, TextAlign.Center)
            .copy(letterSpacing = 0.1.sp, lineHeight = 20.sp)
    )
    // Keep scroll position while a league reload temporarily hides the table, as RecyclerView did.
    val tableState = rememberLazyListState()
    MaterialTheme(colorScheme = appearance.colors, typography = typography) {
        Surface(Modifier.fillMaxSize(), color = appearance.colors.background) {
            Column {
                Surface(Modifier.zIndex(2f), color = appearance.colors.surface, shadowElevation = 4.dp) {
                    LeagueSelector(leagues, selectedLeagueCode, actions.onLeagueSelected, Modifier.fillMaxWidth())
                }
                Surface(Modifier.zIndex(1f), color = appearance.colors.surface, shadowElevation = 2.dp) {
                    StandingsModeSelector(mode, actions.onModeSelected)
                }
                StandingsLegend()
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when (state) {
                        StandingsUiState.Loading -> LoadingState()
                        is StandingsUiState.Error -> ErrorState(
                            state.message, actions.onRetry, retryButtonMinWidth = 88.dp, spacing = 12.dp
                        )
                        is StandingsUiState.Success -> LazyColumn(state = tableState) {
                            item(key = "header") { StandingsTableHeader(mode) }
                            items(state.rows, key = { it.standing.teamId }) { row ->
                                StandingsTableRow(row, mode, actions.onTeamClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsModeSelector(mode: ViewMode, onModeSelected: (ViewMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp, vertical = 8.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(ViewMode.FULL to R.string.standings_full, ViewMode.SHORT to R.string.standings_short,
            ViewMode.FORM to R.string.standings_form).forEach { (item, label) ->
            val selected = item == mode
            Box(
                Modifier.height(32.dp)
                    .background(
                        colorResource(if (selected) R.color.color_tab_selected_bg else R.color.color_tab_unselected_bg),
                        RoundedCornerShape(20.dp)
                    )
                    .selectable(selected, role = Role.Tab, onClick = { onModeSelected(item) })
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    stringResource(label),
                    style = matchTextStyle(13, colorResource(
                        if (selected) R.color.color_tab_selected_text else R.color.color_tab_unselected_text
                    ), FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun StandingsLegend() {
    val entries = listOf(
        LeagueZone.UCL to R.string.zone_ucl, LeagueZone.UEL to R.string.zone_uel,
        LeagueZone.UECL to R.string.zone_uecl, LeagueZone.RELEGATION to R.string.zone_relegation,
        LeagueZone.PLAYOFF to R.string.zone_playoff
    )
    val text = buildAnnotatedString {
        entries.forEachIndexed { index, (zone, label) ->
            if (index > 0) append(stringResource(R.string.standings_legend_separator))
            withStyle(SpanStyle(color = standingsZoneColor(zone))) {
                append(stringResource(R.string.standings_legend_marker))
            }
            append(stringResource(label))
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        val style = matchTextStyle(11, colorResource(R.color.color_text_secondary))
        BasicText(text, Modifier.fillMaxWidth(), style = style)
        BasicText(stringResource(R.string.standings_places_note), Modifier.fillMaxWidth(), style = style)
    }
}

@Composable
internal fun standingsZoneColor(zone: LeagueZone): Color = when (zone) {
    LeagueZone.UCL -> colorResource(R.color.color_standings_cl_border)
    LeagueZone.UEL -> colorResource(R.color.color_standings_el_border)
    LeagueZone.UECL -> colorResource(R.color.color_form_win_bg)
    LeagueZone.RELEGATION -> colorResource(R.color.color_standings_rel_border)
    LeagueZone.PLAYOFF -> colorResource(R.color.color_form_draw_bg)
    LeagueZone.NONE -> Color.Transparent
}
