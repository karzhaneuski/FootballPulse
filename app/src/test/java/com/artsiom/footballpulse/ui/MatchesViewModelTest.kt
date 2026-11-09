package com.artsiom.footballpulse.ui

import com.artsiom.footballpulse.domain.model.MatchdayResult
import com.artsiom.footballpulse.testing.FakeFootballRepository
import com.artsiom.footballpulse.testing.ViewModelTest
import com.artsiom.footballpulse.testing.match
import com.artsiom.footballpulse.testing.record
import com.artsiom.footballpulse.ui.matches.MatchesUiState
import com.artsiom.footballpulse.ui.matches.MatchesViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchesViewModelTest : ViewModelTest() {
    private val repository = FakeFootballRepository()

    @Test
    fun `initial load publishes loading then matches and selected matchday`() = runTest {
        val vm = own(MatchesViewModel(repository))
        val states = record(vm.uiState)
        runCurrent()
        assertEquals(listOf("PL"), repository.leagues.single().arguments)
        repository.leagues.single().result.complete(MatchdayResult(8, 2025, listOf(match)))
        runCurrent()
        assertEquals(listOf(MatchesUiState.Loading, MatchesUiState.Success(listOf(match))), states)
        assertEquals(8, vm.currentMatchday.value)
    }

    @Test
    fun `failed league load publishes error and retry recovers the same league`() = runTest {
        val vm = own(MatchesViewModel(repository))
        vm.loadLeague("BL1")
        val states = record(vm.uiState)
        runCurrent()
        repository.leagues.single().result.completeExceptionally(IOException("Offline"))
        runCurrent()
        vm.retry()
        runCurrent()
        assertEquals(listOf("BL1"), repository.leagues.last().arguments)
        repository.leagues.last().result.complete(MatchdayResult(4, 2024, listOf(match)))
        runCurrent()
        assertEquals(listOf(MatchesUiState.Loading, MatchesUiState.Error("Offline"),
            MatchesUiState.Loading, MatchesUiState.Success(listOf(match))), states)
    }

    @Test
    fun `changing league cancels superseded load and only new result is shown`() = runTest {
        val vm = own(MatchesViewModel(repository))
        val states = record(vm.uiState)
        runCurrent()
        val old = repository.leagues.single()
        vm.loadLeague("PD")
        runCurrent()
        assertTrue(old.cancelled)
        assertTrue(old.job.isCancelled)
        assertEquals("PD", vm.currentLeagueCode.value)
        assertEquals(listOf("PD"), repository.leagues.last().arguments)
        repository.leagues.last().result.complete(MatchdayResult(7, 2025, listOf(match)))
        old.result.complete(MatchdayResult(20, 2024, emptyList()))
        runCurrent()
        assertEquals(MatchesUiState.Success(listOf(match)), vm.uiState.value)
        assertEquals(7, vm.currentMatchday.value)
        assertFalse(states.any { it is MatchesUiState.Error })
    }

    @Test
    fun `retry during league load cancels old request instead of starting a parallel load`() = runTest {
        val vm = own(MatchesViewModel(repository))
        val states = record(vm.uiState)
        runCurrent()
        val old = repository.leagues.single()
        vm.retry()
        runCurrent()
        assertTrue(old.cancelled)
        assertEquals(2, repository.leagues.size)
        repository.leagues.last().result.complete(MatchdayResult(3, 2025, listOf(match)))
        old.result.complete(MatchdayResult(9, 2025, emptyList()))
        runCurrent()
        assertEquals(3, vm.currentMatchday.value)
        assertEquals(listOf(MatchesUiState.Loading, MatchesUiState.Success(listOf(match))), states)
    }

    @Test
    fun `matchday navigation and retry retain league season and failed matchday`() = runTest {
        val vm = own(MatchesViewModel(repository))
        vm.loadLeague("BL1")
        runCurrent()
        repository.leagues.single().result.complete(MatchdayResult(17, 2024, listOf(match)))
        runCurrent()
        val states = record(vm.uiState)
        vm.goToNextMatchday()
        runCurrent()
        assertEquals(listOf("BL1", 18, 2024), repository.matchdays.single().arguments)
        repository.matchdays.single().result.completeExceptionally(IOException("Offline"))
        runCurrent()
        vm.retry()
        runCurrent()
        assertEquals(listOf("BL1", 18, 2024), repository.matchdays.last().arguments)
        repository.matchdays.last().result.complete(emptyList())
        runCurrent()
        assertEquals(listOf(MatchesUiState.Success(listOf(match)), MatchesUiState.Loading,
            MatchesUiState.Error("Offline"), MatchesUiState.Loading, MatchesUiState.Success(emptyList())), states)
        assertEquals(18, vm.currentMatchday.value)
    }

    @Test
    fun `matchday retry cancels superseded request and previous navigation uses same season`() = runTest {
        val vm = own(MatchesViewModel(repository))
        runCurrent()
        repository.leagues.single().result.complete(MatchdayResult(17, 2024, emptyList()))
        runCurrent()
        vm.goToPreviousMatchday()
        runCurrent()
        val old = repository.matchdays.single()
        assertEquals(listOf("PL", 16, 2024), old.arguments)
        vm.retry()
        runCurrent()
        assertTrue(old.cancelled)
        assertTrue(old.job.isCancelled)
        repository.matchdays.last().result.complete(listOf(match))
        old.result.complete(emptyList())
        runCurrent()
        assertEquals(MatchesUiState.Success(listOf(match)), vm.uiState.value)
    }

    @Test
    fun `matchday cancellation is rethrown without publishing an error`() = runTest {
        val vm = own(MatchesViewModel(repository))
        runCurrent()
        repository.leagues.single().result.complete(MatchdayResult(17, 2024, listOf(match)))
        runCurrent()
        val states = record(vm.uiState)
        vm.goToNextMatchday()
        runCurrent()
        val request = repository.matchdays.single()
        request.result.cancel(CancellationException("Cancelled"))
        runCurrent()
        assertTrue(request.job.isCancelled)
        assertEquals(listOf(MatchesUiState.Success(listOf(match)), MatchesUiState.Loading), states)
    }

    @Test
    fun `matchday navigation does not request beyond league boundaries`() = runTest {
        val vm = own(MatchesViewModel(repository))
        runCurrent()
        repository.leagues.single().result.complete(MatchdayResult(1, 2025, emptyList()))
        runCurrent()
        vm.goToPreviousMatchday()
        runCurrent()
        assertTrue(repository.matchdays.isEmpty())
        vm.loadLeague("BL1")
        runCurrent()
        repository.leagues.last().result.complete(MatchdayResult(34, 2025, emptyList()))
        runCurrent()
        vm.goToNextMatchday()
        runCurrent()
        assertTrue(repository.matchdays.isEmpty())
        assertEquals(34, vm.currentMatchday.value)
    }

    @Test
    fun `repository cancellation is rethrown without publishing an error`() = runTest {
        val vm = own(MatchesViewModel(repository))
        val states = record(vm.uiState)
        runCurrent()
        val request = repository.leagues.single()
        request.result.cancel(CancellationException("Cancelled"))
        runCurrent()
        assertTrue(request.job.isCancelled)
        assertEquals(listOf(MatchesUiState.Loading), states)
    }
}
