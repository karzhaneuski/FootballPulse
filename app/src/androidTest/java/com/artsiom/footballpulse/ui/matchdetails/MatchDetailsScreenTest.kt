package com.artsiom.footballpulse.ui.matchdetails

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.MatchDetail
import com.artsiom.footballpulse.ui.onLoadingIndicator
import com.artsiom.footballpulse.ui.string
import com.artsiom.footballpulse.ui.theme.FootballPulseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var retryClicks = 0
    private var backClicks = 0

    private fun setScreen(state: MatchDetailsUiState) {
        composeRule.setContent {
            FootballPulseTheme(darkTheme = false) {
                MatchDetailsScreen(state, onRetry = { retryClicks++ }, onBack = { backClicks++ })
            }
        }
    }

    @Test
    fun loading_showsProgress() {
        setScreen(MatchDetailsUiState.Loading)

        composeRule.onLoadingIndicator().assertIsDisplayed()
    }

    @Test
    fun success_finishedMatch_showsTeamsScoreAndBreakdown() {
        setScreen(MatchDetailsUiState.Success(FINISHED_MATCH))

        composeRule.onNodeWithText("Arsenal").assertIsDisplayed()
        composeRule.onNodeWithText("Chelsea").assertIsDisplayed()
        composeRule.onNodeWithText("2 - 1").assertIsDisplayed()
        composeRule.onNodeWithText("FINISHED").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.match_details_score)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.first_half)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.full_time)).assertIsDisplayed()
    }

    @Test
    fun success_scheduledMatch_showsNotStartedMessage() {
        setScreen(MatchDetailsUiState.Success(SCHEDULED_MATCH))

        composeRule.onNodeWithText(string(R.string.match_not_started)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.full_time)).assertDoesNotExist()
    }

    @Test
    fun success_backInvokesCallback() {
        setScreen(MatchDetailsUiState.Success(FINISHED_MATCH))

        composeRule.onNodeWithText(string(R.string.match_details_back)).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun error_showsMessage_andRetryInvokesCallback() {
        setScreen(MatchDetailsUiState.Error(ERROR_MESSAGE))

        composeRule.onNodeWithText(ERROR_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()

        assertEquals(1, retryClicks)
        assertEquals(0, backClicks)
    }

    private companion object {
        const val ERROR_MESSAGE = "Unable to load match"

        val FINISHED_MATCH = MatchDetail(
            42, "Arsenal", "Chelsea", 2, 1, "2025-08-10T12:00:00Z", "FINISHED",
            halfTimeHome = 1, halfTimeAway = 0, fullTimeHome = 2, fullTimeAway = 1,
            homeTeamCrest = null, awayTeamCrest = null
        )
        val SCHEDULED_MATCH = MatchDetail(
            43, "Everton", "Fulham", null, null, "2025-08-11T18:30:00Z", "SCHEDULED",
            halfTimeHome = null, halfTimeAway = null, fullTimeHome = null, fullTimeAway = null,
            homeTeamCrest = null, awayTeamCrest = null
        )
    }
}
