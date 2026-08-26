package com.moondicine.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.moondicine.app.ui.screens.home.HomeScreen
import com.moondicine.app.ui.screens.upload.UploadScreen
import com.moondicine.app.ui.screens.quiz.QuizScreen
import com.moondicine.app.ui.screens.review.ReviewScreen
import com.moondicine.app.ui.screens.stats.StatsScreen
import com.moondicine.app.ui.screens.onboarding.OnboardingScreen
import com.moondicine.app.ui.screens.quiz.QuizResultScreen
import com.moondicine.app.ui.screens.exams.ExamBrowserScreen
import com.moondicine.app.ui.screens.specialties.SpecialtyBrowserScreen
import com.moondicine.app.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Upload : Screen("upload")
    data object QuizSetup : Screen("quiz_setup")
    data object Quiz : Screen("quiz/{quizType}/{specialtyFilter}/{examSource}/{questionCount}/{quizMode}") {
        fun createRoute(
            quizType: String,
            specialtyFilter: String = "all",
            questionCount: Int = 10,
            examSource: String = "all",
            quizMode: String = "teste"
        ): String {
            return "quiz/$quizType/$specialtyFilter/${Uri.encode(examSource)}/$questionCount/$quizMode"
        }
    }
    data object QuizResult : Screen("quiz_result/{totalQuestions}/{correctAnswers}/{timeSpent}") {
        fun createRoute(totalQuestions: Int, correctAnswers: Int, timeSpent: Int): String {
            return "quiz_result/$totalQuestions/$correctAnswers/$timeSpent"
        }
    }
    data object Review : Screen("review")
    data object Exams : Screen("exams")
    data object Specialties : Screen("specialties")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Início", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Review, "Revisão", Icons.Filled.FavoriteBorder, Icons.Outlined.FavoriteBorder),
    BottomNavItem(Screen.Stats, "Estatísticas", Icons.Filled.BarChart, Icons.Outlined.BarChart)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoonDiceNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if bottom bar should be shown
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onCompleted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onStartQuiz = { quizType, specialty, count, mode ->
                        navController.navigate(Screen.Quiz.createRoute(quizType, specialty, count, quizMode = mode))
                    },
                    onSelectExam = {
                        navController.navigate(Screen.Exams.route)
                    },
                    onSelectSpecialty = {
                        navController.navigate(Screen.Specialties.route)
                    },
                    onUploadClick = {
                        navController.navigate(Screen.Upload.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Upload.route) {
                UploadScreen(
                    onUploadComplete = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.QuizSetup.route) {
                // Quiz setup is integrated into HomeScreen
                navController.navigate(Screen.Home.route)
            }

            composable(
                route = Screen.Quiz.route,
                arguments = listOf(
                    navArgument("quizType") { type = NavType.StringType },
                    navArgument("specialtyFilter") { type = NavType.StringType; defaultValue = "all" },
                    navArgument("examSource") { type = NavType.StringType; defaultValue = "all" },
                    navArgument("questionCount") { type = NavType.IntType; defaultValue = 10 },
                    navArgument("quizMode") { type = NavType.StringType; defaultValue = "teste" }
                )
            ) { backStackEntry ->
                val quizType = backStackEntry.arguments?.getString("quizType") ?: "quick"
                val specialtyFilter = backStackEntry.arguments?.getString("specialtyFilter") ?: "all"
                val examSource = backStackEntry.arguments?.getString("examSource") ?: "all"
                val questionCount = backStackEntry.arguments?.getInt("questionCount") ?: 10
                val quizMode = backStackEntry.arguments?.getString("quizMode") ?: "teste"

                QuizScreen(
                    quizType = quizType,
                    specialtyFilter = specialtyFilter,
                    examSource = examSource,
                    questionCount = questionCount,
                    quizMode = quizMode,
                    onQuizFinished = { total, correct, time ->
                        navController.navigate(Screen.QuizResult.createRoute(total, correct, time)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.QuizResult.route,
                arguments = listOf(
                    navArgument("totalQuestions") { type = NavType.IntType },
                    navArgument("correctAnswers") { type = NavType.IntType },
                    navArgument("timeSpent") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val total = backStackEntry.arguments?.getInt("totalQuestions") ?: 0
                val correct = backStackEntry.arguments?.getInt("correctAnswers") ?: 0
                val time = backStackEntry.arguments?.getInt("timeSpent") ?: 0

                QuizResultScreen(
                    totalQuestions = total,
                    correctAnswers = correct,
                    timeSpent = time,
                    onBackToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onReviewAnswers = {
                        navController.navigate(Screen.Review.route)
                    }
                )
            }

            composable(Screen.Review.route) {
                ReviewScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Exams.route) {
                ExamBrowserScreen(
                    onBackClick = { navController.popBackStack() },
                    onStartExam = { examSource, questionCount, mode ->
                        navController.navigate(
                            Screen.Quiz.createRoute(
                                quizType = "exam",
                                questionCount = questionCount,
                                examSource = examSource,
                                quizMode = mode
                            )
                        )
                    }
                )
            }

            composable(Screen.Specialties.route) {
                SpecialtyBrowserScreen(
                    onBackClick = { navController.popBackStack() },
                    onStartSpecialty = { specialty, questionCount, mode ->
                        navController.navigate(
                            Screen.Quiz.createRoute(
                                quizType = "specialty",
                                specialtyFilter = specialty,
                                questionCount = questionCount,
                                quizMode = mode
                            )
                        )
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
