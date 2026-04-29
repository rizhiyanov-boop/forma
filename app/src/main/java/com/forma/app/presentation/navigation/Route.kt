package com.forma.app.presentation.navigation

sealed class Route(val path: String) {
    companion object {
        const val ARG_SESSION_ID = "sessionId"
        const val ARG_EXERCISE_ID = "exerciseId"
    }

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
    data object Workout : Route("workout/{$ARG_SESSION_ID}") {
        fun build(sessionId: String) = "workout/$sessionId"
    }

    data object SetEntry : Route("set-entry/{$ARG_SESSION_ID}/{setId}/{$ARG_EXERCISE_ID}/{restSeconds}") {
        fun build(sessionId: String, setId: String, exerciseId: String, restSeconds: Int) =
            "set-entry/$sessionId/$setId/$exerciseId/$restSeconds"
        const val ARG_SET = "setId"
        const val ARG_REST = "restSeconds"
    }

    data object Rest : Route("rest/{$ARG_SESSION_ID}/{$ARG_EXERCISE_ID}/{seconds}") {
        fun build(sessionId: String, exerciseId: String, seconds: Int) =
            "rest/$sessionId/$exerciseId/$seconds"
        const val ARG_SECONDS = "seconds"
    }

    data object PostWorkoutWellness : Route("post-wellness/{$ARG_SESSION_ID}") {
        fun build(sessionId: String) = "post-wellness/$sessionId"
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
