# ТЗ: Этап 6 LiveCoach — picker самочувствия между упражнениями

**Тип задачи:** новая мини-фича + интеграция с существующими.
**Приёмка:** компиляция, тесты, юзер видит picker между упражнениями по правильным триггерам.
**Контекст:** см. `docs/LIVECOACH_DESIGN.md` (секция 4 «Сбор самочувствия», секция 13 этап 6).
**Зависимости:** этапы 1-5 LiveCoach должны быть закрыты (контентный пул, wellness данные, LiveCoach core, интеграция в AI). Этот этап **последний** в плане LiveCoach.

---

## Идея этапа

Сейчас приложение собирает самочувствие только в двух точках:
- **Pre-workout** — перед началом тренировки (overlay в WorkoutScreen)
- **Post-workout** — после тренировки (PostWorkoutWellnessScreen)

Это даёт нам **точечный снэпшот** на старте и итог в конце. Но между этими точками тренировка может пойти по-разному: юзер начал бодрым, но через 4 упражнения сел. Или начал уставшим и после третьего упражнения раскачался.

**Этап 6** добавляет **третью точку контакта** — picker между упражнениями. Это даёт LiveCoach живой контекст текущего состояния юзера и помогает делать более точные рекомендации в моменте.

Из дизайн-документа (секция 4):

> **Между упражнениями** (опционально, не каждый раз):
> - "Как ощущения?" — тот же picker
> - Появляется только если сильное отклонение в предыдущем упражнении или прошло >3 упражнений

Ключевое слово — **«не каждый раз»**. Picker появляться должен **редко**, только когда есть смысл его показать. Иначе юзер замучается от лишних кликов.

---

## Что НЕ входит в этап

❌ Перепроектирование уже сделанных pre-workout / post-workout flows
❌ Picker внутри упражнения (между подходами одного упражнения)
❌ Использование `betweenExerciseEnergy` в AI-разборе (это уже работает через `WellnessRepository.observeRecent`, новые записи автоматически попадут)
❌ Экран статистики самочувствия — это отдельная фича на потом

---

## Что входит — обзор

1. **B6.1** — Расширение `WellnessSnapshot` для LiveCoach: добавить поле `betweenExerciseEnergy`
2. **B6.2** — DAO-метод для чтения between-записей конкретной сессии
3. **B6.3** — Триггер-логика: когда показывать picker
4. **B6.4** — UI: реюз компонента `EnergyPicker` который уже есть в WorkoutScreen
5. **B6.5** — Интеграция в RestScreen — picker появляется при переходе на новое упражнение
6. **B6.6** — Использование собранных данных в LiveCoach
7. **B6.7** — Тесты триггер-логики

---

## B6.1. Расширение WellnessSnapshot

### Задача

В `domain/livecoach/LiveCoach.kt` модель `WellnessSnapshot` сейчас:

```kotlin
data class WellnessSnapshot(
    val preWorkoutEnergy: EnergyLevel? = null,
    val sevenDayAvgEnergy: Double? = null,
    val sevenDayAvgSleep: Double? = null
)
```

Расширить:

```kotlin
data class WellnessSnapshot(
    val preWorkoutEnergy: EnergyLevel? = null,
    val betweenExerciseEnergy: EnergyLevel? = null,    // НОВОЕ — последнее
                                                         // значение в этой сессии
    val sevenDayAvgEnergy: Double? = null,
    val sevenDayAvgSleep: Double? = null
)
```

### Логика fatigue в LiveCoach

В `LiveCoach.analyze()` есть переменная `fatiguedContext`:

```kotlin
val fatiguedContext = wellness?.preWorkoutEnergy == EnergyLevel.FATIGUED ||
    ((wellness?.sevenDayAvgEnergy ?: 2.0) < 1.8) ||
    ((wellness?.sevenDayAvgSleep ?: 3.0) < 2.5)
```

Расширить чтобы учитывать betweenExerciseEnergy с **приоритетом** над preWorkoutEnergy:

