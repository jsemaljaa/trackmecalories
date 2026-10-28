package com.jsemalja.trackmecalories

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsemalja.trackmecalories.data.IosFastingTimer
import com.jsemalja.trackmecalories.data.UserDefaultsStore
import com.jsemalja.trackmecalories.data.UserPrefs
import com.jsemalja.trackmecalories.data.bundledBaseFoods
import com.jsemalja.trackmecalories.data.bundledRestaurantMenu
import com.jsemalja.trackmecalories.data.createDatabase
import com.jsemalja.trackmecalories.data.healthKit
import com.jsemalja.trackmecalories.ui.TrackmecaloriesApp
import com.jsemalja.trackmecalories.ui.theme.AppearancePrefs
import com.jsemalja.trackmecalories.ui.theme.TrackmecaloriesTheme
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import platform.UIKit.UIViewController

/** What MainActivity builds on Android: one of each for the whole process. */
private object AppGraph {
    val dao by lazy { createDatabase().foodDao() }
    val userPrefs by lazy { UserPrefs(UserDefaultsStore("user_prefs")) }
    val appearancePrefs by lazy { AppearancePrefs(UserDefaultsStore("appearance")) }
    val baseFoods by lazy { bundledBaseFoods() }
    val restaurantMenu by lazy { bundledRestaurantMenu() }
    val fasting by lazy { IosFastingTimer(dao, userPrefs) }
}

/** The app's root, shown by the Swift app in iosApp/ (IosAppKt.MainViewController()). */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
    val appearance by AppGraph.appearancePrefs.appearance.collectAsState()
    TrackmecaloriesTheme(appearance = appearance) {
        val viewModel = viewModel {
            with(AppGraph) { MainViewModel(dao, baseFoods, restaurantMenu, userPrefs, healthKit, fasting) }
        }
        // As MainActivity.onResume: Health data or permissions may have changed while away.
        LifecycleResumeEffect(viewModel) {
            viewModel.refreshHealth()
            viewModel.refreshFasting()
            viewModel.refreshGamification()
            onPauseOrDispose { }
        }
        TrackmecaloriesApp(viewModel, AppGraph.appearancePrefs)
    }
}
