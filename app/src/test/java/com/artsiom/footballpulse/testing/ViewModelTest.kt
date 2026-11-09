package com.artsiom.footballpulse.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()
    private val store = ViewModelStore()

    protected fun <T : ViewModel> own(viewModel: T): T = viewModel.also {
        store.put("subject", it)
    }

    @After
    fun clearViewModels() {
        store.clear()
        main.dispatcher.scheduler.runCurrent()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.record(state: StateFlow<T>): List<T> {
    val values = mutableListOf<T>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect { values.add(it) } }
    return values
}