```kotlin
// Если есть свежее значение между упражнениями — используем его как главный сигнал.
// Иначе — pre-workout. Иначе — средние значения.
val currentEnergy = wellness?.betweenExerciseEnergy ?: wellness?.preWorkoutEnergy

val fatiguedContext = currentEnergy == EnergyLevel.FATIGUED ||
    ((wellness?.sevenDayAvgEnergy ?: 2.0) < 1.8) ||
    ((wellness?.sevenDayAvgSleep ?: 3.0) < 2.5)
```

Это значит: если юзер был бодрый перед тренировкой, а через 4 упражнения нажал FATIGUED — мы будем считать его уставшим и LiveCoach будет давать соответствующие рекомендации (KEEP вместо DECREASE при тяжёлом подходе).

⚠️ Обновить существующие тесты `LiveCoachTest` где есть `wellness != null` — добавить `betweenExerciseEnergy = null` явно или через named-параметр.

---

## B6.2. DAO-метод для between-записей

### Задача

В `data/local/dao/WellnessDao.kt` сейчас есть:
- `findBySessionAndType(sessionId, type)` — возвращает **одну** запись
- `observeRecent(since)`
- `listSince(since)`
- `all()`

Нет метода для получения **всех** between-записей конкретной сессии. Добавить:

```kotlin
@Query("""
    SELECT * FROM wellness_log
    WHERE sessionId = :sessionId AND type = :type
    ORDER BY timestamp DESC
""")
suspend fun listBySessionAndType(sessionId: String, type: String): List<WellnessEntity>
```

### В WellnessRepository

```kotlin
interface WellnessRepository {
    // ... существующие методы

    /** Все between-записи конкретной сессии, от свежих к старым. */
    suspend fun getBetweenExercisesFor(sessionId: String): List<WellnessEntry>
}
```

Реализация:

```kotlin
override suspend fun getBetweenExercisesFor(sessionId: String): List<WellnessEntry> {
    return dao.listBySessionAndType(sessionId, WellnessTriggerType.BETWEEN_EXERCISES.name)
        .map { it.toDomain() }
}
```

### В WorkoutViewModel

В методе `analyzeSetAndPrepareReactionInternal` сейчас собирается wellness-снэпшот. Расширить чтобы брать **последнюю** between-запись этой сессии:

```kotlin
val pre = wellnessRepo.getPreWorkoutFor(sessionId)?.energy
val betweens = wellnessRepo.getBetweenExercisesFor(sessionId)
val latestBetween = betweens.firstOrNull()?.energy   // первая = самая свежая, т.к. сортировка DESC
val avgEnergy = wellnessRepo.avgEnergyLast(7)
val avgSleep = ...

val wellnessSnapshot = WellnessSnapshot(
    preWorkoutEnergy = pre,
    betweenExerciseEnergy = latestBetween,    // НОВОЕ
    sevenDayAvgEnergy = avgEnergy,
    sevenDayAvgSleep = avgSleep
)
```

---

## B6.3. Триггер-логика

### Когда показывать picker

Из дизайн-документа:

> Появляется только если сильное отклонение в предыдущем упражнении или прошло >3 упражнений

Конкретизирую правила:

**Picker показывается ПЕРЕД новым упражнением (не первым) если:**

1. **С момента последнего picker-а прошло 3+ упражнений** — даже если всё было ок, давно не спрашивали; ИЛИ
2. **В только что закрытом упражнении был сильный сигнал** — хотя бы один подход с вердиктом `TOO_HEAVY` или `TOO_EASY`; ИЛИ
3. **Это первое упражнение после второй трети тренировки** — например 3-е или 4-е из 6, точка где обычно копится усталость.

**Picker НЕ показывается если:**

1. Это **первое упражнение тренировки** — pre-workout уже спрашивал
2. Уже показывали picker для этого перехода (флаг в БД — записан `BETWEEN_EXERCISES` для этой сессии в последний переход)
3. Юзер пометил «не показывать в этой сессии» (см. UI)

### Где хранить «прошло 3+ упражнений с последнего picker»

Просто считаем по записям в БД:

