package com.artsiom.footballpulse.ui.standings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.Leagues
import com.artsiom.footballpulse.domain.model.Standing
import com.artsiom.footballpulse.ui.CallRecorder
import com.artsiom.footballpulse.ui.onLoadingIndicator
import com.artsiom.footballpulse.ui.string
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StandingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val leagueSelections = CallRecorder<String>()
    private val modeSelections = CallRecorder<ViewMode>()
    private val teamClicks = CallRecorder<Int>()
    private var retryClicks = 0

    private val actions = StandingsActions(
        onLeagueSelected = { leagueSelections(it) },
        onModeSelected = { modeSelections(it) },
        onRetry = { retryClicks++ },
        onTeamClick = { teamClicks(it) }
    )

    private fun setScreen(state: StandingsUiState, mode: ViewMode = ViewMode.FULL) {
        composeRule.setContent {
            FootballPulseTheme(darkTheme = false) {
                StandingsScreen(state, Leagues.ALL, Leagues.DEFAULT.code, mode, actions)
            }
        }
    }

    @Test
    fun loading_showsProgressAndModeSelector() {
        setScreen(StandingsUiState.Loading)

        composeRule.onLoadingIndicator().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.standings_full)).assertIsSelected()
    }

    @Test
    fun success_showsTableHeaderAndRows() {
        setScreen(StandingsUiState.Success(SAMPLE_STANDINGS))

        composeRule.onNodeWithText(string(R.string.standings_team)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.standings_points)).assertIsDisplayed()
        composeRule.onNodeWithText("Liverpool", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Arsenal", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("15:4", substring = true).assertIsDisplayed()
    }

    @Test
    fun success_shortMode_showsGoalDifferenceColumn() {
        setScreen(StandingsUiState.Success(SAMPLE_STANDINGS), ViewMode.SHORT)

        composeRule.onNodeWithText(string(R.string.standings_short)).assertIsSelected()
        composeRule.onNodeWithText(string(R.string.standings_goal_difference)).assertIsDisplayed()
        composeRule.onNodeWithText("+11", substring = true).assertIsDisplayed()
    }

    @Test
    fun success_clickOnRow_reportsTeamId() {
        setScreen(StandingsUiState.Success(SAMPLE_STANDINGS))

        composeRule.onNodeWithText("Arsenal", substring = true).performClick()

        assertEquals(listOf(ARSENAL_ID), teamClicks.calls)
    }

    @Test
    fun error_showsMessage_andRetryInvokesCallback() {
        setScreen(StandingsUiState.Error(ERROR_MESSAGE))

        composeRule.onNodeWithText(ERROR_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()

        assertEquals(1, retryClicks)
    }

    @Test
    fun modeTab_click_reportsMode() {
        setScreen(StandingsUiState.Loading)

        composeRule.onNodeWithText(string(R.string.standings_form)).performClick()

        assertEquals(listOf(ViewMode.FORM), modeSelections.calls)
    }

    @Test
    fun leagueTab_click_reportsLeagueCode() {
        setScreen(StandingsUiState.Loading)
        val serieA = Leagues.forCode("SA")

        composeRule.onNodeWithText(serieA.shortName).performClick()

        assertEquals(listOf(serieA.code), leagueSelections.calls)
    }

    private companion object {
        const val ARSENAL_ID = 57
        const val ERROR_MESSAGE = "Unable to load standings"

        val SAMPLE_STANDINGS = listOf(
            Standing(1, 64, "Liverpool", null, 6, 5, 1, 0, 16, 15, 4, 11, "WWDWW"),
            Standing(2, ARSENAL_ID, "Arsenal", null, 6, 4, 1, 1, 13, 12, 6, 6, "WLWDW")
        )
    }
}
