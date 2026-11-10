package com.artsiom.footballpulse.ui.teamdetails

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.SquadPlayer
import com.artsiom.footballpulse.domain.model.TeamDetails
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme

@Preview(name = "Loading light", showBackground = true)
@Composable
internal fun TeamDetailsLoadingPreview() {
    FootballPulseTheme(darkTheme = false) { TeamDetailsScreen(TeamDetailsUiState.Loading, {}, {}) }
}

@Preview(name = "Loading dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun TeamDetailsLoadingDarkPreview() {
    FootballPulseTheme(darkTheme = true) { TeamDetailsScreen(TeamDetailsUiState.Loading, {}, {}) }
}

@Preview(name = "Error light", showBackground = true)
@Composable
internal fun TeamDetailsErrorPreview() {
    FootballPulseTheme(darkTheme = false) {
        TeamDetailsScreen(TeamDetailsUiState.Error(stringResource(R.string.team_details_preview_error)), {}, {})
    }
}

@Preview(name = "Error dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun TeamDetailsErrorDarkPreview() {
    FootballPulseTheme(darkTheme = true) {
        TeamDetailsScreen(TeamDetailsUiState.Error(stringResource(R.string.team_details_preview_error)), {}, {})
    }
}

@Preview(name = "Success light", showBackground = true)
@Composable
internal fun TeamDetailsSuccessPreview() {
    FootballPulseTheme(darkTheme = false) { TeamDetailsScreen(previewTeamState(), {}, {}) }
}

@Preview(name = "Success dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun TeamDetailsSuccessDarkPreview() {
    FootballPulseTheme(darkTheme = true) { TeamDetailsScreen(previewTeamState(), {}, {}) }
}

@Composable
private fun previewTeamState() = TeamDetailsUiState.Success(
    TeamDetails(
        id = 7, name = "Fixture United", shortName = "Fixture", tla = "FIX", crest = null,
        founded = 1900, venue = "Fixture Stadium", clubColors = "Blue / White",
        squad = listOf(
            SquadPlayer(1, "First Keeper", "Goalkeeper", "Poland", 1, "1990-01-01"),
            SquadPlayer(2, "Fixture Defender", "Centre-Back", "France", 4, null),
            SquadPlayer(3, "Fixture Midfielder", "Central Midfield", "Spain", 8, "2000-01-01"),
            SquadPlayer(4, "Fixture Forward", "Centre-Forward", "Brazil", 9, "2000-01-01")
        )
    )
)