```kotlin
suspend fun shouldShowBetweenPicker(
    sessionId: String,
    completedExerciseIndex: Int,    // 0-based номер только что завершённого упражнения
    totalExercises: Int,
    lastExerciseHadStrongSignal: Boolean
): Boolean {
    // Не показываем после первого упражнения — pre-workout уже был
    if (completedExerciseIndex == 0) return false

    // Уже спрашивали в этом переходе?
    val betweens = wellnessRepo.getBetweenExercisesFor(sessionId)
    val lastBetweenAfterExerciseIndex = betweens.firstOrNull()?.let { entry ->
        // Извлекаем номер упражнения из notes — см. B6.5 как сохраняем
        entry.notes?.toIntOrNull()
    }
    if (lastBetweenAfterExerciseIndex == completedExerciseIndex) return false

    // Триггер 1: сильный сигнал в последнем упражнении
    if (lastExerciseHadStrongSignal) return true

    // Триггер 2: прошло 3+ упражнений с последнего picker
    val sinceLast = lastBetweenAfterExerciseIndex
        ?.let { completedExerciseIndex - it }
        ?: completedExerciseIndex
    if (sinceLast >= 3) return true

    // Триггер 3: середина тренировки
    val midpoint = totalExercises / 2
    if (completedExerciseIndex == midpoint) return true

    return false
}
```

### Как считать "lastExerciseHadStrongSignal"

Через `set_reactions` таблицу (которая уже есть):

```kotlin
suspend fun lastExerciseHadStrongSignal(
    sessionId: String,
    exerciseId: String
): Boolean {
    // Берём все реакции для этого упражнения в этой сессии
    val reactions = setReactionDao.findByExerciseInSession(sessionId, exerciseId)
    return reactions.any { 
        it.verdict in listOf(SetVerdict.TOO_HEAVY.name, SetVerdict.TOO_EASY.name)
    }
}
```

⚠️ Этого DAO-метода сейчас нет. Добавить:

```kotlin
@Query("SELECT * FROM set_reactions WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
suspend fun findByExerciseInSession(sessionId: String, exerciseId: String): List<SetReactionEntity>
```

---

## B6.4. UI — реюз EnergyPicker

### Что сейчас есть

В `WorkoutScreen.kt` уже есть приватный `PreWorkoutEnergyPicker` overlay с тремя кнопками 😴/🙂/💪. Он работает, в дизайн-системе.

### Задача

Извлечь его в **публичный переиспользуемый компонент** `EnergyPickerOverlay` в `presentation/components/Components.kt`.

Параметры:

```kotlin
@Composable
fun EnergyPickerOverlay(
    title: String,                              // "КАК ОЩУЩЕНИЯ?" / "КАК САМОЧУВСТВИЕ ТЕПЕРЬ?"
    subtitle: String? = null,                   // опциональное пояснение
    onPick: (EnergyLevel) -> Unit,
    onSkip: (() -> Unit)? = null,               // если null — нет кнопки skip
    onDismiss: (() -> Unit)? = null              // тап вне = закрыть; null = модальный
)
```

В `WorkoutScreen.kt` существующий picker заменить на использование нового компонента:

```kotlin
if (ui.showPreWorkoutPicker) {
    EnergyPickerOverlay(
        title = "КАК ОЩУЩЕНИЯ?",
        subtitle = null,
        onPick = vm::savePreWorkoutEnergy,
        onSkip = vm::skipPreWorkoutPicker     // если уже сделали в Block D.1
    )
}
```

⚠️ Если Block D.1 из предыдущего ТЗ ещё не сделан — `onSkip = null` и сохранить как есть, добавив skip потом.

---

## B6.5. Интеграция в RestScreen

### Где появляется picker

**RestScreen** показывается во время отдыха после каждого подхода. Pickers появляется **только** когда:
- `nextPending.exerciseId != exerciseId` (param RestScreen) — следующий подход уже на другом упражнении
- `vm.shouldShowBetweenPicker(...)` вернул `true`

Логика в `RestScreen`:

