package com.forma.app.presets

import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.ExperienceLevel
import com.forma.app.domain.model.Goal
import com.forma.app.domain.model.MuscleGroup
import com.forma.app.domain.model.Program
import com.forma.app.domain.model.Sex
import com.forma.app.domain.model.TargetSet
import com.forma.app.domain.model.UserProfile
import com.forma.app.domain.model.Workout
import java.util.UUID

/**
 * Готовая программа пользователя — Push/Pull сплит на 3 дня.
 * Используется как preset, без обращения к OpenAI.
 *
 * Структура:
 *  - Понедельник: основной (ширина + бицепс + дельта)
 *  - Среда: лёгкий (техника + памп)
 *  - Пятница: силовой (жим + закрепление)
 */
object MyProgramPreset {

    /** Стартовый профиль для preset-программы. */
    val profile: UserProfile = UserProfile(
        goal = Goal.MUSCLE_GAIN,
        level = ExperienceLevel.INTERMEDIATE,
        sex = Sex.MALE,
        daysPerWeek = 3,
        preferredDays = listOf(DayOfWeek.MON, DayOfWeek.WED, DayOfWeek.FRI),
        equipment = listOf(
            Equipment.BARBELL,
            Equipment.DUMBBELLS,
            Equipment.MACHINES,
            Equipment.PULLUP_BAR,
            Equipment.BENCH,
            Equipment.CABLES,
            Equipment.BODYWEIGHT
        ),
        heightCm = null,
        weightKg = null,
        age = null,
        limitations = null,
        sessionDurationMin = 60
    )

    fun build(): Program {
        val programId = UUID.randomUUID().toString()

        val monday = workoutMonday(programId)
        val wednesday = workoutWednesday(programId)
        val friday = workoutFriday(programId)

        return Program(
            id = programId,
            name = "Моя программа",
            description = "Push/Pull сплит на 3 дня. Понедельник — основной (ширина + бицепс), среда — лёгкий (техника + памп), пятница — силовой (жим + закрепление).",
            createdAt = System.currentTimeMillis(),
            weekNumber = 1,
            profileSnapshot = profile,
            workouts = listOf(monday, wednesday, friday)
        )
    }

