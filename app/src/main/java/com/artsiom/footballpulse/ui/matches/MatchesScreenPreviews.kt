package com.artsiom.footballpulse.ui.matches

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.Leagues
import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme

@Preview(name = "Loading light", showBackground = true)
@Preview(name = "Loading dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun MatchesLoadingPreview() {
    MatchesPreview(MatchesUiState.Loading, MatchdayNavigation(1, false, false))
}

@Preview(name = "Error light", showBackground = true)
@Preview(name = "Error dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun MatchesErrorPreview() {
    MatchesPreview(MatchesUiState.Error(stringResource(R.string.matches_preview_error)))
}

@Preview(name = "Success light", showBackground = true)
@Preview(name = "Success dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun MatchesSuccessPreview() {
    MatchesPreview(MatchesUiState.Success(listOf(
        Match(1, "Home Club", "Away Club", 2, 1, "2025-08-10T12:00:00Z", "FINISHED", null, null),
        Match(2, "Home Club", "Away Club", 0, 0, "2025-08-10T14:00:00Z", "IN_PLAY", null, null),
        Match(3, "Home Club", "Away Club", null, null, "2025-08-11T18:30:00Z", "SCHEDULED", null, null)
    )))
}

@Preview(name = "Empty light", showBackground = true)
@Preview(name = "Empty dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun MatchesEmptyPreview() {
    MatchesPreview(MatchesUiState.Success(emptyList()))
}

@Composable
private fun MatchesPreview(state: MatchesUiState, navigation: MatchdayNavigation = MatchdayNavigation(2, true, true)) {
    FootballPulseTheme {
        MatchesScreen(state, Leagues.ALL, Leagues.DEFAULT.code, navigation, MatchesActions({}, {}, {}, {}, {}))
    }
}