```kotlin
@Composable
fun RestScreen(...) {
    val ui by vm.ui.collectAsState()
    val session by vm.session.collectAsState()

    // Триггер picker только при переходе на новое упражнение
    val isExerciseTransition = nextPending != null && nextPending.second != exerciseId

    LaunchedEffect(isExerciseTransition, session) {
        if (isExerciseTransition) {
            vm.checkBetweenExercisePicker(
                completedExerciseId = exerciseId,
                nextExerciseId = nextPending!!.second
            )
        }
    }

    Box(...) {
        // ... таймер + карточки

        if (ui.showBetweenExercisePicker) {
            EnergyPickerOverlay(
                title = "КАК САМОЧУВСТВИЕ ТЕПЕРЬ?",
                subtitle = "Помогает подобрать нагрузку для следующего упражнения",
                onPick = { level ->
                    vm.saveBetweenExerciseEnergy(level, exerciseId)
                },
                onSkip = { vm.skipBetweenExercisePicker(exerciseId) }
            )
        }
    }
}
```

### VM-методы

В `WorkoutViewModel`:

```kotlin
// Добавить в WorkoutUi
data class WorkoutUi(
    // ... существующие поля
    val showBetweenExercisePicker: Boolean = false
)

fun checkBetweenExercisePicker(
    completedExerciseId: String,
    nextExerciseId: String
) {
    viewModelScope.launch {
        val session = sessionRepo.getSession(sessionId) ?: return@launch
        val completedIndex = session.exerciseLogs
            .indexOfFirst { it.exerciseId == completedExerciseId }
        if (completedIndex < 0) return@launch
        val totalExercises = session.exerciseLogs.size
        val hadStrongSignal = lastExerciseHadStrongSignal(sessionId, completedExerciseId)
        val should = shouldShowBetweenPicker(
            sessionId = sessionId,
            completedExerciseIndex = completedIndex,
            totalExercises = totalExercises,
            lastExerciseHadStrongSignal = hadStrongSignal
        )
        if (should) {
            _ui.update { it.copy(showBetweenExercisePicker = true) }
        }
    }
}

fun saveBetweenExerciseEnergy(level: EnergyLevel, completedExerciseId: String) {
    viewModelScope.launch {
        val session = sessionRepo.getSession(sessionId) ?: return@launch
        val completedIndex = session.exerciseLogs
            .indexOfFirst { it.exerciseId == completedExerciseId }
        wellnessRepo.save(
            WellnessEntry(
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                type = WellnessTriggerType.BETWEEN_EXERCISES,
                energy = level,
                notes = completedIndex.toString()    // храним номер упражнения для дедупа
            )
        )
        _ui.update { it.copy(showBetweenExercisePicker = false) }
    }
}

fun skipBetweenExercisePicker(completedExerciseId: String) {
    viewModelScope.launch {
        val session = sessionRepo.getSession(sessionId) ?: return@launch
        val completedIndex = session.exerciseLogs
            .indexOfFirst { it.exerciseId == completedExerciseId }
        // Сохраняем «маркер пропуска» — запись с energy=null
        wellnessRepo.save(
            WellnessEntry(
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                type = WellnessTriggerType.BETWEEN_EXERCISES,
                energy = null,
                notes = completedIndex.toString()
            )
        )
        _ui.update { it.copy(showBetweenExercisePicker = false) }
    }
}
```

⚠️ **Поле `notes` используется как метаданные** — храним там номер завершённого упражнения чтобы:
1. Не показывать picker дважды для одного и того же перехода
2. Считать «прошло X упражнений с последнего picker»

Это **прагматичное использование** существующего поля, без новой миграции БД.

### Обновлённый поток flow

```
Юзер → SetEntry: сохранил последний подход упражнения А
    ↓
Rest: таймер + AdaptationCard (если есть реакция) + 3 коуч-карточки
    ↓
Юзер тапнул «Продолжить тренировку» → следующий подход уже упражнение Б
    ↓
NEW: При входе в новый Rest LaunchedEffect видит что exercise сменилось
    → checkBetweenExercisePicker() → если правила выполняются → picker
    ↓
Юзер выбрал/пропустил → picker исчезает → продолжает таймер/идёт дальше
```

⚠️ Нюанс: picker появляется **в Rest, а не в SetEntry следующего упражнения**. Логика: юзер только что закончил упражнение А, у него есть пауза → лучшее место чтобы спросить.

