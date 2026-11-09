package com.artsiom.footballpulse

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.artsiom.footballpulse.ui.matchdetails.MatchDetailsFragment
import com.artsiom.footballpulse.ui.matches.MatchesFragment
import com.artsiom.footballpulse.ui.standings.StandingsFragment
import com.artsiom.footballpulse.ui.teamdetails.TeamDetailsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "settings"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    private val matchesFragment by lazy { MatchesFragment() }
    private val standingsFragment by lazy { StandingsFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val content = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            // The parent keeps both content and bottom navigation inside the bars;
            // consuming prevents BottomNavigationView from applying the bottom inset twice.
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(content)

        val btnThemeToggle = findViewById<FloatingActionButton>(R.id.btnThemeToggle)
        val isNightNow = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        btnThemeToggle.setImageResource(if (isNightNow) R.drawable.ic_sun else R.drawable.ic_moon)

        btnThemeToggle.setOnClickListener {
            val isDarkNow = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            if (isDarkNow) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                prefs.edit().putBoolean(KEY_DARK_MODE, false).apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                prefs.edit().putBoolean(KEY_DARK_MODE, true).apply()
            }
        }

        if (savedInstanceState == null) {
            showFragment(matchesFragment)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_matches -> { showFragment(matchesFragment); true }
                R.id.nav_standings -> { showFragment(standingsFragment); true }
                else -> false
            }
        }
    }

    private fun showFragment(target: Fragment) {
        val tx = supportFragmentManager.beginTransaction()
        supportFragmentManager.fragments.forEach { if (it !== target) tx.hide(it) }
        if (!target.isAdded) tx.add(R.id.fragmentContainer, target) else tx.show(target)
        tx.commit()
    }

    fun navigateToMatchDetails(matchId: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MatchDetailsFragment.newInstance(matchId))
            .addToBackStack(null)
            .commit()
    }

    fun navigateToTeamDetails(teamId: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, TeamDetailsFragment.newInstance(teamId))
            .addToBackStack(null)
            .commit()
    }
}
