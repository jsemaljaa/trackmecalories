package com.jsemalja.trackmecalories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.jsemalja.trackmecalories.ui.theme.TrackmecaloriesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "track-me-calories-db"
        ).fallbackToDestructiveMigration().build()
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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(painterResource(it.icon), contentDescription = it.label) },
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
                AppDestinations.SCAN -> ScannerScreen(viewModel, Modifier.padding(innerPadding))
                AppDestinations.FAVORITES -> Text("Favorites Screen", Modifier.padding(innerPadding))
                AppDestinations.PROFILE -> Text("Profile Screen", Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    SCAN("Scan", R.drawable.ic_camera),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    PROFILE("Profile", R.drawable.ic_account_box),
}
