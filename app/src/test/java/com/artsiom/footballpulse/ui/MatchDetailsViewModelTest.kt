package com.artsiom.footballpulse.ui

import com.artsiom.footballpulse.testing.FakeFootballRepository
import com.artsiom.footballpulse.testing.ViewModelTest
import com.artsiom.footballpulse.testing.matchDetail
import com.artsiom.footballpulse.testing.record
import com.artsiom.footballpulse.ui.matchdetails.MatchDetailsUiState
import com.artsiom.footballpulse.ui.matchdetails.MatchDetailsViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDetailsViewModelTest : ViewModelTest() {
    private val repository = FakeFootballRepository()

    @Test
    fun `initial load publishes loading then requested details`() = runTest {
        val vm = own(MatchDetailsViewModel(repository, 42))
        val states = record(vm.uiState)
        runCurrent()
        assertEquals(listOf(42), repository.matches.single().arguments)
        repository.matches.single().result.complete(matchDetail)
        runCurrent()
        assertEquals(listOf(MatchDetailsUiState.Loading, MatchDetailsUiState.Success(matchDetail)), states)
    }

    @Test
    fun `failed load publishes error and retry recovers the same identifier`() = runTest {
        val vm = own(MatchDetailsViewModel(repository, 42))
        val states = record(vm.uiState)
        runCurrent()
        repository.matches.single().result.completeExceptionally(IOException("Offline"))
        runCurrent()
        vm.retry()
        runCurrent()
        assertEquals(listOf(42), repository.matches.last().arguments)
        repository.matches.last().result.complete(matchDetail)
        runCurrent()
        assertEquals(listOf(MatchDetailsUiState.Loading, MatchDetailsUiState.Error("Offline"),
            MatchDetailsUiState.Loading, MatchDetailsUiState.Success(matchDetail)), states)
    }

    @Test
    fun `exception without message still publishes an error`() = runTest {
        val vm = own(MatchDetailsViewModel(repository, 42))
        runCurrent()
        repository.matches.single().result.completeExceptionally(IOException())
        runCurrent()
        assertTrue(vm.uiState.value is MatchDetailsUiState.Error)
        assertTrue((vm.uiState.value as MatchDetailsUiState.Error).message.isNotBlank())
    }

    @Test
    fun `repository cancellation is rethrown without showing an error`() = runTest {
        val vm = own(MatchDetailsViewModel(repository, 42))
        val states = record(vm.uiState)
        runCurrent()
        val request = repository.matches.single()
        request.result.cancel(CancellationException("Cancelled"))
        runCurrent()
        assertTrue(request.job.isCancelled)
        assertEquals(listOf(MatchDetailsUiState.Loading), states)
    }
}
