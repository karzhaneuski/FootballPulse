package com.artsiom.footballpulse.ui

import com.artsiom.footballpulse.domain.model.LeagueZone
import com.artsiom.footballpulse.testing.FakeFootballRepository
import com.artsiom.footballpulse.testing.ViewModelTest
import com.artsiom.footballpulse.testing.record
import com.artsiom.footballpulse.testing.standing
import com.artsiom.footballpulse.ui.standings.StandingsUiState
import com.artsiom.footballpulse.ui.standings.StandingsViewModel
import com.artsiom.footballpulse.ui.standings.ViewMode
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
class StandingsViewModelTest : ViewModelTest() {
    private val repository = FakeFootballRepository()

    @Test
    fun `success maps zones for selected league rather than the default league`() = runTest {
        val vm = own(StandingsViewModel(repository))
        vm.loadLeague("BL1")
        runCurrent()
        repository.standings.single().result.complete(listOf(standing.copy(position = 16)))
        runCurrent()
        val state = vm.uiState.value as StandingsUiState.Success
        assertEquals(LeagueZone.PLAYOFF, state.rows.single().zone)
    }

    @Test
    fun `initial load publishes loading then standings`() = runTest {
        val vm = own(StandingsViewModel(repository))
        val states = record(vm.uiState)
        runCurrent()
        assertEquals(listOf("PL"), repository.standings.single().arguments)
        repository.standings.single().result.complete(listOf(standing))
        runCurrent()
        assertEquals(listOf(StandingsUiState.Loading, StandingsUiState.Success(listOf(standing))), states)
    }

    @Test
    fun `retry after error publishes loading and recovers selected league`() = runTest {
        val vm = own(StandingsViewModel(repository))
        vm.loadLeague("SA")
        val states = record(vm.uiState)
        runCurrent()
        repository.standings.single().result.completeExceptionally(IOException("Offline"))
        runCurrent()
        vm.retry()
        runCurrent()
        assertEquals(listOf("SA"), repository.standings.last().arguments)
        repository.standings.last().result.complete(emptyList())
        runCurrent()
        assertEquals(listOf(StandingsUiState.Loading, StandingsUiState.Error("Offline"),
            StandingsUiState.Loading, StandingsUiState.Success(emptyList())), states)
    }

    @Test
    fun `league change cancels superseded load without flashing an error`() = runTest {
        val vm = own(StandingsViewModel(repository))
        val states = record(vm.uiState)
        runCurrent()
        val old = repository.standings.single()
        vm.loadLeague("PD")
        runCurrent()
        assertTrue(old.cancelled)
        assertTrue(old.job.isCancelled)
        assertEquals("PD", vm.currentLeagueCode.value)
        assertEquals(listOf("PD"), repository.standings.last().arguments)
        repository.standings.last().result.complete(listOf(standing))
        old.result.complete(emptyList())
        runCurrent()
        assertEquals(StandingsUiState.Success(listOf(standing)), vm.uiState.value)
        assertFalse(states.any { it is StandingsUiState.Error })
    }

    @Test
    fun `changing a loaded league publishes loading before its replacement standings`() = runTest {
        val vm = own(StandingsViewModel(repository))
        runCurrent()
        repository.standings.single().result.complete(listOf(standing))
        runCurrent()
        val states = record(vm.uiState)
        vm.loadLeague("BL1")
        runCurrent()
        assertEquals(listOf("BL1"), repository.standings.last().arguments)
        repository.standings.last().result.complete(emptyList())
        runCurrent()
        assertEquals(listOf(StandingsUiState.Success(listOf(standing)), StandingsUiState.Loading,
            StandingsUiState.Success(emptyList())), states)
    }

    @Test
    fun `retry during load cancels old request and accepts only replacement result`() = runTest {
        val vm = own(StandingsViewModel(repository))
        runCurrent()
        val old = repository.standings.single()
        vm.retry()
        runCurrent()
        assertTrue(old.cancelled)
        assertEquals(2, repository.standings.size)
        repository.standings.last().result.complete(listOf(standing))
        old.result.complete(emptyList())
        runCurrent()
        assertEquals(StandingsUiState.Success(listOf(standing)), vm.uiState.value)
    }

    @Test
    fun `same successful league and view mode changes do not refetch standings`() = runTest {
        val vm = own(StandingsViewModel(repository))
        runCurrent()
        repository.standings.single().result.complete(listOf(standing))
        runCurrent()
        vm.loadLeague("PL")
        vm.setViewMode(ViewMode.FORM)
        runCurrent()
        assertEquals(1, repository.standings.size)
        assertEquals(ViewMode.FORM, vm.viewMode.value)
        assertEquals(StandingsUiState.Success(listOf(standing)), vm.uiState.value)
    }

    @Test
    fun `repository cancellation is rethrown instead of becoming an error`() = runTest {
        val vm = own(StandingsViewModel(repository))
        val states = record(vm.uiState)
        runCurrent()
        val request = repository.standings.single()
        request.result.cancel(CancellationException("Cancelled"))
        runCurrent()
        assertTrue(request.job.isCancelled)
        assertEquals(listOf(StandingsUiState.Loading), states)
    }
}
