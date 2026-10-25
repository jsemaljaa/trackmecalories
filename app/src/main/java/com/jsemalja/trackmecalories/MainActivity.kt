package com.jsemalja.trackmecalories

import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import com.jsemalja.trackmecalories.widget.TodayWidget
import com.jsemalja.trackmecalories.data.Database
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import android.content.Intent
import androidx.compose.ui.res.stringResource
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsemalja.trackmecalories.data.RestaurantMenu
import com.jsemalja.trackmecalories.ui.screens.HomeScreen
import com.jsemalja.trackmecalories.ui.screens.OnboardingDialog
import com.jsemalja.trackmecalories.ui.screens.ScannerScreen
import com.jsemalja.trackmecalories.ui.screens.SearchScreen
import com.jsemalja.trackmecalories.ui.screens.SettingsScreen
import com.jsemalja.trackmecalories.ui.screens.TrendsScreen
import com.jsemalja.trackmecalories.ui.theme.AppearancePrefs
import com.jsemalja.trackmecalories.ui.theme.TrackmecaloriesTheme
import com.jsemalja.trackmecalories.ui.theme.glassBorder
import com.jsemalja.trackmecalories.ui.theme.isGlass
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    // Set by a launcher shortcut or widget button; consumed once by the UI.
    private val pendingAction = MutableStateFlow<QuickAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingAction.value = QuickAction.from(intent)

        val dao = Database.get(applicationContext).foodDao()

        // Keep the home-screen widget in step with today's log while the app is open.
        lifecycleScope.launch {
            combine(dao.getEntriesForDate(LocalDate.now().toString()), dao.getUserGoal()) { e, g -> e.sumOf { it.kcal } to g?.calorieGoal }
                .distinctUntilChanged()
                .collect { TodayWidget.refresh(applicationContext) }
        }
        val restaurantMenu = RestaurantMenu(applicationContext.assets)
        val appearancePrefs = AppearancePrefs(applicationContext)

        enableEdgeToEdge()
        setContent {
            val appearance by appearancePrefs.appearance.collectAsState()
            TrackmecaloriesTheme(appearance = appearance) {
                val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(dao, restaurantMenu))
                val action by pendingAction.collectAsState()
                TrackmecaloriesApp(mainViewModel, appearancePrefs, action, onActionHandled = { pendingAction.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        QuickAction.from(intent)?.let { pendingAction.value = it }
    }

    override fun onStop() {
        super.onStop()
        TodayWidget.refresh(applicationContext)
    }
}

@Composable
fun TrackmecaloriesApp(
    viewModel: MainViewModel,
    appearancePrefs: AppearancePrefs,
    pendingAction: QuickAction? = null,
    onActionHandled: () -> Unit = {}
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var showScanner by rememberSaveable { mutableStateOf(false) }
    // Quick add / log weight open as dialogs on Home.
    var homeRequest by remember { mutableStateOf<QuickAction?>(null) }

    LaunchedEffect(pendingAction) {
        when (pendingAction ?: return@LaunchedEffect) {
            QuickAction.ADD_FOOD -> currentDestination = AppDestinations.FOOD
            QuickAction.SCAN -> showScanner = true
            QuickAction.QUICK_ADD, QuickAction.LOG_WEIGHT -> {
                // Shortcuts act on today, whatever day was being browsed.
                viewModel.changeDate(LocalDate.now())
                currentDestination = AppDestinations.HOME
                homeRequest = pendingAction
            }
        }
        onActionHandled()
    }
    val hasSetGoals by viewModel.hasSetGoals.collectAsState()
    val appearance by appearancePrefs.appearance.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MinimalNavBar(
                current = currentDestination,
                onSelect = { currentDestination = it }
            )
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (currentDestination) {
            AppDestinations.HOME -> HomeScreen(
                viewModel,
                modifier,
                onOpenSearch = { currentDestination = AppDestinations.FOOD },
                onOpenScanner = { showScanner = true },
                request = homeRequest,
                onRequestHandled = { homeRequest = null }
            )
            AppDestinations.FOOD -> SearchScreen(viewModel, modifier, onOpenScanner = { showScanner = true })
            AppDestinations.TRENDS -> TrendsScreen(
                viewModel,
                modifier,
                // Tapping a day in the chart opens that day's log.
                onOpenDay = { date ->
                    viewModel.changeDate(date)
                    currentDestination = AppDestinations.HOME
                }
            )
            AppDestinations.SETTINGS -> SettingsScreen(
                viewModel,
                modifier,
                appearance = appearance,
                onStyleChange = appearancePrefs::setStyle,
                onAccentChange = appearancePrefs::setAccent
            )
        }
    }

    // The scanner is a full-screen overlay rather than a tab: it's an action, not a place.
    if (showScanner) {
        BackHandler { showScanner = false }
        ScannerScreen(viewModel, onClose = { showScanner = false })
    }

    if (!hasSetGoals) {
        OnboardingDialog(onComplete = { goal -> viewModel.updateGoals(goal) })
    }
}

/**
 * A floating pill with four icon-only tabs; the selected one expands to show its label.
 * Flat: a solid surface with a hairline outline. Glass: frosted with a light edge.
 */
@Composable
private fun MinimalNavBar(current: AppDestinations, onSelect: (AppDestinations) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val glass = isGlass
    val shape = CircleShape

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = shape,
            color = if (glass) colors.surfaceContainer else colors.surfaceContainerLow,
            border = if (glass) glassBorder() else androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(
                Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDestinations.entries.forEach { destination ->
                    NavItem(destination, selected = destination == current) { onSelect(destination) }
                }
            }
        }
    }
}

@Composable
private fun NavItem(destination: AppDestinations, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        if (selected) colors.primary.copy(alpha = 0.16f) else Color.Transparent,
        label = "navItemBackground"
    )
    val content by animateColorAsState(
        if (selected) colors.primary else colors.onSurfaceVariant,
        label = "navItemContent"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        contentColor = content
    ) {
        Row(
            Modifier
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (selected) destination.selectedIcon else destination.icon,
                contentDescription = stringResource(destination.label),
                modifier = Modifier.size(24.dp)
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row {
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(destination.label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

enum class AppDestinations(
    @androidx.annotation.StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(R.string.nav_today, Icons.Outlined.Home, Icons.Filled.Home),
    FOOD(R.string.nav_food, Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
    TRENDS(R.string.nav_trends, Icons.Outlined.BarChart, Icons.Filled.BarChart),
    SETTINGS(R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
}
