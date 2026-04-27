package com.forma.app.presentation.screens.workout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.forma.app.domain.model.ExerciseFeedback

@Composable
fun FeedbackScreen(
    onSubmit: (ExerciseFeedback) -> Unit
) {
    var pump by remember { mutableStateOf(3f) }
    var fatigue by remember { mutableStateOf(3f) }
    var pain by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {

        Text("Как прошло упражнение?", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Памп: ${pump.toInt()}")
        Slider(
            value = pump,
            onValueChange = { pump = it },
            valueRange = 1f..5f,
            steps = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Усталость: ${fatigue.toInt()}")
        Slider(
            value = fatigue,
            onValueChange = { fatigue = it },
            valueRange = 1f..5f,
            steps = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = pain, onCheckedChange = { pain = it })
            Text("Есть боль")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                onSubmit(
                    ExerciseFeedback(
                        pump = pump.toInt(),
                        fatigue = fatigue.toInt(),
                        pain = pain
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Продолжить")
        }
    }
}