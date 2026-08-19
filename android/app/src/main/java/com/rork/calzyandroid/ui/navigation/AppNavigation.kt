package com.rork.calzyandroid.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.AnalysisResult
import com.rork.calzyandroid.data.EntrySource
import com.rork.calzyandroid.data.MealEntry
import com.rork.calzyandroid.ui.components.CalzyBackdrop
import com.rork.calzyandroid.ui.components.ProvideAppLanguage
import com.rork.calzyandroid.ui.screens.HomeScreen
import com.rork.calzyandroid.ui.screens.OnboardingScreen
import com.rork.calzyandroid.ui.screens.ProgressScreen
import com.rork.calzyandroid.ui.screens.SettingsScreen
import com.rork.calzyandroid.ui.sheets.DescribeSheet
import com.rork.calzyandroid.ui.sheets.EditMealSheet
import com.rork.calzyandroid.ui.sheets.ExerciseSheet
import com.rork.calzyandroid.ui.sheets.MealDetailSheet
import com.rork.calzyandroid.ui.sheets.MealResultSheet
import com.rork.calzyandroid.ui.sheets.SavedSheet
import com.rork.calzyandroid.ui.sheets.ScanSheet
import com.rork.calzyandroid.ui.sheets.SearchSheet

/** Which quick-entry flow is open on top of the home tab. */
enum class HomeRoute { scan, describe, search, saved, exercise }

/** Which main tab is active. */
enum class AppTab { home, progress, settings }

/** A recognised meal waiting for user review before logging. */
data class MealDraft(
    val result: AnalysisResult,
    val photo: String? = null,
    val source: EntrySource,
)

@Composable
fun AppNavigation() {
    val viewModel: AppViewModel = viewModel()
    val data by viewModel.data.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    ProvideAppLanguage(languageCode = data.profile.languageCode) {
        NavHost(
            navController = navController,
            startDestination = if (data.profile.hasOnboarded) "main" else "onboarding",
        ) {
            composable("onboarding") {
                OnboardingScreen(viewModel = viewModel)
                LaunchedEffect(data.profile.hasOnboarded) {
                    if (data.profile.hasOnboarded) {
                        navController.navigate("main") { popUpTo("onboarding") { inclusive = true } }
                    }
                }
            }
            composable("main") {
                MainShell(viewModel = viewModel)
                // Erasing all data sends the user back through onboarding.
                LaunchedEffect(data.profile.hasOnboarded) {
                    if (!data.profile.hasOnboarded) {
                        navController.navigate("onboarding") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}

/** Root shell: tab content, floating tab bar and sheet routing. */
@Composable
private fun MainShell(viewModel: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(AppTab.home) }
    var route by remember { mutableStateOf<HomeRoute?>(null) }
    var draft by remember { mutableStateOf<MealDraft?>(null) }
    var openMeal by remember { mutableStateOf<MealEntry?>(null) }
    var editingMeal by remember { mutableStateOf<MealEntry?>(null) }

    val onResult: (MealDraft) -> Unit = { next ->
        route = null
        draft = next
    }

    CalzyBackdrop {
        Box(modifier = Modifier.fillMaxSize()) {
            when (tab) {
                AppTab.home -> HomeScreen(
                    viewModel = viewModel,
                    onRoute = { route = it },
                    onOpenMeal = { openMeal = it },
                    onEditMeal = { editingMeal = it },
                )
                AppTab.progress -> ProgressScreen(viewModel = viewModel)
                AppTab.settings -> SettingsScreen(viewModel = viewModel)
            }

            TabBar(
                active = tab,
                onChange = { tab = it },
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            )

            ScanSheet(
                open = route == HomeRoute.scan,
                viewModel = viewModel,
                onClose = { route = null },
                onResult = onResult,
            )
            DescribeSheet(
                open = route == HomeRoute.describe,
                viewModel = viewModel,
                onClose = { route = null },
                onResult = onResult,
            )
            SearchSheet(
                open = route == HomeRoute.search,
                viewModel = viewModel,
                onClose = { route = null },
            )
            SavedSheet(
                open = route == HomeRoute.saved,
                viewModel = viewModel,
                onClose = { route = null },
            )
            ExerciseSheet(
                open = route == HomeRoute.exercise,
                viewModel = viewModel,
                onClose = { route = null },
            )

            MealResultSheet(
                draft = draft,
                viewModel = viewModel,
                onClose = { draft = null },
            )
            MealDetailSheet(
                meal = openMeal,
                viewModel = viewModel,
                onClose = { openMeal = null },
                onEdit = { editingMeal = it },
            )
            EditMealSheet(
                meal = editingMeal,
                viewModel = viewModel,
                onClose = { editingMeal = null },
                onSaved = { updated ->
                    // Keep an open detail sheet in sync with the correction.
                    if (openMeal?.id == updated.id) openMeal = updated
                },
            )
        }
    }
}