    // ───────────────────────────────────────────────────────────
    // ПОНЕДЕЛЬНИК — основной (ширина + бицепс + дельта)
    // ───────────────────────────────────────────────────────────
    private fun workoutMonday(programId: String): Workout {
        val workoutId = UUID.randomUUID().toString()

        val exercises = listOf(

            // 1. Средняя дельта: 9×12 RIR2, 9×10-12 RIR1-2, опц. 9×10-12
            Exercise(
                name = "Средняя дельта (махи в стороны)",
                description = "Махи гантелями через стороны для средней дельты. Локти чуть согнуты и ведут движение, плечи не поднимай к ушам. Корпус держи почти неподвижным, без разгона гантелей спиной.",
                primaryMuscle = MuscleGroup.SHOULDERS,
                secondaryMuscles = emptyList(),
                equipment = listOf(Equipment.DUMBBELLS),
                targetSets = 3,
                targetRepsMin = 10, targetRepsMax = 12,
                restSeconds = 90,
                usesWeight = true,
                startingWeightKg = 9.0,
                notes = "Не подключай трапецию — плечи вниз.",
                orderIndex = 0,
                targetSetsDetailed = listOf(
                    TargetSet(1, 9.0, 12, 12, rirTarget = 2),
                    TargetSet(2, 9.0, 10, 12, rirTarget = 1),
                    TargetSet(3, 9.0, 10, 12, isOptional = true)
                )
            ),

            // 2. Вертикальная тяга: 45×10 RIR2, 45×8-9 RIR1-2, 41×8 RIR1
            Exercise(
                name = "Вертикальная тяга (верхний блок)",
                description = "Тяга верхнего блока к груди для широчайших и верхней части спины. Сначала опусти плечи вниз, затем тяни локтями к корпусу, а не кистями. Держи грудь открытой и не раскачивайся назад ради веса.",
                primaryMuscle = MuscleGroup.BACK,
                secondaryMuscles = listOf(MuscleGroup.BICEPS),
                equipment = listOf(Equipment.CABLES, Equipment.MACHINES),
                targetSets = 3,
                targetRepsMin = 8, targetRepsMax = 10,
                restSeconds = 120,
                usesWeight = true,
                startingWeightKg = 45.0,
                notes = "Тяни локтями вниз, не руками.",
                orderIndex = 1,
                targetSetsDetailed = listOf(
                    TargetSet(1, 45.0, 10, 10, rirTarget = 2),
                    TargetSet(2, 45.0, 8, 9, rirTarget = 1),
                    TargetSet(3, 41.0, 8, 8, rirTarget = 1)
                )
            ),

            // 3. Горизонтальная тяга: 54-59×10 RIR2, 54-59×8-9 RIR1
            Exercise(
                name = "Горизонтальная тяга",
                description = "Горизонтальная тяга в тренажёре для середины спины и широчайших. Тяни рукоять к низу живота, грудь держи открытой, поясницу нейтральной. Не откидывайся корпусом назад на каждом повторе.",
                primaryMuscle = MuscleGroup.BACK,
                secondaryMuscles = listOf(MuscleGroup.BICEPS),
                equipment = listOf(Equipment.CABLES, Equipment.MACHINES),
                targetSets = 2,
                targetRepsMin = 8, targetRepsMax = 10,
                restSeconds = 120,
                usesWeight = true,
                startingWeightKg = 54.0,
                notes = "Рабочий диапазон 54–59 кг. Грудь раскрыта, спина прямая.",
                orderIndex = 2,
                targetSetsDetailed = listOf(
                    TargetSet(1, 54.0, 10, 10, rirTarget = 2),
                    TargetSet(2, 54.0, 8, 9, rirTarget = 1)
                )
            ),

            // 4. Бицепс (основной): 8-10, 8-10, опц. 10-12
            Exercise(
                name = "Бицепс (сгибания)",
                description = "Сгибания на бицепс с гантелями или штангой. Локти держи почти на месте рядом с корпусом, без рывка спиной. Опускай вес медленно — негативная фаза здесь так же важна, как подъём.",
                primaryMuscle = MuscleGroup.BICEPS,
                secondaryMuscles = listOf(MuscleGroup.FOREARMS),
                equipment = listOf(Equipment.DUMBBELLS, Equipment.BARBELL),
                targetSets = 3,
                targetRepsMin = 8, targetRepsMax = 12,
                restSeconds = 90,
                usesWeight = true,
                startingWeightKg = 10.0,
                notes = "Без рывка корпусом. Медленный негатив.",
                orderIndex = 3,
                targetSetsDetailed = listOf(
                    TargetSet(1, 10.0, 8, 10),
                    TargetSet(2, 10.0, 8, 10),
                    TargetSet(3, 10.0, 10, 12, isOptional = true)
                )
            ),

            // 5. Face Pull: 12-15, 12-15
            Exercise(
                name = "Face Pull",
                description = "Тяга каната к лицу для задней дельты, наружных ротаторов плеча и верха спины. Держи локти высоко, плечи опущены, шею свободной. В конце мягко разворачивай кисти назад, без рывка корпусом.",
                primaryMuscle = MuscleGroup.SHOULDERS,
                secondaryMuscles = listOf(MuscleGroup.BACK),
                equipment = listOf(Equipment.CABLES),
                targetSets = 2,
                targetRepsMin = 12, targetRepsMax = 15,
                restSeconds = 60,
                usesWeight = true,
                startingWeightKg = null, // подбираешь сам
                notes = "Памповый формат, цель — заднюю дельту прокачать.",
                orderIndex = 4
            ),

            // 6. Присед: 50×8, 60×8
            Exercise(
                name = "Присед",
                description = "Присед со штангой на спине для квадрицепсов, ягодиц и корпуса. Перед спуском вдохни и напряги живот по кругу, стопы держи полностью в полу. Колени веди по направлению носков, а глубину выбирай такую, где спина остаётся нейтральной.",
                primaryMuscle = MuscleGroup.QUADS,
                secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
                equipment = listOf(Equipment.BARBELL),
                targetSets = 2,
                targetRepsMin = 8, targetRepsMax = 8,
                restSeconds = 150,
                usesWeight = true,
                startingWeightKg = 50.0,
                notes = "Лёгкий разгрузочный вариант, не до отказа.",
                orderIndex = 5,
                targetSetsDetailed = listOf(
                    TargetSet(1, 50.0, 8, 8),
                    TargetSet(2, 60.0, 8, 8)
                )
            ),

            // 7. Пресс: 2 подхода (вес)
            Exercise(
                name = "Пресс с весом",
                description = "Скручивания или подъёмы с отягощением для мышц живота. Думай о подкрутке таза и сближении рёбер с тазом, а не о подъёме головы. Вес добавляй только если поясница и шея не забирают движение.",
                primaryMuscle = MuscleGroup.CORE,
                secondaryMuscles = emptyList(),
                equipment = listOf(Equipment.BODYWEIGHT, Equipment.CABLES),
                targetSets = 2,
                targetRepsMin = 12, targetRepsMax = 15,
                restSeconds = 60,
                usesWeight = true,
                startingWeightKg = null,
                notes = "Работай через подкрутку таза, не тяни шеей.",
                orderIndex = 6
            )
        )

        return Workout(
            id = workoutId,
            programId = programId,
            dayOfWeek = DayOfWeek.MON,
            title = "Понедельник — основной",
            focus = "Спина · Дельты · Бицепс",
            estimatedMinutes = 75,
            exercises = exercises.withContentIds()
        )
    }