---

## B6.6. Использование данных в LiveCoach

Уже описано в B6.1 — `betweenExerciseEnergy` приходит в `WellnessSnapshot` и используется в `fatiguedContext`. Дополнительной работы здесь нет.

Но **проверить вручную**: если юзер пометил FATIGUED через picker, и в следующем упражнении делает HEAVY подход — должен прийти suggestion `KEEP + дополнительный отдых`, не `DECREASE`.

---

## B6.7. Тесты

Создать `app/src/test/java/com/forma/app/presentation/screens/workout/BetweenExercisePickerLogicTest.kt` с минимум **8 кейсами**:

### 1. Не показываем после первого упражнения

```kotlin
@Test
fun shouldShow_returnsFalse_afterFirstExercise() = runBlocking {
    val result = shouldShowBetweenPicker(
        sessionId = "s1",
        completedExerciseIndex = 0,
        totalExercises = 5,
        lastExerciseHadStrongSignal = false
    )
    assertThat(result).isFalse()
}
```

### 2. Показываем при сильном сигнале

```kotlin
@Test
fun shouldShow_returnsTrue_onStrongSignal() = runBlocking {
    val result = shouldShowBetweenPicker(
        sessionId = "s1",
        completedExerciseIndex = 1,
        totalExercises = 5,
        lastExerciseHadStrongSignal = true
    )
    assertThat(result).isTrue()
}
```

### 3. Показываем когда прошло 3+ упражнений с последнего picker

Fake repo возвращает `BETWEEN_EXERCISES` запись с `notes = "0"` (после первого упражнения). Текущий index = 4. Прошло 4 упражнения → показываем.

### 4. НЕ показываем когда picker был только что

Fake repo возвращает запись с `notes = "2"` (после 3-го упражнения). Текущий index = 3. Прошло 1 упражнение → не показываем (даже если есть сильный сигнал — нет, тут показываем, кейс по-другому).

Уточняю: **сильный сигнал переопределяет «недавно показывали»**. Кейс 4 — нет сильного сигнала, прошло 1 упражнение → false.

### 5. Показываем в середине тренировки

```kotlin
val result = shouldShowBetweenPicker(
    completedExerciseIndex = 2,    // 3-е упражнение, midpoint от 5 = 2
    totalExercises = 5,
    lastExerciseHadStrongSignal = false
)
// Если midpoint срабатывает — true
```

### 6. НЕ показываем дважды для одного и того же перехода

Fake repo возвращает запись с `notes = "3"`. Текущий index = 3 (тот же самый — повторное открытие Rest для того же завершённого упражнения).
- Должно быть false независимо от других условий.

### 7. lastExerciseHadStrongSignal корректно вычисляется

Fake `SetReactionDao` возвращает реакции с `verdict = "TOO_HEAVY"`. Метод должен вернуть true.

### 8. lastExerciseHadStrongSignal false если только ON_TARGET

Все реакции `verdict = "ON_TARGET"`. Метод возвращает false.

### Структура тестов

Использовать **fake** реализации `WellnessRepository` и `SetReactionDao` без mock-фреймворков, как в существующих тестах проекта.

---

## B6.8. Обновление LiveCoach тестов

Так как добавили поле в `WellnessSnapshot`, нужно проверить что **существующие тесты в `LiveCoachTest`** (которые мы делаем в TZ_STAGE3) продолжают компилироваться.

Если в существующих тестах `WellnessSnapshot(preWorkoutEnergy = ..., sevenDayAvgEnergy = ..., ...)` создаётся через позиционные параметры — пересмотреть на named parameters чтобы не пришлось менять много мест при добавлении поля.

Также добавить **2 новых теста** в `LiveCoachTest` если их там нет:

```kotlin
@Test
fun fatigueDetected_fromBetweenExerciseEnergy_overridesPreWorkout() {
    // Pre-workout = ENERGIZED, between = FATIGUED. HEAVY подход.
    // Должно сработать как fatiguedContext = true → KEEP, не DECREASE
}

@Test
fun fatigueDetected_fallbackToPreWorkout_whenBetweenIsNull() {
    // Pre-workout = FATIGUED, between = null. HEAVY подход.
    // fatiguedContext = true → KEEP
}
```

