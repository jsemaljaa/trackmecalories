package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.UserPrefs
import com.jsemalja.trackmecalories.data.HealthData
import com.jsemalja.trackmecalories.fasting.FastingTimer
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import com.jsemalja.trackmecalories.widget.TodayWidget
import com.jsemalja.trackmecalories.data.Database
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import android.content.Intent
import androidx.compose.ui.res.stringResource
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsemalja.trackmecalories.data.RestaurantMenu
import com.jsemalja.trackmecalories.data.BaseFoods
import com.jsemalja.trackmecalories.ui.screens.HomeScreen
import com.jsemalja.trackmecalories.ui.screens.LocalUndo
import com.jsemalja.trackmecalories.ui.screens.rememberUndoController
import com.jsemalja.trackmecalories.ui.screens.OnboardingDialog
import com.jsemalja.trackmecalories.ui.screens.ScannerScreen
import com.jsemalja.trackmecalories.ui.screens.SearchScreen
import com.jsemalja.trackmecalories.ui.screens.settings.SettingsScreen
import com.jsemalja.trackmecalories.ui.screens.TrendsScreen
import com.jsemalja.trackmecalories.ui.theme.AppearancePrefs
import com.jsemalja.trackmecalories.ui.theme.TrackmecaloriesTheme
import com.jsemalja.trackmecalories.ui.theme.fadeThrough
import com.jsemalja.trackmecalories.ui.theme.predictiveBack
import com.jsemalja.trackmecalories.ui.theme.rememberPredictiveBack
import com.jsemalja.trackmecalories.ui.theme.isGlass
import com.jsemalja.trackmecalories.ui.theme.AppTheme
import com.jsemalja.trackmecalories.ui.theme.LocalAppTheme
import com.jsemalja.trackmecalories.ui.theme.raisedEdge
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.Role
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    // Set by a launcher shortcut or widget button; consumed once by the UI.
    private val pendingAction = MutableStateFlow<QuickAction?>(null)
    private lateinit var viewModelFactory: MainViewModelFactory

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
        val baseFoods = BaseFoods(applicationContext.assets)
        val restaurantMenu = RestaurantMenu(applicationContext.assets)
        val appearancePrefs = AppearancePrefs(applicationContext)
        val userPrefs = UserPrefs(applicationContext)
        viewModelFactory = MainViewModelFactory(
            dao, baseFoods, restaurantMenu, userPrefs, HealthData(applicationContext), FastingTimer(applicationContext, dao, userPrefs)
        )

        enableEdgeToEdge()
        setContent {
            val appearance by appearancePrefs.appearance.collectAsState()
            TrackmecaloriesTheme(appearance = appearance) {
                val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)
                val action by pendingAction.collectAsState()
                TrackmecaloriesApp(mainViewModel, appearancePrefs, action, onActionHandled = { pendingAction.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        QuickAction.from(intent)?.let { pendingAction.value = it }
    }

    override fun onResume() {
        super.onResume()
        // The watch may have synced (or permissions changed) while we were away. Same
        // instance as the UI's viewModel(): both come from this activity's store.
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        viewModel.refreshHealth()
        viewModel.refreshFasting()
        viewModel.refreshToday()
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

    val undo = rememberUndoController()
    // Back from another tab goes to Today (the start destination) before leaving the app.
    val tabBack = rememberPredictiveBack(enabled = currentDestination != AppDestinations.HOME && !showScanner) {
        currentDestination = AppDestinations.HOME
    }

    CompositionLocalProvider(LocalUndo provides undo) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // System bars plus the camera cutout, so nothing hides under it in landscape.
            contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
            // Home shows the snackbar itself, above its add button.
            snackbarHost = { if (currentDestination != AppDestinations.HOME) SnackbarHost(undo.hostState) },
            bottomBar = {
                MinimalNavBar(
                    current = currentDestination,
                    onSelect = { currentDestination = it }
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeThrough() },
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).predictiveBack(tabBack),
                label = "tab"
            ) { destination ->
                val modifier = Modifier.fillMaxSize()
                when (destination) {
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
                        onThemeChange = appearancePrefs::setTheme,
                        onAccentChange = appearancePrefs::setAccent,
                        onCustomAccentChange = appearancePrefs::setCustomAccent,
                        onFontChange = appearancePrefs::setFont,
                        onCornersChange = appearancePrefs::setCorners,
                        onModeChange = appearancePrefs::setMode
                    )
                }
            }
        }

        // The scanner is a full-screen overlay rather than a tab: it's an action, not a place.
        AnimatedVisibility(
            visible = showScanner,
            enter = fadeIn() + slideInVertically { it / 10 },
            exit = fadeOut() + slideOutVertically { it / 10 }
        ) {
            val scannerBack = rememberPredictiveBack(enabled = showScanner) { showScanner = false }
            ScannerScreen(viewModel, Modifier.predictiveBack(scannerBack), onClose = { showScanner = false })
        }
    }

    if (!hasSetGoals) {
        OnboardingDialog(onComplete = { goal -> viewModel.updateGoals(goal) })
    }
}

