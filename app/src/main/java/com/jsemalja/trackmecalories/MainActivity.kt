package com.jsemalja.trackmecalories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.jsemalja.trackmecalories.data.AppDatabase
import com.jsemalja.trackmecalories.data.MIGRATION_5_6
import com.jsemalja.trackmecalories.ui.screens.HomeScreen
import com.jsemalja.trackmecalories.ui.screens.OnboardingDialog
import com.jsemalja.trackmecalories.ui.screens.RecentsScreen
import com.jsemalja.trackmecalories.ui.screens.ScannerScreen
import com.jsemalja.trackmecalories.ui.screens.SearchScreen
import com.jsemalja.trackmecalories.ui.screens.SettingsScreen
import com.jsemalja.trackmecalories.ui.screens.TrendsScreen
import com.jsemalja.trackmecalories.ui.theme.TrackmecaloriesTheme
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "track-me-calories-db"
        )
            .addMigrations(MIGRATION_5_6)
            // Only destructive on downgrade (e.g. reinstalling an older debug build).
            // Upgrades must ship a real Migration so user data is never silently wiped.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
        val dao = db.foodDao()

        enableEdgeToEdge()
        setContent {
            TrackmecaloriesTheme {
                val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(dao))
                TrackmecaloriesApp(mainViewModel)
            }
        }
    }
}

@Composable
fun TrackmecaloriesApp(viewModel: MainViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val hasSetGoals by viewModel.hasSetGoals.collectAsState()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(viewModel, Modifier.padding(innerPadding))
                AppDestinations.SEARCH -> SearchScreen(viewModel, Modifier.padding(innerPadding))
                AppDestinations.SCAN -> ScannerScreen(viewModel, Modifier.padding(innerPadding))
                AppDestinations.RECENTS -> RecentsScreen(viewModel, Modifier.padding(innerPadding))
                AppDestinations.TRENDS -> TrendsScreen(viewModel, Modifier.padding(innerPadding))
                AppDestinations.SETTINGS -> SettingsScreen(viewModel, Modifier.padding(innerPadding))
            }
        }
    }

    if (!hasSetGoals) {
        OnboardingDialog(onComplete = { goal -> viewModel.updateGoals(goal) })
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    SCAN("Scan", Icons.Default.CameraAlt),
    RECENTS("Recents", Icons.Default.History),
    TRENDS("Trends", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings),
}