    // ───────────────────────────────────────────────────────────
    // СРЕДА — лёгкий (техника + памп)
    // ───────────────────────────────────────────────────────────
    private fun workoutWednesday(programId: String): Workout {
        val workoutId = UUID.randomUUID().toString()

        val exercises = listOf(

            // 1. Средняя дельта: 9×12-15, 9×12-15
            Exercise(
                name = "Средняя дельта (махи в стороны)",
                description = "Памповый вариант махов для средней дельты. Поднимай локти в стороны чуть перед корпусом, плечи держи вниз. Вес должен позволять чистую амплитуду без раскачки и зажима в шее.",
                primaryMuscle = MuscleGroup.SHOULDERS,
                secondaryMuscles = emptyList(),
                equipment = listOf(Equipment.DUMBBELLS),
                targetSets = 2,
                targetRepsMin = 12, targetRepsMax = 15,
                restSeconds = 60,
                usesWeight = true,
                startingWeightKg = 9.0,
                notes = "Памп. Можно делать суперсетом с face pull.",
                orderIndex = 0,
                targetSetsDetailed = listOf(
                    TargetSet(1, 9.0, 12, 15),
                    TargetSet(2, 9.0, 12, 15)
                )
            ),

            // 2. Вертикальная тяга: 45×10, 45×8
            Exercise(
                name = "Вертикальная тяга (верхний блок)",
                description = "Лёгкий технический вариант тяги верхнего блока к груди. Сначала опусти плечи вниз, затем тяни локтями к корпусу. Не гонись за весом: цель — почувствовать спину без раскачки и отказа.",
                primaryMuscle = MuscleGroup.BACK,
                secondaryMuscles = listOf(MuscleGroup.BICEPS),
                equipment = listOf(Equipment.CABLES, Equipment.MACHINES),
                targetSets = 2,
                targetRepsMin = 8, targetRepsMax = 10,
                restSeconds = 90,
                usesWeight = true,
                startingWeightKg = 45.0,
                notes = "Без отказа. Чувствуй спину, не тяни руками.",
                orderIndex = 1,
                targetSetsDetailed = listOf(
                    TargetSet(1, 45.0, 10, 10),
                    TargetSet(2, 45.0, 8, 8)
                )
            ),

            // 3. Горизонтальная тяга: 54×10, 54×8
            Exercise(
                name = "Горизонтальная тяга",
                description = "Лёгкий технический вариант горизонтальной тяги. Держи грудь открытой, поясницу нейтральной, тяни локтями назад. В конце повтора сделай короткую паузу без откидывания корпусом.",
                primaryMuscle = MuscleGroup.BACK,
                secondaryMuscles = listOf(MuscleGroup.BICEPS),
                equipment = listOf(Equipment.CABLES, Equipment.MACHINES),
                targetSets = 2,
                targetRepsMin = 8, targetRepsMax = 10,
                restSeconds = 90,
                usesWeight = true,
                startingWeightKg = 54.0,
                notes = "Сведи лопатки, удержи на пиковом сокращении.",
                orderIndex = 2,
                targetSetsDetailed = listOf(
                    TargetSet(1, 54.0, 10, 10),
                    TargetSet(2, 54.0, 8, 8)
                )
            ),

            // 4. Жим лёжа: 60×6-8, опц. 60×5
            Exercise(
                name = "Жим лёжа",
                description = "Жим штанги лёжа для груди, трицепса и передней дельты. Перед снятием грифа сведи и опусти лопатки, стопы плотно поставь в пол. Опускай штангу к нижней части груди, локти держи примерно 45-70° от корпуса.",
                primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                equipment = listOf(Equipment.BARBELL, Equipment.BENCH),
                targetSets = 2,
                targetRepsMin = 5, targetRepsMax = 8,
                restSeconds = 150,
                usesWeight = true,
                startingWeightKg = 60.0,
                notes = "Лёгкий день — сохраняй технику, не гонись за повторами.",
                orderIndex = 3,
                targetSetsDetailed = listOf(
                    TargetSet(1, 60.0, 6, 8),
                    TargetSet(2, 60.0, 5, 5, isOptional = true)
                )
            ),

            // 5. Наклонный жим: 20×10, 20×8
            Exercise(
                name = "Наклонный жим (гантели)",
                description = "Жим гантелей на наклонной скамье для верхней части груди и передней дельты. Угол скамьи держи около 20-35°, лопатки сведи и опусти. Опускай гантели до комфортной глубины без боли в передней части плеча.",
                primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                equipment = listOf(Equipment.DUMBBELLS, Equipment.BENCH),
                targetSets = 2,
                targetRepsMin = 8, targetRepsMax = 10,
                restSeconds = 120,
                usesWeight = true,
                startingWeightKg = 20.0,
                notes = "Угол ~30°, лопатки сведены.",
                orderIndex = 4,
                targetSetsDetailed = listOf(
                    TargetSet(1, 20.0, 10, 10),
                    TargetSet(2, 20.0, 8, 8)
                )
            ),

            // 6. Face Pull: 12-15, 12-15
            Exercise(
                name = "Face Pull",
                description = "Памповая тяга каната к лицу для задней дельты и верха спины. Локти держи высоко, плечи вниз, корпус неподвижен. В конце мягко разворачивай кисти назад и не тяни шеей.",
                primaryMuscle = MuscleGroup.SHOULDERS,
                secondaryMuscles = listOf(MuscleGroup.BACK),
                equipment = listOf(Equipment.CABLES),
                targetSets = 2,
                targetRepsMin = 12, targetRepsMax = 15,
                restSeconds = 60,
                usesWeight = true,
                notes = "Локти высоко.",
                orderIndex = 5
            ),

            // 7. Бицепс (лёгкий): 12, 10
            Exercise(
                name = "Бицепс (лёгкий)",
                description = "Лёгкие сгибания на бицепс с акцентом на технику и памп. Локти держи рядом с корпусом, плечи не поднимай. Поднимай вес без рывка и медленно контролируй опускание.",
                primaryMuscle = MuscleGroup.BICEPS,
                secondaryMuscles = listOf(MuscleGroup.FOREARMS),
                equipment = listOf(Equipment.DUMBBELLS, Equipment.BARBELL),
                targetSets = 2,
                targetRepsMin = 10, targetRepsMax = 12,
                restSeconds = 60,
                usesWeight = true,
                notes = "Лёгкий вес, контроль на негативе.",
                orderIndex = 6,
                targetSetsDetailed = listOf(
                    TargetSet(1, null, 12, 12),
                    TargetSet(2, null, 10, 10)
                )
            ),

            // 8. Пресс: 2 подхода
            Exercise(
                name = "Пресс",
                description = "Скручивания для мышц живота без отягощения. Думай о подкрутке таза и выдохе, а не о рывке головой. Шея расслаблена, поясница не должна забирать движение.",
                primaryMuscle = MuscleGroup.CORE,
                secondaryMuscles = emptyList(),
                equipment = listOf(Equipment.BODYWEIGHT),
                targetSets = 2,
                targetRepsMin = 12, targetRepsMax = 20,
                restSeconds = 60,
                usesWeight = false,
                notes = "Подкрутка таза, не тяни шеей.",
                orderIndex = 7
            )
        )

        return Workout(
            id = workoutId,
            programId = programId,
            dayOfWeek = DayOfWeek.WED,
            title = "Среда — лёгкий",
            focus = "Грудь · Спина · Памп",
            estimatedMinutes = 60,
            exercises = exercises.withContentIds()
        )
    }

