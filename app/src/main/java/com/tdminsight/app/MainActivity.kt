package com.tdminsight.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tdminsight.app.data.AuthStore
import com.tdminsight.app.ui.AppViewModel
import com.tdminsight.app.ui.screens.CalculatorScreen
import com.tdminsight.app.ui.screens.HomeScreen
import com.tdminsight.app.ui.screens.LoginScreen
import com.tdminsight.app.ui.screens.ResultsScreen
import com.tdminsight.app.ui.theme.TDMInsightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TDMInsightTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TdmInsightApp()
                }
            }
        }
    }
}

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CALCULATOR = "calculator"
    const val RESULTS = "results"
}

@Composable
fun TdmInsightApp(navController: NavHostController = rememberNavController()) {
    val viewModel: AppViewModel = viewModel()
    val context = LocalContext.current
    val startDestination = remember { if (AuthStore.isLoggedIn(context)) Routes.HOME else Routes.LOGIN }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNewCalculation = { navController.navigate(Routes.CALCULATOR) },
                onOpenHistoryEntry = { id ->
                    viewModel.selectHistoryEntry(id)
                    navController.navigate(Routes.RESULTS)
                },
                onLogout = {
                    AuthStore.logout(context)
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.CALCULATOR) {
            CalculatorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCalculated = {
                    navController.navigate(Routes.RESULTS) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }
        composable(Routes.RESULTS) {
            ResultsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
    }
}
