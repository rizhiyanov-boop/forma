package com.forma.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.forma.app.presentation.screens.wellness.PostWorkoutWellnessScreen
import com.forma.app.presentation.screens.workout.RestScreen
import com.forma.app.presentation.screens.workout.SetEntryScreen
import com.forma.app.presentation.screens.workout.WorkoutScreen
import com.forma.app.presentation.screens.workoutdetail.WorkoutDetailScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

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
                onChooseAi = { navController.navigate(Route.Onboarding.path) },
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
                onOpenWorkout = { workoutId -> navController.navigate(Route.WorkoutDetail.build(workoutId)) },
                onOpenProgress = { navController.navigate(Route.Progress.path) },
                onOpenSettings = { navController.navigate(Route.Settings.path) }
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
            val workoutId = it.arguments?.getString(Route.WorkoutDetail.ARG) ?: return@composable
            WorkoutDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenExercise = { _, exerciseId ->
                    navController.navigate(Route.ExerciseDetail.build(workoutId, exerciseId))
                },
                onSessionStarted = { sessionId ->
                    navController.navigate(Route.Workout.build(sessionId)) {
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
            arguments = listOf(navArgument(Route.ARG_SESSION_ID) { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString(Route.ARG_SESSION_ID) ?: return@composable
            val workoutEntry = remember(back) { navController.getBackStackEntry(Route.Workout.build(id)) }
            val sharedWorkoutVm: com.forma.app.presentation.screens.workout.WorkoutViewModel =
                hiltViewModel(workoutEntry)
            WorkoutScreen(
                sessionId = id,
                vm = sharedWorkoutVm,
                onBack = { navController.popBackStack() },
                onOpenSetEntry = { setId, exerciseId, rest ->
                    navController.navigate(Route.SetEntry.build(id, setId, exerciseId, rest))
                },
                onFinished = { sessionId ->
                    navController.navigate(Route.PostWorkoutWellness.build(sessionId)) {
                        popUpTo(Route.Home.path)
                    }
                }
            )
        }
        composable(
            Route.SetEntry.path,
            arguments = listOf(
                navArgument(Route.ARG_SESSION_ID) { type = NavType.StringType },
                navArgument(Route.SetEntry.ARG_SET) { type = NavType.StringType },
                navArgument(Route.ARG_EXERCISE_ID) { type = NavType.StringType },
                navArgument(Route.SetEntry.ARG_REST) { type = NavType.IntType }
            )
        ) { back ->
            val sessionId = back.arguments?.getString(Route.ARG_SESSION_ID) ?: return@composable
            val setId = back.arguments?.getString(Route.SetEntry.ARG_SET) ?: return@composable
            val exerciseId = back.arguments?.getString(Route.ARG_EXERCISE_ID) ?: return@composable
            val restSeconds = back.arguments?.getInt(Route.SetEntry.ARG_REST) ?: 90
            val workoutEntry = remember(back) { navController.getBackStackEntry(Route.Workout.build(sessionId)) }
            val sharedWorkoutVm: com.forma.app.presentation.screens.workout.WorkoutViewModel =
                hiltViewModel(workoutEntry)
            SetEntryScreen(
                setId = setId,
                exerciseId = exerciseId,
                restSeconds = restSeconds,
                vm = sharedWorkoutVm,
                onBack = { navController.popBackStack() },
                onSaved = { exId, rest ->
                    navController.navigate(Route.Rest.build(sessionId, exId, rest))
                }
            )
        }
        composable(
            Route.Rest.path,
            arguments = listOf(
                navArgument(Route.ARG_SESSION_ID) { type = NavType.StringType },
                navArgument(Route.ARG_EXERCISE_ID) { type = NavType.StringType },
                navArgument(Route.Rest.ARG_SECONDS) { type = NavType.IntType }
            )
        ) { back ->
            val sessionId = back.arguments?.getString(Route.ARG_SESSION_ID) ?: return@composable
            val exerciseId = back.arguments?.getString(Route.ARG_EXERCISE_ID) ?: return@composable
            val seconds = back.arguments?.getInt(Route.Rest.ARG_SECONDS) ?: 90
            val workoutEntry = remember(back) { navController.getBackStackEntry(Route.Workout.build(sessionId)) }
            val sharedWorkoutVm: com.forma.app.presentation.screens.workout.WorkoutViewModel =
                hiltViewModel(workoutEntry)
            RestScreen(
                sessionId = sessionId,
                exerciseId = exerciseId,
                seconds = seconds,
                vm = sharedWorkoutVm,
                onBackToPlan = { navController.popBackStack(Route.Workout.build(sessionId), false) },
                onOpenNextSet = { setId, exId, rest ->
                    navController.navigate(Route.SetEntry.build(sessionId, setId, exId, rest)) {
                        popUpTo(Route.Workout.build(sessionId))
                    }
                }
            )
        }
        composable(
            Route.PostWorkoutWellness.path,
            arguments = listOf(navArgument(Route.ARG_SESSION_ID) { type = NavType.StringType })
        ) {
            PostWorkoutWellnessScreen(
                onSubmit = { reviewId ->
                    if (reviewId != null) {
                        navController.navigate(Route.Review.build(reviewId)) {
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