    // ───────────────────────────────────────────────────────────
    // ПЯТНИЦА — силовой (жим + закрепление)
    // ───────────────────────────────────────────────────────────
    private fun workoutFriday(programId: String): Workout {
        val workoutId = UUID.randomUUID().toString()

        val exercises = listOf(

            // 1. Жим лёжа: 40×10, 50×8, 65×5-6, 60×5-6
            Exercise(
                name = "Жим лёжа",
                description = "Силовой жим штанги лёжа с разогревом, топ-сетом и закреплением. Перед каждым рабочим подходом сведи лопатки, упри стопы в пол и собери корпус. Опускай гриф к нижней части груди без отбива, локти держи под контролем.",
                primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                equipment = listOf(Equipment.BARBELL, Equipment.BENCH),
                targetSets = 4,
                targetRepsMin = 5, targetRepsMax = 10,
                restSeconds = 180,
                usesWeight = true,
                startingWeightKg = 65.0,
                notes = "Топ-сет 65×5-6, потом откатной 60×5-6. Работай чисто.",
                orderIndex = 0,
                targetSetsDetailed = listOf(
                    TargetSet(1, 40.0, 10, 10, note = "Разогрев"),
                    TargetSet(2, 50.0, 8, 8, note = "Подводящий"),
                    TargetSet(3, 65.0, 5, 6, note = "Топ-сет"),
                    TargetSet(4, 60.0, 5, 6, note = "Закрепление")
                )
            ),

            // 2. Вертикальная тяга: 45×9-10, 45×7-8
            Exercise(
                name = "Вертикальная тяга (верхний блок)",
                description = "Вертикальная тяга после жима для баланса спины и плечевого пояса. Тяни рукоять к груди, сначала опуская плечи вниз. Не раскачивайся назад и не тяни блок за голову.",
                primaryMuscle = MuscleGroup.BACK,
                secondaryMuscles = listOf(MuscleGroup.BICEPS),
                equipment = listOf(Equipment.CABLES, Equipment.MACHINES),
                targetSets = 2,
                targetRepsMin = 7, targetRepsMax = 10,
                restSeconds = 120,
                usesWeight = true,
                startingWeightKg = 45.0,
                orderIndex = 1,
                targetSetsDetailed = listOf(
                    TargetSet(1, 45.0, 9, 10),
                    TargetSet(2, 45.0, 7, 8)
                )
            ),

            // 3. Горизонтальная тяга: 59×8-10, 59×7-8
            Exercise(
                name = "Горизонтальная тяга",
                description = "Горизонтальная тяга на более тяжёлом рабочем весе. Держи корпус стабильным, грудь открытой, тяни локтями назад. Если пауза в конце невозможна, вес уже слишком тяжёлый.",
                primaryMuscle = MuscleGroup.BACK,
                secondaryMuscles = listOf(MuscleGroup.BICEPS),
                equipment = listOf(Equipment.CABLES, Equipment.MACHINES),
                targetSets = 2,
                targetRepsMin = 7, targetRepsMax = 10,
                restSeconds = 120,
                usesWeight = true,
                startingWeightKg = 59.0,
                orderIndex = 2,
                targetSetsDetailed = listOf(
                    TargetSet(1, 59.0, 8, 10),
                    TargetSet(2, 59.0, 7, 8)
                )
            ),

            // 4. Средняя дельта: 9×10-12, 9×10-12
            Exercise(
                name = "Средняя дельта (махи в стороны)",
                description = "Махи гантелями для средней дельты с контролем амплитуды. Поднимай локти примерно до уровня плеч, плечи не зажимай к ушам. Опускай гантели медленно, без броска вниз.",
                primaryMuscle = MuscleGroup.SHOULDERS,
                secondaryMuscles = emptyList(),
                equipment = listOf(Equipment.DUMBBELLS),
                targetSets = 2,
                targetRepsMin = 10, targetRepsMax = 12,
                restSeconds = 75,
                usesWeight = true,
                startingWeightKg = 9.0,
                orderIndex = 3,
                targetSetsDetailed = listOf(
                    TargetSet(1, 9.0, 10, 12),
                    TargetSet(2, 9.0, 10, 12)
                )
            ),

            // 5. Наклонный жим: 22×8-10 (1 сет)
            Exercise(
                name = "Наклонный жим (гантели)",
                description = "Один рабочий сет наклонного жима гантелей на закрепление. Лопатки сведи, стопы держи в полу, гантели опускай до комфортной глубины. Сохраняй RIR и не превращай сет в отказный максимум.",
                primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                equipment = listOf(Equipment.DUMBBELLS, Equipment.BENCH),
                targetSets = 1,
                targetRepsMin = 8, targetRepsMax = 10,
                restSeconds = 120,
                usesWeight = true,
                startingWeightKg = 22.0,
                notes = "Один сет, RIR 2.",
                orderIndex = 4,
                targetSetsDetailed = listOf(
                    TargetSet(1, 22.0, 8, 10, rirTarget = 2)
                )
            ),

            // 6. Бицепс: 10×8-10, 10×8-10
            Exercise(
                name = "Бицепс (сгибания)",
                description = "Сгибания на бицепс со стабильным рабочим весом. Локти держи почти неподвижно, корпус не раскачивай. Опускай гантели медленно и сохраняй одинаковую траекторию в каждом повторе.",
                primaryMuscle = MuscleGroup.BICEPS,
                secondaryMuscles = listOf(MuscleGroup.FOREARMS),
                equipment = listOf(Equipment.DUMBBELLS),
                targetSets = 2,
                targetRepsMin = 8, targetRepsMax = 10,
                restSeconds = 90,
                usesWeight = true,
                startingWeightKg = 10.0,
                orderIndex = 5,
                targetSetsDetailed = listOf(
                    TargetSet(1, 10.0, 8, 10),
                    TargetSet(2, 10.0, 8, 10)
                )
            ),

            // 7. Трицепс: 1-2 сета
            Exercise(
                name = "Трицепс (разгибания)",
                description = "Разгибания на трицепс на блоке или с гантелями. Локти держи стабильными, движение выполняй в локтевом суставе без раскачки плечами. Если появляется резкая боль в локте, снизь вес или останови упражнение.",
                primaryMuscle = MuscleGroup.TRICEPS,
                secondaryMuscles = emptyList(),
                equipment = listOf(Equipment.CABLES, Equipment.DUMBBELLS),
                targetSets = 2,
                targetRepsMin = 10, targetRepsMax = 12,
                restSeconds = 75,
                usesWeight = true,
                notes = "1-2 рабочих сета. Подбери комфортный вес.",
                orderIndex = 6,
                targetSetsDetailed = listOf(
                    TargetSet(1, null, 10, 12),
                    TargetSet(2, null, 10, 12, isOptional = true)
                )
            ),

            // 8. Пресс с весом: 2 подхода
            Exercise(
                name = "Пресс с весом",
                description = "Скручивания с отягощением для мышц живота. Сначала подкрути таз и опусти рёбра, затем добавляй вес. Не тяни шеей и не продолжай, если нагрузка уходит в поясницу.",
                primaryMuscle = MuscleGroup.CORE,
                secondaryMuscles = emptyList(),
                equipment = listOf(Equipment.BODYWEIGHT, Equipment.CABLES),
                targetSets = 2,
                targetRepsMin = 12, targetRepsMax = 15,
                restSeconds = 60,
                usesWeight = true,
                notes = "Подкрутка таза, фокус на жжение.",
                orderIndex = 7
            )
        )

        return Workout(
            id = workoutId,
            programId = programId,
            dayOfWeek = DayOfWeek.FRI,
            title = "Пятница — силовой",
            focus = "Жим · Спина · Закрепление",
            estimatedMinutes = 80,
            exercises = exercises.withContentIds()
        )
    }

    private fun List<Exercise>.withContentIds(): List<Exercise> = map { exercise ->
        exercise.copy(contentId = contentIdForName(exercise.name))
    }

    private fun contentIdForName(name: String): String? = when (name) {
        "Средняя дельта (махи в стороны)" -> "lateral-raise"
        "Вертикальная тяга (верхний блок)" -> "lat-pulldown"
        "Горизонтальная тяга" -> "seated-row"
        "Бицепс (сгибания)" -> "biceps-curl"
        "Бицепс (лёгкий)" -> "biceps-curl"
        "Face Pull" -> "face-pull"
        "Присед" -> "squat"
        "Пресс с весом" -> "weighted-abs"
        "Пресс" -> "abs"
        "Жим лёжа" -> "bench-press"
        "Наклонный жим (гантели)" -> "incline-db-press"
        "Трицепс (разгибания)" -> "triceps-extension"
        else -> null
    }
}
