package com.artsiom.footballpulse.ui.matches

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.Leagues
import com.artsiom.footballpulse.domain.model.Match
import com.artsiom.footballpulse.ui.CallRecorder
import com.artsiom.footballpulse.ui.onLoadingIndicator
import com.artsiom.footballpulse.ui.string
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val leagueSelections = CallRecorder<String>()
    private val matchClicks = CallRecorder<Int>()
    private var previousClicks = 0
    private var nextClicks = 0
    private var retryClicks = 0

    private val actions = MatchesActions(
        onLeagueSelected = { leagueSelections(it) },
        onPreviousMatchday = { previousClicks++ },
        onNextMatchday = { nextClicks++ },
        onRetry = { retryClicks++ },
        onMatchClick = { matchClicks(it) }
    )

    private fun setScreen(
        state: MatchesUiState,
        navigation: MatchdayNavigation = MatchdayNavigation(MATCHDAY, previousEnabled = true, nextEnabled = true)
    ) {
        composeRule.setContent {
            FootballPulseTheme(darkTheme = false) {
                MatchesScreen(state, Leagues.ALL, Leagues.DEFAULT.code, navigation, actions)
            }
        }
    }

    @Test
    fun loading_showsProgressAndMatchdayHeader() {
        setScreen(MatchesUiState.Loading)

        composeRule.onLoadingIndicator().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.matchday_format, MATCHDAY)).assertIsDisplayed()
    }

    @Test
    fun success_showsMatchCards() {
        setScreen(MatchesUiState.Success(SAMPLE_MATCHES))

        composeRule.onNodeWithText("Arsenal", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Chelsea", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("2 - 1", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Everton", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("- : -", substring = true).assertIsDisplayed()
    }

    @Test
    fun success_clickOnMatchCard_reportsMatchId() {
        setScreen(MatchesUiState.Success(SAMPLE_MATCHES))

        composeRule.onNodeWithText("Everton", substring = true).performClick()

        assertEquals(listOf(SCHEDULED_MATCH_ID), matchClicks.calls)
    }

    @Test
    fun error_showsMessage_andRetryInvokesCallback() {
        setScreen(MatchesUiState.Error(ERROR_MESSAGE))

        composeRule.onNodeWithText(ERROR_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()

        assertEquals(1, retryClicks)
    }

    @Test
    fun matchdayArrows_invokeNavigationCallbacks() {
        setScreen(MatchesUiState.Success(SAMPLE_MATCHES))

        composeRule.onNodeWithContentDescription(string(R.string.matches_previous_matchday)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.matches_next_matchday)).performClick()

        assertEquals(1, previousClicks)
        assertEquals(1, nextClicks)
    }

    @Test
    fun disabledMatchdayArrows_doNotInvokeCallbacks() {
        setScreen(
            MatchesUiState.Success(SAMPLE_MATCHES),
            MatchdayNavigation(1, previousEnabled = false, nextEnabled = false)
        )

        composeRule.onNodeWithContentDescription(string(R.string.matches_previous_matchday))
            .assertIsNotEnabled()
            .performClick()
        composeRule.onNodeWithContentDescription(string(R.string.matches_next_matchday))
            .assertIsNotEnabled()
            .performClick()

        assertEquals(0, previousClicks)
        assertEquals(0, nextClicks)
    }

    @Test
    fun leagueTab_click_reportsLeagueCode() {
        setScreen(MatchesUiState.Loading)
        val laLiga = Leagues.forCode("PD")

        composeRule.onNodeWithText(laLiga.shortName).performClick()

        assertEquals(listOf(laLiga.code), leagueSelections.calls)
    }

    private companion object {
        const val MATCHDAY = 7
        const val SCHEDULED_MATCH_ID = 102
        const val ERROR_MESSAGE = "Unable to load matches"

        val SAMPLE_MATCHES = listOf(
            Match(101, "Arsenal", "Chelsea", 2, 1, "2025-08-10T12:00:00Z", "FINISHED", null, null),
            Match(SCHEDULED_MATCH_ID, "Everton", "Fulham", null, null, "2025-08-10T16:30:00Z", "SCHEDULED", null, null)
        )
    }
}
