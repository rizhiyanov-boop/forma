package com.forma.app.data.remote

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * JSON-схемы для OpenAI Structured Outputs.
 * Важно: в strict-режиме все свойства должны быть в required и
 * additionalProperties=false. Опциональные поля эмулируем через nullable-тип.
 */
object ProgramSchemas {

    fun programPlanSchema(): JsonElement = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", buildJsonArray {
            add("programName"); add("programDescription"); add("workouts")
        })
        put("properties", buildJsonObject {
            put("programName", strSchema("Название программы, кратко и по делу"))
            put("programDescription", strSchema("Описание программы и общей логики"))
            put("workouts", buildJsonObject {
                put("type", "array")
                put("description", "Тренировки на неделю")
                put("items", workoutSchema())
            })
        })
    }

    /**
     * Схема ответа AI с вариантами замены упражнения.
     * Возвращает 2-3 альтернативы на ту же группу мышц + объяснение почему.
     */
    fun replacementSuggestionsSchema(): JsonElement = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", buildJsonArray {
            add("suggestions"); add("reasoning")
        })
        put("properties", buildJsonObject {
            put("suggestions", buildJsonObject {
                put("type", "array")
                put("description", "От 2 до 3 альтернативных упражнений")
                put("items", exerciseSchema())
            })
            put("reasoning", strSchema("Краткое объяснение почему именно эти варианты подходят"))
        })
    }

    /**
     * Схема быстрого разбора после тренировки.
     * Возвращает вердикт + список конкретных рекомендаций.
     */
    fun quickReviewSchema(): JsonElement = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", buildJsonArray {
            add("verdict"); add("summary"); add("recommendations")
        })
        put("properties", buildJsonObject {
            put("verdict", buildJsonObject {
                put("type", "string")
                put("description", "Общий вердикт")
                put("enum", buildJsonArray {
                    add("PROGRESS"); add("STAGNATION"); add("REGRESS"); add("EARLY"); add("SOLID")
                })
            })
            put("summary", strSchema("2-3 предложения объяснение тренировки"))
            put("recommendations", buildJsonObject {
                put("type", "array")
                put("description", "От 1 до 4 конкретных рекомендаций. Если нечего менять — одна KEEP.")
                put("items", recommendationSchema())
            })
        })
    }

    private fun recommendationSchema(): JsonElement = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", buildJsonArray {
            add("type"); add("title"); add("rationale")
            add("applicableExerciseIds")
            add("newWeightKg"); add("newRepsMin"); add("newRepsMax"); add("newRestSeconds")
        })
        put("properties", buildJsonObject {
            put("type", buildJsonObject {
                put("type", "string")
                put("description", "Тип рекомендации")
                put("enum", buildJsonArray {
                    add("INCREASE_WEIGHT"); add("DECREASE_WEIGHT")
                    add("CHANGE_REPS"); add("DELOAD"); add("KEEP")
                    add("TECHNIQUE"); add("REST_LONGER"); add("REST_SHORTER"); add("INFO")
                })
            })
            put("title", strSchema("Краткое название рекомендации, императив (\"Подними жим на 2.5 кг\")"))
            put("rationale", strSchema("Обоснование 1-2 предложения с фактами из тренировки"))
            put("applicableExerciseIds", buildJsonObject {
                put("type", "array")
                put("description", "ID упражнений из текущей тренировки, к которым применима. Пустой массив для общих рекомендаций (DELOAD, INFO).")
                put("items", buildJsonObject { put("type", "string") })
            })
            put("newWeightKg", buildJsonObject {
                put("type", buildJsonArray { add("number"); add("null") })
                put("description", "Новый вес — для INCREASE_WEIGHT/DECREASE_WEIGHT, иначе null")
            })
            put("newRepsMin", buildJsonObject {
                put("type", buildJsonArray { add("integer"); add("null") })
                put("description", "Минимум повторов — для CHANGE_REPS")
            })
            put("newRepsMax", buildJsonObject {
                put("type", buildJsonArray { add("integer"); add("null") })
                put("description", "Максимум повторов — для CHANGE_REPS")
            })
            put("newRestSeconds", buildJsonObject {
                put("type", buildJsonArray { add("integer"); add("null") })
                put("description", "Новый отдых в секундах — для REST_LONGER/REST_SHORTER")
            })
        })
    }

    private fun workoutSchema(): JsonElement = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", buildJsonArray {
            add("dayOfWeek"); add("title"); add("focus")
            add("estimatedMinutes"); add("exercises")
        })
        put("properties", buildJsonObject {
            put("dayOfWeek", buildJsonObject {
                put("type", "string")
                put("enum", buildJsonArray {
                    add("MON"); add("TUE"); add("WED"); add("THU"); add("FRI"); add("SAT"); add("SUN")
                })
            })
            put("title", strSchema("Короткий заголовок тренировки"))
            put("focus", strSchema("Фокус, например 'Грудь + Трицепс'"))
            put("estimatedMinutes", intSchema("Оценочная длительность в минутах"))
            put("exercises", buildJsonObject {
                put("type", "array")
                put("items", exerciseSchema())
            })
        })
    }

    private fun exerciseSchema(): JsonElement = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        put("required", buildJsonArray {
            add("name"); add("description"); add("primaryMuscle")
            add("secondaryMuscles"); add("equipment")
            add("targetSets"); add("targetRepsMin"); add("targetRepsMax")
            add("restSeconds"); add("usesWeight"); add("notes")
        })
        put("properties", buildJsonObject {
            put("name", strSchema("Название упражнения"))
            put("description", strSchema("Как выполнять, 1-2 предложения"))
            put("primaryMuscle", muscleEnumSchema())
            put("secondaryMuscles", buildJsonObject {
                put("type", "array")
                put("items", muscleEnumSchema())
            })
            put("equipment", buildJsonObject {
                put("type", "array")
                put("items", equipmentEnumSchema())
            })
            put("targetSets", intSchema("Целевое число подходов"))
            put("targetRepsMin", intSchema("Нижняя граница повторов"))
            put("targetRepsMax", intSchema("Верхняя граница повторов"))
            put("restSeconds", intSchema("Отдых между подходами в секундах"))
            put("usesWeight", buildJsonObject { put("type", "boolean") })
            put("notes", strSchema("Техника, подсказки или пустая строка"))
        })
    }

    private fun strSchema(description: String): JsonElement = buildJsonObject {
        put("type", "string"); put("description", description)
    }

    private fun intSchema(description: String): JsonElement = buildJsonObject {
        put("type", "integer"); put("description", description)
    }

    private fun muscleEnumSchema(): JsonElement = buildJsonObject {
        put("type", "string")
        put("enum", buildJsonArray {
            add("CHEST"); add("BACK"); add("SHOULDERS"); add("BICEPS"); add("TRICEPS")
            add("FOREARMS"); add("CORE"); add("QUADS"); add("HAMSTRINGS")
            add("GLUTES"); add("CALVES"); add("FULL_BODY"); add("CARDIO")
        })
    }

    private fun equipmentEnumSchema(): JsonElement = buildJsonObject {
        put("type", "string")
        put("enum", buildJsonArray {
            add("BARBELL"); add("DUMBBELLS"); add("MACHINES"); add("PULLUP_BAR")
            add("BENCH"); add("CABLES"); add("KETTLEBELLS"); add("BODYWEIGHT"); add("CARDIO")
        })
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.add(value: String) {
    add(kotlinx.serialization.json.JsonPrimitive(value))
}
