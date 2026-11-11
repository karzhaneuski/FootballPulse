package com.artsiom.footballpulse.ui.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.artsiom.footballpulse.R

internal const val TAG_APP_CONTENT = "app_content"
internal const val TAG_BOTTOM_NAV = "bottom_nav"

private enum class Tab(val route: String, @StringRes val label: Int, @DrawableRes val icon: Int) {
    Matches(Routes.MATCHES, R.string.nav_matches, R.drawable.ic_matches),
    Standings(Routes.STANDINGS, R.string.nav_standings, R.drawable.ic_standings)
}

@Composable
fun AppShell(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // Details screens keep the tab they were opened from highlighted, as the old bottom navigation did.
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Matches) }
    LaunchedEffect(currentRoute) {
        Tab.entries.firstOrNull { it.route == currentRoute }?.let { selectedTab = it }
    }
    Scaffold(
        bottomBar = {
            AppBottomBar(selectedTab) { tab ->
                selectedTab = tab
                navController.navigateToTab(tab.route)
            }
        },
        floatingActionButton = { ThemeToggleButton(darkTheme, onToggleTheme) },
        // The old root inset listener covered the cutout as well as the system bars.
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    ) { innerPadding ->
        AppNavHost(navController, Modifier.padding(innerPadding).testTag(TAG_APP_CONTENT))
    }
}

@Composable
private fun AppBottomBar(selected: Tab, onTabClick: (Tab) -> Unit) {
    NavigationBar(
        modifier = Modifier.testTag(TAG_BOTTOM_NAV),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onTabClick(tab) },
                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                label = { Text(stringResource(tab.label)) }
            )
        }
    }
}

@Composable
private fun ThemeToggleButton(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    SmallFloatingActionButton(onClick = onToggleTheme) {
        Icon(
            painterResource(if (darkTheme) R.drawable.ic_sun else R.drawable.ic_moon),
            contentDescription = stringResource(R.string.toggle_theme)
        )
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        // Pop details screens and keep each tab's state, so the back stack cannot be corrupted
        // by tapping a tab while a details screen is open (#18).
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
