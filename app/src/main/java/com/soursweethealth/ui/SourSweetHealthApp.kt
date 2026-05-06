package com.soursweethealth.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soursweethealth.ui.screens.*

@Composable
fun SourSweetHealthApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(viewModel = viewModel, navController = navController)
        }
        composable("add_record") {
            AddRecordScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("llm_settings") {
            LlmSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("health_analysis") {
            HealthAnalysisScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
