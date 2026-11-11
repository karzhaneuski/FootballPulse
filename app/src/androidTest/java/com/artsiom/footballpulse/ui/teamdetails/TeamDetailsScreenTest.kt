package com.artsiom.footballpulse.ui.teamdetails

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.SquadPlayer
import com.artsiom.footballpulse.domain.model.TeamDetails
import com.artsiom.footballpulse.ui.onLoadingIndicator
import com.artsiom.footballpulse.ui.string
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TeamDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var retryClicks = 0
    private var backClicks = 0

    private fun setScreen(state: TeamDetailsUiState) {
        composeRule.setContent {
            FootballPulseTheme(darkTheme = false) {
                TeamDetailsScreen(state, onRetry = { retryClicks++ }, onBack = { backClicks++ })
            }
        }
    }

    @Test
    fun loading_showsProgress() {
        setScreen(TeamDetailsUiState.Loading)

        composeRule.onLoadingIndicator().assertIsDisplayed()
    }

    @Test
    fun success_showsHeaderAndClubInfo() {
        setScreen(TeamDetailsUiState.Success(SAMPLE_TEAM))

        composeRule.onNodeWithText("Fixture United").assertIsDisplayed()
        composeRule.onNodeWithText("Fixture (FIX)").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.team_details_club_info)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.founded)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1900").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fixture Stadium").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun success_showsSquadGroupedByPosition() {
        setScreen(TeamDetailsUiState.Success(SAMPLE_TEAM))

        composeRule.onNodeWithText(string(R.string.team_details_squad)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.goalkeepers)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("First Keeper").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.forwards)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fixture Forward").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun success_withoutSquad_hidesSquadCard() {
        setScreen(TeamDetailsUiState.Success(SAMPLE_TEAM.copy(squad = emptyList())))

        composeRule.onNodeWithText("Fixture United").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.team_details_squad)).assertDoesNotExist()
    }

    @Test
    fun success_backInvokesCallback() {
        setScreen(TeamDetailsUiState.Success(SAMPLE_TEAM))

        composeRule.onNodeWithText(string(R.string.team_details_back)).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun error_showsMessage_andRetryInvokesCallback() {
        setScreen(TeamDetailsUiState.Error(ERROR_MESSAGE))

        composeRule.onNodeWithText(ERROR_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()

        assertEquals(1, retryClicks)
        assertEquals(0, backClicks)
    }

    private companion object {
        const val ERROR_MESSAGE = "Unable to load team"

        val SAMPLE_TEAM = TeamDetails(
            id = 7, name = "Fixture United", shortName = "Fixture", tla = "FIX", crest = null,
            founded = 1900, venue = "Fixture Stadium", clubColors = "Blue / White",
            squad = listOf(
                SquadPlayer(1, "First Keeper", "Goalkeeper", "Poland", 1, "1990-01-01"),
                SquadPlayer(2, "Fixture Defender", "Centre-Back", "France", 4, null),
                SquadPlayer(4, "Fixture Forward", "Centre-Forward", "Brazil", 9, "2000-01-01")
            )
        )
    }
}
