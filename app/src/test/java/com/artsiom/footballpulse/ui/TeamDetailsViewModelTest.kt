package com.artsiom.footballpulse.ui

import com.artsiom.footballpulse.testing.FakeFootballRepository
import com.artsiom.footballpulse.testing.ViewModelTest
import com.artsiom.footballpulse.testing.team
import com.artsiom.footballpulse.testing.record
import com.artsiom.footballpulse.ui.teamdetails.TeamDetailsUiState
import com.artsiom.footballpulse.ui.teamdetails.TeamDetailsViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeamDetailsViewModelTest : ViewModelTest() {
    private val repository = FakeFootballRepository()

    @Test
    fun `initial load publishes loading then requested details`() = runTest {
        val vm = own(TeamDetailsViewModel(repository, 7))
        val states = record(vm.uiState)
        runCurrent()
        assertEquals(listOf(7), repository.teams.single().arguments)
        repository.teams.single().result.complete(team)
        runCurrent()
        assertEquals(listOf(TeamDetailsUiState.Loading, TeamDetailsUiState.Success(team)), states)
    }

    @Test
    fun `failed load publishes error and retry recovers the same identifier`() = runTest {
        val vm = own(TeamDetailsViewModel(repository, 7))
        val states = record(vm.uiState)
        runCurrent()
        repository.teams.single().result.completeExceptionally(IOException("Offline"))
        runCurrent()
        vm.retry()
        runCurrent()
        assertEquals(listOf(7), repository.teams.last().arguments)
        repository.teams.last().result.complete(team)
        runCurrent()
        assertEquals(listOf(TeamDetailsUiState.Loading, TeamDetailsUiState.Error("Offline"),
            TeamDetailsUiState.Loading, TeamDetailsUiState.Success(team)), states)
    }

    @Test
    fun `exception without message still publishes an error`() = runTest {
        val vm = own(TeamDetailsViewModel(repository, 7))
        runCurrent()
        repository.teams.single().result.completeExceptionally(IOException())
        runCurrent()
        assertTrue(vm.uiState.value is TeamDetailsUiState.Error)
        assertTrue((vm.uiState.value as TeamDetailsUiState.Error).message.isNotBlank())
    }

    @Test
    fun `repository cancellation is rethrown without showing an error`() = runTest {
        val vm = own(TeamDetailsViewModel(repository, 7))
        val states = record(vm.uiState)
        runCurrent()
        val request = repository.teams.single()
        request.result.cancel(CancellationException("Cancelled"))
        runCurrent()
        assertTrue(request.job.isCancelled)
        assertEquals(listOf(TeamDetailsUiState.Loading), states)
    }
}
