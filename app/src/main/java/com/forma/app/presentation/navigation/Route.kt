package com.forma.app.presentation.navigation

sealed class Route(val path: String) {
    data object Welcome : Route("welcome")
    data object Onboarding : Route("onboarding")
    data object Home : Route("home")

    /** Детали тренировки (день программы) — список упражнений, замена, инструкции. */
    data object WorkoutDetail : Route("workout-detail/{workoutId}") {
        fun build(workoutId: String) = "workout-detail/$workoutId"
        const val ARG = "workoutId"
    }

    /** Детали отдельного упражнения — описание, техника, видео в будущем. */
    data object ExerciseDetail : Route("exercise-detail/{workoutId}/{exerciseId}") {
        fun build(workoutId: String, exerciseId: String) =
            "exercise-detail/$workoutId/$exerciseId"
        const val ARG_WORKOUT = "workoutId"
        const val ARG_EXERCISE = "exerciseId"
    }

    /** Активная сессия тренировки. */
    data object Workout : Route("workout/{sessionId}") {
        fun build(sessionId: String) = "workout/$sessionId"
        const val ARG = "sessionId"
    }

    /** Экран графиков прогресса. */
    data object Progress : Route("progress")

    /** AI-разбор после тренировки. */
    data object Review : Route("review/{reviewId}") {
        fun build(reviewId: String) = "review/$reviewId"
        const val ARG = "reviewId"
    }

    /** Настройки приложения. */
    data object Settings : Route("settings")
}
