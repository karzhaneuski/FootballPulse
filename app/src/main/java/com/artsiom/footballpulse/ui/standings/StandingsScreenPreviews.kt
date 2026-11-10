package com.artsiom.footballpulse.ui.standings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.Leagues
import com.artsiom.footballpulse.domain.model.Standing
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme

@Preview(name = "Loading light", showBackground = true)
@Preview(name = "Loading dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun StandingsLoadingPreview() {
    StandingsPreview(StandingsUiState.Loading)
}

@Preview(name = "Error light", showBackground = true)
@Preview(name = "Error dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun StandingsErrorPreview() {
    StandingsPreview(StandingsUiState.Error(stringResource(R.string.standings_preview_error)))
}

@Preview(name = "Full light", showBackground = true)
@Preview(name = "Full dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun StandingsSuccessPreview() {
    StandingsPreview(previewStandingsState())
}

@Preview(name = "Short light", showBackground = true)
@Preview(name = "Short dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun StandingsShortPreview() {
    StandingsPreview(previewStandingsState(), ViewMode.SHORT)
}

@Preview(name = "Form light", showBackground = true)
@Preview(name = "Form dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun StandingsFormPreview() {
    StandingsPreview(previewStandingsState(), ViewMode.FORM)
}

@Composable
private fun StandingsPreview(state: StandingsUiState, mode: ViewMode = ViewMode.FULL) {
    FootballPulseTheme {
        StandingsScreen(state, Leagues.ALL, Leagues.DEFAULT.code, mode, StandingsActions({}, {}, {}, {}))
    }
}

@Composable
private fun previewStandingsState() = StandingsUiState.Success(
    (1..20).map { rank ->
        Standing(rank, rank, "Fixture Club $rank", null, 10, 6, 2, 2, 20, 18, 8, 10,
            when (rank) { 1 -> "WWDLW"; 2 -> "W"; else -> null })
    }
)