/**
 * The tab bar, in the current theme. Classic and Glass: a floating pill with four
 * icon-only tabs, the selected one expanding to show its label (Classic's floats on a
 * shadow, Glass's has a light edge). Bold: the same, but a bordered block with a hard
 * shadow and a solid selected tab; Terminal a square hairline panel with a solid one.
 * Paper: a docked bar under an ink rule with every label shown (see [DockedNavBar]).
 */
@Composable
private fun MinimalNavBar(current: AppDestinations, onSelect: (AppDestinations) -> Unit) {
    val theme = LocalAppTheme.current
    if (theme == AppTheme.PAPER) {
        DockedNavBar(current, onSelect)
        return
    }
    val colors = MaterialTheme.colorScheme
    val glass = isGlass
    val shape = if (theme == AppTheme.BOLD || theme == AppTheme.TERMINAL) MaterialTheme.shapes.large else CircleShape
    val (edge, raisedBorder) = raisedEdge(shape)

    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding()) {
        // Tighter on narrow phones, and the selected label gets only the width that's left
        // (ellipsized past that), so a long translation can't push the pill off screen.
        val narrow = maxWidth < 400.dp
        val sidePadding = if (narrow) 12.dp else 24.dp
        val itemPadding = if (narrow) 12.dp else 16.dp
        val count = AppDestinations.entries.size
        val labelMaxWidth = (maxWidth - sidePadding * 2 - 12.dp - 4.dp * (count - 1) - (24.dp + itemPadding * 2) * count - 8.dp)
            .coerceAtLeast(0.dp)

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = sidePadding, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = edge,
                shape = shape,
                color = if (glass) colors.surfaceContainer else colors.surfaceContainerLow,
                border = raisedBorder
            ) {
                Row(
                    Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppDestinations.entries.forEach { destination ->
                        NavItem(destination, selected = destination == current, itemPadding, labelMaxWidth) { onSelect(destination) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(destination: AppDestinations, selected: Boolean, horizontalPadding: Dp, labelMaxWidth: Dp, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val theme = LocalAppTheme.current
    val bold = theme == AppTheme.BOLD
    // Bold and Terminal mark the tab solid; Classic with a tonal pill, like Material's nav.
    val solid = bold || theme == AppTheme.TERMINAL
    val background by animateColorAsState(
        when {
            !selected -> Color.Transparent
            solid -> colors.primary
            theme == AppTheme.CLASSIC -> colors.primaryContainer
            else -> colors.primary.copy(alpha = 0.16f)
        },
        label = "navItemBackground"
    )
    val content by animateColorAsState(
        when {
            !selected -> colors.onSurfaceVariant
            solid -> colors.onPrimary
            theme == AppTheme.CLASSIC -> colors.onPrimaryContainer
            else -> colors.primary
        },
        label = "navItemContent"
    )

    Surface(
        onClick = onClick,
        shape = if (solid) MaterialTheme.shapes.medium else CircleShape,
        color = background,
        contentColor = content,
        border = if (bold && selected) androidx.compose.foundation.BorderStroke(2.dp, colors.outline) else null
    ) {
        Row(
            Modifier
                .height(48.dp)
                .padding(horizontal = horizontalPadding),
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
                    Text(
                        stringResource(destination.label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = labelMaxWidth)
                    )
                }
            }
        }
    }
}

/**
 * Paper's tab bar: docked full width under an ink rule, like a page footer, with every
 * tab labelled. The selected tab is inked and marked by a short rule on top.
 */
@Composable
private fun DockedNavBar(current: AppDestinations, onSelect: (AppDestinations) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(thickness = 1.dp, color = colors.onSurface)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp)) {
            AppDestinations.entries.forEach { destination ->
                val selected = destination == current
                val content by animateColorAsState(
                    if (selected) colors.onSurface else colors.onSurfaceVariant,
                    label = "dockedNavContent"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = selected, onClick = { onSelect(destination) }, role = Role.Tab),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.width(32.dp).height(3.dp).background(if (selected) colors.onSurface else Color.Transparent))
                    Spacer(Modifier.height(8.dp))
                    Icon(
                        if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(destination.label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
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
