package com.forma.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.forma.app.domain.model.UserProfile
import com.forma.app.domain.repository.UserProfileRepository
import com.forma.app.presentation.screens.exercisedetail.ExerciseDetailScreen
import com.forma.app.presentation.screens.home.HomeScreen
import com.forma.app.presentation.screens.onboarding.OnboardingScreen
import com.forma.app.presentation.screens.progress.ProgressScreen
import com.forma.app.presentation.screens.review.ReviewScreen
import com.forma.app.presentation.screens.settings.SettingsScreen
import com.forma.app.presentation.screens.welcome.WelcomeScreen
import com.forma.app.presentation.screens.workout.WorkoutScreen
import com.forma.app.presentation.screens.workoutdetail.WorkoutDetailScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

@HiltViewModel
class RootViewModel @Inject constructor(
    profileRepo: UserProfileRepository
) : ViewModel() {
    val profile: StateFlow<UiState> = profileRepo.observeProfile()
        .let { flow ->
            kotlinx.coroutines.flow.flow {
                emit(UiState.Loading)
                flow.collect { p ->
                    emit(if (p != null) UiState.Ready(p) else UiState.NoProfile)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    sealed interface UiState {
        data object Loading : UiState
        data object NoProfile : UiState
        data class Ready(val profile: UserProfile) : UiState
    }
}

@Composable
fun FormaNavGraph(
    navController: NavHostController = rememberNavController(),
    vm: RootViewModel = hiltViewModel()
) {
    val state by vm.profile.collectAsState()

    val startRoute = when (state) {
        is RootViewModel.UiState.Ready -> Route.Home.path
        RootViewModel.UiState.NoProfile -> Route.Welcome.path
        RootViewModel.UiState.Loading -> Route.Home.path
    }

    NavHost(navController = navController, startDestination = startRoute) {
        composable(Route.Welcome.path) {
            WelcomeScreen(
                onChooseAi = {
                    navController.navigate(Route.Onboarding.path)
                },
                onPresetReady = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Welcome.path) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Onboarding.path) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Welcome.path) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Home.path) {
            HomeScreen(
                onOpenWorkout = { workoutId ->
                    navController.navigate(Route.WorkoutDetail.build(workoutId))
                },
                onOpenProgress = {
                    navController.navigate(Route.Progress.path)
                },
                onOpenSettings = {
                    navController.navigate(Route.Settings.path)
                }
            )
        }
        composable(Route.Progress.path) {
            ProgressScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.Settings.path) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Route.WorkoutDetail.path,
            arguments = listOf(navArgument(Route.WorkoutDetail.ARG) { type = NavType.StringType })
        ) {
            val args = it.arguments
            val workoutId = args?.getString(Route.WorkoutDetail.ARG) ?: return@composable
            WorkoutDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenExercise = { _, exerciseId ->
                    navController.navigate(Route.ExerciseDetail.build(workoutId, exerciseId))
                },
                onSessionStarted = { sessionId ->
                    navController.navigate(Route.Workout.build(sessionId)) {
                        // После старта сессии не возвращаемся на детали — popUpTo до Home
                        popUpTo(Route.Home.path)
                    }
                }
            )
        }
        composable(
            Route.ExerciseDetail.path,
            arguments = listOf(
                navArgument(Route.ExerciseDetail.ARG_WORKOUT) { type = NavType.StringType },
                navArgument(Route.ExerciseDetail.ARG_EXERCISE) { type = NavType.StringType }
            )
        ) {
            ExerciseDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Route.Workout.path,
            arguments = listOf(navArgument(Route.Workout.ARG) { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString(Route.Workout.ARG) ?: return@composable
            WorkoutScreen(
                sessionId = id,
                onBack = { navController.popBackStack() },
                onFinished = { reviewId ->
                    if (reviewId != null) {
                        navController.navigate(Route.Review.build(reviewId)) {
                            // После завершения сессии — на разбор, не возвращаемся в активную тренировку
                            popUpTo(Route.Home.path)
                        }
                    } else {
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.Home.path) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            Route.Review.path,
            arguments = listOf(navArgument(Route.Review.ARG) { type = NavType.StringType })
        ) {
            ReviewScreen(
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Home.path) { inclusive = true }
                    }
                }
            )
        }
    }
}
