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
import org.jetbrains.compose.resources.stringResource
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
import com.jsemalja.trackmecalories.ui.theme.glassBorder
import com.jsemalja.trackmecalories.ui.theme.isGlass
import com.jsemalja.trackmecalories.ui.theme.AppTheme
import com.jsemalja.trackmecalories.ui.theme.LocalAppTheme
import com.jsemalja.trackmecalories.ui.theme.raisedEdge
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.Role
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModelFactory
import com.jsemalja.trackmecalories.resources.*
import org.jetbrains.compose.resources.StringResource
import com.jsemalja.trackmecalories.ui.TrackmecaloriesApp

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
        viewModel.refreshGamification()
    }

    override fun onStop() {
        super.onStop()
        TodayWidget.refresh(applicationContext)
    }
}