---

## Раздел C. Приёмка

Готово когда:

1. ✅ Сборка `./gradlew assembleDebug` без ошибок
2. ✅ Юнит-тесты проходят: предыдущие 53 + 8 новых = **61** тест минимум
3. ✅ `WellnessSnapshot` имеет поле `betweenExerciseEnergy`
4. ✅ `LiveCoach.fatiguedContext` использует betweenExerciseEnergy с приоритетом
5. ✅ В `WellnessDao` есть метод `listBySessionAndType`
6. ✅ В `WellnessRepository` есть метод `getBetweenExercisesFor`
7. ✅ `EnergyPickerOverlay` извлечён в общий компонент в `presentation/components/`
8. ✅ Существующий PreWorkoutEnergyPicker заменён на использование общего компонента
9. ✅ `WorkoutViewModel` имеет методы `checkBetweenExercisePicker`, `saveBetweenExerciseEnergy`, `skipBetweenExercisePicker`
10. ✅ В RestScreen picker появляется **только** при переходе на новое упражнение И только когда триггер сработал
11. ✅ Picker НЕ появляется после первого упражнения сессии
12. ✅ Picker НЕ появляется дважды для одного и того же перехода
13. ✅ После выбора энергии — запись `BETWEEN_EXERCISES` сохранена в БД с `notes = индекс упражнения`
14. ✅ После пропуска — запись с `energy = null` сохранена

### Вручную проверить на устройстве

1. **Не первое упражнение, нет сильных сигналов, прошло мало** — picker НЕ появляется
2. **TOO_HEAVY на втором упражнении** — picker появляется при переходе на третье
3. **Юзер выбрал FATIGUED** — на следующем упражнении при HEAVY подходе движок даёт KEEP, не DECREASE
4. **Юзер пропустил picker** — больше не появляется в этом переходе. На следующем переходе может появиться по другим триггерам.
5. **Возврат на план и обратно** — picker не дублируется (если уже сохранили запись для этого перехода).

---

## Раздел D. Замечания

### D.1 Производительность

При каждом входе в RestScreen с новым упражнением вызывается `checkBetweenExercisePicker` который дёргает БД 2 раза (`getBetweenExercisesFor` + `findByExerciseInSession`). Это норм — БД локальная, запросы маленькие.

### D.2 Альтернативный подход к хранению

Вместо использования `notes` для метаданных можно было бы создать отдельную таблицу `between_exercise_pickers`. Но это **миграция БД** и больше работы для маленькой пользы. Текущий подход через notes — прагматичный.

### D.3 Триггеры — может потребовать тюнинга

Правила «3+ упражнений или сильный сигнал или midpoint» — это **первая итерация**. После альфы может потребоваться подкрутить:
- Снизить чувствительность если юзеры жалуются на спам picker'ов
- Повысить если юзеры пропускают и не дают данных

Для тюнинга достаточно поправить константы в `shouldShowBetweenPicker`.

### D.4 Что НЕ делаем

- Анимацию появления picker (используем существующий стиль EnergyPickerOverlay)
- Отдельные иконки между упражнениями (те же 😴/🙂/💪 что в pre/post)
- Звуковой сигнал на появление picker
- Уведомление если юзер ушёл из приложения

---

## Раздел E. Стиль и общие правила

- Hilt для DI
- Coroutines для асинхронности
- kotlinx.serialization для DTO
- Material 3 + Compose для UI
- Используем существующие компоненты `FormaCard`, `PrimaryButton`, цвета из `presentation/theme`
- Logger тег `Forma.LiveCoach` для триггер-логики, `Forma.Wellness` для сохранения записей
- Truth (`com.google.common.truth.Truth.assertThat`) для тестов
- Fakes для тестов (никаких mock-фреймворков)

После закрытия этого ТЗ план LiveCoach (`docs/LIVECOACH_DESIGN.md`) **закрыт полностью**. Следующее в roadmap — Heatmap+XP по плану из `docs/HEATMAP_AND_XP_DESIGN.md`.
