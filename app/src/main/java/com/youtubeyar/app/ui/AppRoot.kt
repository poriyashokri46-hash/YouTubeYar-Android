package com.youtubeyar.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.youtubeyar.app.ads.TapsellAdsManager
import com.youtubeyar.app.billing.BillingManager
import com.youtubeyar.app.data.AppSettings
import com.youtubeyar.app.data.SettingsRepository
import kotlinx.coroutines.launch
import com.youtubeyar.app.ui.screens.AcademyScreen
import com.youtubeyar.app.ui.screens.AiScreen
import com.youtubeyar.app.ui.screens.AnalyzerScreen
import com.youtubeyar.app.ui.screens.ChecklistScreen
import com.youtubeyar.app.ui.screens.DashboardScreen
import com.youtubeyar.app.ui.screens.OnboardingScreen
import com.youtubeyar.app.ui.screens.PremiumScreen
import com.youtubeyar.app.ui.screens.SettingsScreen
import com.youtubeyar.app.ui.theme.YouTubeYarTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    val adsManager: TapsellAdsManager,
    private val billingManager: BillingManager
) : ViewModel() {
    val settings = settingsRepository.settings

    init {
        // Reconcile cached premium state with the store's current, signature-verified purchases.
        billingManager.restorePurchases { _, _ -> }
    }

    fun saveOnboarding(name: String, handle: String, niche: String) {
        viewModelScope.launch {
            settingsRepository.setChannel(name.ifBlank { "کانال من" }, handle, niche)
            settingsRepository.setFirstRunDone()
        }
    }

    fun setTheme(v: String) {
        viewModelScope.launch { settingsRepository.setTheme(v) }
    }
}

object Routes {
    const val HOME = "home"
    const val ANALYZER = "analyzer"
    const val AI = "ai"
    const val CHECKLIST = "checklist"
    const val ACADEMY = "academy"
    const val PREMIUM = "premium"
    const val SETTINGS = "settings"
}

data class BottomItem(val route: String, val icon: ImageVector, val labelRes: Int)

@Composable
fun AppRoot(mainViewModel: MainViewModel = hiltViewModel()) {
    val settings by mainViewModel.settings.collectAsStateWithLifecycle(initialValue = null)
    val s: AppSettings? = settings

    val themePref = s?.theme ?: "system"
    val firstRun = s?.firstRunDone

    YouTubeYarTheme(themePref = themePref) {
        when {
            s == null -> { /* در حال بارگذاری تنظیمات */ }
            firstRun != true -> OnboardingScreen(mainViewModel = mainViewModel)
            else -> MainScaffold(mainViewModel = mainViewModel)
        }
    }
}

@Composable
private fun MainScaffold(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val premium = settings.isPremium()

    // آماده‌سازی تبلیغات تپسل (اگر کلیدها تنظیم شده باشند)
    LaunchedEffect(Unit) {
        mainViewModel.adsManager.initIfNeeded()
        mainViewModel.adsManager.preloadInterstitial()
    }

    val items = listOf(
        BottomItem(Routes.HOME, Icons.Filled.Home, com.youtubeyar.app.R.string.nav_home),
        BottomItem(Routes.ANALYZER, Icons.Filled.Analytics, com.youtubeyar.app.R.string.nav_analyzer),
        BottomItem(Routes.AI, Icons.Filled.SmartToy, com.youtubeyar.app.R.string.nav_ai),
        BottomItem(Routes.CHECKLIST, Icons.Filled.TaskAlt, com.youtubeyar.app.R.string.nav_checklist),
        BottomItem(Routes.ACADEMY, Icons.Filled.School, com.youtubeyar.app.R.string.nav_academy)
    )

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in items.map { it.route } ||
        currentRoute == Routes.PREMIUM || currentRoute == Routes.SETTINGS

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme3SafeSurface()) {
                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes), maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) { DashboardScreen(navController, premium) }
            composable(Routes.ANALYZER) { AnalyzerScreen(navController) }
            composable(Routes.AI) { AiScreen(navController, premium) }
            composable(Routes.CHECKLIST) { ChecklistScreen(navController) }
            composable(Routes.ACADEMY) { AcademyScreen(navController) }
            composable(Routes.PREMIUM) { PremiumScreen(navController, mainViewModel) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
        }
    }
}

@Composable
private fun MaterialTheme3SafeSurface() = androidx.compose.material3.MaterialTheme.colorScheme.surface
