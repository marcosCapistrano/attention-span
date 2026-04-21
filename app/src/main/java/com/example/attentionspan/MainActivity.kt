package com.example.attentionspan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.attentionspan.data.preferences.UserPreferences
import com.example.attentionspan.ui.screens.CronometerScreen
import com.example.attentionspan.ui.screens.SettingsScreen
import com.example.attentionspan.ui.screens.StatsScreen
import com.example.attentionspan.ui.screens.SubjectListScreen
import com.example.attentionspan.ui.theme.AttentionSpanTheme
import com.example.attentionspan.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: StudyViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            AttentionSpanTheme {
                AttentionSpanApp(viewModel)
            }
        }
    }
}

@Composable
fun AttentionSpanApp(viewModel: StudyViewModel) {
    val navController = rememberNavController()
    val userPreferences = UserPreferences(androidx.compose.ui.platform.LocalContext.current)

    NavHost(navController = navController, startDestination = "subject_list") {
        composable("subject_list") {
            SubjectListScreen(
                viewModel = viewModel,
                onSubjectClick = { subjectId ->
                    navController.navigate("cronometer/$subjectId")
                },
                onInsightsClick = {
                    navController.navigate("stats")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }
        composable("stats") {
            StatsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                userPreferences = userPreferences,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "cronometer/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: return@composable
            CronometerScreen(
                subjectId = subjectId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
