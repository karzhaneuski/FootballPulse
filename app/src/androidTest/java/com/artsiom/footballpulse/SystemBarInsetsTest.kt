package com.artsiom.footballpulse

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artsiom.footballpulse.ui.shell.TAG_APP_CONTENT
import com.artsiom.footballpulse.ui.shell.TAG_BOTTOM_NAV
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemBarInsetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tabContentAndBottomNavigationStayInsideSystemBars() {
        assertInsets()
        rule.onNodeWithText(rule.activity.getString(R.string.nav_standings)).performClick()
        rule.waitForIdle()
        assertInsets()
    }

    private fun assertInsets() {
        rule.onNodeWithTag(TAG_APP_CONTENT).assertIsDisplayed()
        val root = rule.activity.window.decorView
        val windowInsets = ViewCompat.getRootWindowInsets(root)
        assertNotNull(windowInsets)
        val bars = windowInsets!!.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        assertTrue("The portrait device must have a visible status bar", bars.top > 0)
        val content = rule.onNodeWithTag(TAG_APP_CONTENT).fetchSemanticsNode().boundsInWindow
        val bottomNav = rule.onNodeWithTag(TAG_BOTTOM_NAV).fetchSemanticsNode().boundsInWindow
        assertTrue("Content must start below the status bar", content.top >= bars.top)
        assertTrue("Content must end above the bottom navigation", content.bottom <= bottomNav.top)
        // The bar may draw behind the system navigation bar, but its items must stay clear of it.
        val label = rule.activity.getString(R.string.nav_matches)
        val tabs = rule.onNodeWithText(label).fetchSemanticsNode().boundsInWindow
        assertTrue("Tab labels must stay above the system navigation bar", tabs.bottom <= root.height - bars.bottom)
    }
}
