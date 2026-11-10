package com.artsiom.footballpulse

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artsiom.footballpulse.ui.standings.StandingsFragment
import com.artsiom.footballpulse.ui.matches.MatchesFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemBarInsetsTest {
    @Test
    fun tabContentAndBottomNavigationStayInsideSystemBars() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertInsets(activity, activity.supportFragmentManager.fragments
                    .filterIsInstance<MatchesFragment>().single().requireView())
                activity.findViewById<BottomNavigationView>(R.id.bottomNav)
                    .selectedItemId = R.id.nav_standings
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertInsets(activity, standingsView(activity))
                ViewCompat.requestApplyInsets(activity.findViewById(android.R.id.content))
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                // Redispatch must not accumulate padding.
                assertInsets(activity, standingsView(activity))
            }
        }
    }

    private fun standingsView(activity: MainActivity): View =
        activity.supportFragmentManager.fragments.filterIsInstance<StandingsFragment>().single().requireView()

    private fun assertInsets(activity: MainActivity, tabs: View) {
        val content = activity.findViewById<View>(android.R.id.content)
        val windowInsets = ViewCompat.getRootWindowInsets(content)
        assertNotNull(windowInsets)
        val bars = windowInsets!!.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        assertTrue("The portrait device must have a visible status bar", bars.top > 0)
        assertEquals(bars.top, content.paddingTop)
        assertEquals(bars.bottom, content.paddingBottom)
        assertEquals(bars.left, content.paddingLeft)
        assertEquals(bars.right, content.paddingRight)
        val position = IntArray(2)
        tabs.getLocationOnScreen(position)
        assertEquals(bars.top, position[1])
        assertEquals(0, tabs.paddingTop)
        val bottomNav = activity.findViewById<View>(R.id.bottomNav)
        bottomNav.getLocationOnScreen(position)
        assertTrue(position[1] + bottomNav.height <= content.height - bars.bottom)
        val fragmentContainer = activity.findViewById<View>(R.id.fragmentContainer)
        assertTrue(fragmentContainer.bottom <= bottomNav.top)
    }
}
