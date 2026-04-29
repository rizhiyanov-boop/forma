# ТЗ: Этап 3 финализация + Этап 5 (Wellness в AI-разборе) + Техдолг

**Тип задачи:** дозакрытие фич + новая функциональность + чистка техдолга.
**Приёмка:** компиляция без ошибок, юнит-тесты проходят, AI-разбор учитывает самочувствие, нет «висящих» файлов в репо.

**Контекст:** см. `docs/LIVECOACH_DESIGN.md` (секции 5, 12) и `docs/STAGE2_REVIEW_FINDINGS.md`.

---

## Что уже сделано на текущий момент (для понимания контекста)

Из плана LiveCoach (секция 13 дизайн-документа):

- ✅ Этап 1 — контентный пул
- ✅ Этап 2 — Wellness данные (pre-workout picker + post-workout анкета)
- ✅ Этап 3 — LiveCoach core движок реакций **+ UI карточки реакции в RestScreen** уже работает (`AdaptationCard` с кнопками Принять/Оставить, накапливает решения юзера в `set_reactions` таблице)
- ✅ Этап 4 — рефакторинг WorkoutScreen на 3 экрана (Workout/SetEntry/Rest) с shared VM

**Что НЕ закрыто:**

- 🟡 Этап 3 — **юнит-тесты `LiveCoach`** не написаны
- ❌ Этап 5 — Wellness в AI-разборе (`ReviewRepositoryImpl` не использует `WellnessRepository`)
- ❌ Этап 6 — picker между упражнениями (`FeedbackScreen` существует как сырой набросок, не подключён)

---

## Структура ТЗ

ТЗ из 4 блоков, можно делать в любом порядке:

- **Block A** — финализация LiveCoach core: юнит-тесты движка
- **Block B** — Wellness в AI-разборе (этап 5)
- **Block C** — техдолг: судьба `FeedbackScreen` + комментарии в `BackupSnapshot`
- **Block D** — мелкие UX/UI правки которые я выявил при ревью кода

---

# Block A. Юнит-тесты LiveCoach

## Контекст

Класс `domain/livecoach/LiveCoach.kt` сделан и используется в `WorkoutViewModel.analyzeSetAndPrepareReactionInternal()`. Он принимает `SetContext` (повторы, RIR, плановый вес, предыдущие подходы упражнения, wellness-снэпшот) и возвращает `SetReaction` (вердикт, краткое сообщение, опционально предложение по следующему подходу).

В коде сейчас 7 вердиктов + 4 типа корректировки веса + ветвление по wellness. **Без тестов** — что недопустимо для критичной функции.

## Задача

Создать `app/src/test/java/com/forma/app/domain/livecoach/LiveCoachTest.kt` с минимум **15 кейсами**.

## Кейсы (обязательные)

### 1. INSUFFICIENT при null RIR

```kotlin
@Test
fun analyze_returnsInsufficient_whenRirIsNull() {
    val ctx = SetContext(
        setNumber = 1, totalSets = 3,
        targetReps = 6..10, targetRir = 1,
        plannedWeight = 60.0, actualReps = 8,
        actualRir = null,             // не указан
        actualWeight = 60.0,
        previousSetsThisExercise = emptyList(),
        currentWellness = null
    )
    val r = LiveCoach().analyze(ctx, suggestedStep = 2.5)
    assertThat(r.verdict).isEqualTo(SetVerdict.INSUFFICIENT)
    assertThat(r.confidence).isEqualTo(Confidence.LOW)
    assertThat(r.nextSetSuggestion).isNull()
}
```

### 2. ON_TARGET — повторы в плане + RIR в пределах ±1 от цели

Цель RIR=1, факт RIR=1 или 2. Повторы 8 (план 6..10).
- `verdict = ON_TARGET`
- `nextSetSuggestion = null`
- `shortMessage = "В плане, RIR в норме"`

### 3. EASY — RIR на 2 выше цели, повторы в плане

Цель RIR=1, факт RIR=3. Повторы 10 (план 6..10).
- `verdict = EASY`
- При `setNumber >= 2` — `suggestion.type = INCREASE`, `newWeight = plannedWeight + step`
- При `setNumber = 1` — `suggestion = null` (ещё рано предлагать)

### 4. TOO_EASY — RIR на 3+ выше цели И повторы выше плана

Цель RIR=1, факт RIR=4. Повторы 12 (план 6..10, верх 10).
- `verdict = TOO_EASY`
- На 2+ подходе — `INCREASE`

### 5. HEAVY — RIR на 2+ ниже цели, повторы ниже плана

Цель RIR=1, факт RIR=-1. Повторы 5 (план 6..10).
- `verdict = HEAVY`
- На 2+ подходе и без fatigue — `DECREASE`, `newWeight = plannedWeight - step`

### 6. TOO_HEAVY — RIR=0 И повторы ниже минимума

Цель RIR=1, факт RIR=0. Повторы 4 (план 6..10).
- `verdict = TOO_HEAVY`
- Без fatigue — `DECREASE`

### 7. UNUSUAL — повторы вне плана, но RIR в пределах ±1

Цель RIR=1, факт RIR=1. Повторы 12 (план 6..10).
- `verdict = UNUSUAL`
- `confidence = LOW`
- `suggestion = null`

### 8. Suggestion заблокирован на первом подходе

Любая комбинация даёт EASY/HEAVY на `setNumber = 1`.
- `verdict` соответствующий
- `nextSetSuggestion = null`

Аргумент: первый подход может быть нерепрезентативным (плохая разминка, странный день).

### 9. Wellness FATIGUED + HEAVY → KEEP с дополнительным отдыхом

Юзер пометил себя FATIGUED перед тренировкой. Подход тяжёлый.
```kotlin
val wellness = WellnessSnapshot(
    preWorkoutEnergy = EnergyLevel.FATIGUED,
    sevenDayAvgEnergy = null,
    sevenDayAvgSleep = null
)
```
- `verdict = HEAVY`
- `suggestion.type = KEEP` (не снижаем вес)
- `suggestion.newRestSeconds = 30` (даём больше отдыха)
- В rationale упоминается усталость

### 10. Низкий 7-дневный сон → fatigue context даже без preWorkoutEnergy

```kotlin
val wellness = WellnessSnapshot(
    preWorkoutEnergy = null,
    sevenDayAvgEnergy = null,
    sevenDayAvgSleep = 2.0    // ниже порога 2.5
)
```
HEAVY → `KEEP`, не `DECREASE`.

### 11. Низкая 7-дневная энергия → fatigue context

`sevenDayAvgEnergy = 1.5` (ниже 1.8). HEAVY → KEEP.

### 12. ENERGIZED + HEAVY → DECREASE как обычно

Юзер бодрый, но всё равно тяжело — снижаем вес (это значит вес объективно завышен, не от усталости).
- `verdict = HEAVY`
- `suggestion.type = DECREASE`

### 13. plannedWeight = null (бодвейт упражнение) → suggestion = null

Подтягивания, отжимания. Нет веса.
- При EASY/HEAVY — `suggestion = null` (нечего корректировать в кг)
- `verdict` всё равно правильный

### 14. INCREASE использует переданный `suggestedStep`

```kotlin
val r = LiveCoach().analyze(ctxWithEasy, suggestedStep = 4.0)
assertThat(r.nextSetSuggestion?.newWeight).isEqualTo(60.0 + 4.0)
```

Это проверяет что step из `WorkoutViewModel.smartWeightStepFor()` корректно прокидывается в движок.

### 15. DECREASE не уходит в минус при низком весе

`plannedWeight = 1.0`, `step = 2.5`. HEAVY → suggestion с `newWeight = 0.0` (через `coerceAtLeast(0.0)`), не -1.5.

## Структура теста

Использовать `Truth` (`com.google.common.truth.Truth.assertThat`) — уже подключён в проекте.

Хелпер для удобного создания контекста:

```kotlin
private fun ctx(
    setNumber: Int = 2,
    targetReps: IntRange = 6..10,
    targetRir: Int = 1,
    plannedWeight: Double? = 60.0,
    actualReps: Int,
    actualRir: Int?,
    actualWeight: Double? = plannedWeight,
    previousSets: List<SetLog> = emptyList(),
    wellness: WellnessSnapshot? = null
) = SetContext(
    setNumber = setNumber,
    totalSets = 3,
    targetReps = targetReps,
    targetRir = targetRir,
    plannedWeight = plannedWeight,
    actualReps = actualReps,
    actualRir = actualRir,
    actualWeight = actualWeight,
    previousSetsThisExercise = previousSets,
    currentWellness = wellness
)
```

## Приёмка Block A

- ✅ Файл `LiveCoachTest.kt` создан в `app/src/test/java/com/forma/app/domain/livecoach/`
- ✅ Минимум 15 тестов, все 15 описанных выше кейсов покрыты
- ✅ `./gradlew test` проходит, итого тестов 38 + 15 = **53**

---

# Block B. Wellness в AI-разборе (этап 5)

## Контекст

Сейчас `ReviewRepositoryImpl.reviewAfterWorkout()`:
1. Запускает `ProgressionEngine.analyze()` для каждого упражнения
2. Проверяет через `SanityValidator`
3. Передаёт результат в `OpenAiService.enrichEngineReview()` → AI пишет summary и обогащает rationale
4. Сохраняет review в БД

**Wellness-данные (энергия/сон/стресс) сейчас НЕ передаются в AI.** Это значит AI не знает что юзер был уставшим/выспанным/в стрессе, и может неправильно интерпретировать данные тренировки.

Пример: юзер сделал жим 60×6 RIR 0, RPE максимальный. Без wellness AI скажет «ты упёрся в потолок, попробуй снизить вес». **С wellness** (юзер пометил FATIGUED перед тренировкой и spal плохо 3 дня) AI скажет «не страшно, сегодня день не зашёл, на следующей неделе попробуем тот же вес снова».

## Задача

Расширить поток AI-разбора чтобы он учитывал wellness-контекст.

## B.1 Расширение модели запроса в OpenAiService

В `OpenAiService.kt` метод `enrichEngineReview()` сейчас принимает:

```kotlin
suspend fun enrichEngineReview(
    session: WorkoutSession,
    workout: Workout,
    engineRecommendations: List<EnrichmentInput>,
    userSex: Sex
): QuickReviewDto
```

Расширить до:

```kotlin
suspend fun enrichEngineReview(
    session: WorkoutSession,
    workout: Workout,
    engineRecommendations: List<EnrichmentInput>,
    userSex: Sex,
    wellnessContext: WellnessContext?     // НОВОЕ — может быть null если данных нет
): QuickReviewDto
```

Где `WellnessContext` — новая модель в `data/remote/`:

```kotlin
package com.forma.app.data.remote

/**
 * Контекст самочувствия для передачи в AI-разбор.
 * Все поля опциональные — если данных нет, передаём null.
 */
data class WellnessContext(
    val preWorkoutEnergy: String?,        // EnergyLevel.displayName: "Уставший" / "Норма" / "Бодрый"
    val postWorkoutEnergy: String?,
    val postWorkoutSleep: Int?,           // 1..5
    val postWorkoutStress: Int?,          // 1..5
    val postWorkoutMood: Int?,            // 1..5
    val sevenDayAvgEnergy: Double?,       // 1.0..3.0
    val recentLowEnergyStreak: Int        // дней подряд с энергией FATIGUED, 0 если нет
)
```

## B.2 Промпт-инжиниринг

В системный промпт `enrichEngineReview` добавить блок про учёт wellness:

```
Если в запросе передан блок WELLNESS_CONTEXT:

- Если `preWorkoutEnergy = "Уставший"` ИЛИ `sevenDayAvgEnergy < 1.8` —
  юзер пришёл уставший. Если есть регрессы или повторы ниже плана — НЕ интерпретируй
  это как «упёрся в потолок» или «нужен deload». Скажи что сегодня день не зашёл,
  это нормально, на следующей неделе попробуем тот же вес.

- Если `recentLowEnergyStreak >= 3` — юзер несколько дней подряд уставший.
  Если результаты тренировки слабые — это объяснимо. Предложи задуматься о
  восстановлении (сон, питание, стресс), а не менять программу.

- Если `postWorkoutSleep <= 2` или `postWorkoutStress >= 4` — упомяни это в
  summary как контекст результатов. Не надо «рекомендовать выспаться» — это
  банально. Лучше: «учитывая что сон последние ночи был плохой, прогресс
  держится на уровне — это уже результат».

- Если все wellness-маркеры в норме (энергия NORMAL/ENERGIZED, сон 4-5,
  стресс 1-3) и при этом РЕГРЕСС — тогда это реальный сигнал. AI может
  предположить что вес объективно завышен или нужен deload.

- Если wellness в норме и ПРОГРЕСС — обычная похвала.

ВАЖНО: не упоминай wellness в каждом предложении. Это контекст для интерпретации,
не самостоятельная тема разбора. Достаточно 1-2 упоминаний если они уместны.
```

В пользовательский промпт добавить блок WELLNESS_CONTEXT после блока ALGORITHMIC_RECOMMENDATIONS:

```
════════ WELLNESS_CONTEXT ════════

Самочувствие до тренировки: {preWorkoutEnergy ?: "не указано"}
Самочувствие после тренировки: {postWorkoutEnergy ?: "не указано"}
Сон прошлой ночью: {postWorkoutSleep}/5
Уровень стресса: {postWorkoutStress}/5
Настроение: {postWorkoutMood}/5
Средняя энергия за 7 дней: {format(sevenDayAvgEnergy)}
Уставших дней подряд: {recentLowEnergyStreak}
```

⚠️ Если `wellnessContext = null` — этот блок в промпт **не добавляется** вообще, чтобы не запутать AI.

## B.3 Сборка контекста в ReviewRepositoryImpl

В `ReviewRepositoryImpl` инжектируется `WellnessRepository`:

```kotlin
@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val reviewDao: ReviewDao,
    private val sessionRepo: SessionRepository,
    private val programRepo: ProgramRepository,
    private val profileRepo: UserProfileRepository,
    private val wellnessRepo: WellnessRepository,    // НОВОЕ
    private val openAi: OpenAiService
) : ReviewRepository
```

В `reviewAfterWorkout()` перед вызовом `enrichEngineReview` собираем контекст:

```kotlin
val wellnessContext = buildWellnessContext(sessionId)

val dto = openAi.enrichEngineReview(
    session = session,
    workout = workout,
    engineRecommendations = enrichmentInputs,
    userSex = profile.sex,
    wellnessContext = wellnessContext       // НОВОЕ
)
```

Метод сборки:

```kotlin
private suspend fun buildWellnessContext(sessionId: String): WellnessContext? {
    val pre = wellnessRepo.getPreWorkoutFor(sessionId)
    val post = wellnessRepo.getPostWorkoutFor(sessionId)
    val avg7d = wellnessRepo.avgEnergyLast(7)
    val streak = computeLowEnergyStreak(daysBack = 7)

    // Если вообще нет ни одной wellness-записи — возвращаем null
    // чтобы блок не появлялся в промпте
    if (pre == null && post == null && avg7d == null) return null

    return WellnessContext(
        preWorkoutEnergy = pre?.energy?.displayName,
        postWorkoutEnergy = post?.energy?.displayName,
        postWorkoutSleep = post?.sleepQuality,
        postWorkoutStress = post?.stressLevel,
        postWorkoutMood = post?.mood,
        sevenDayAvgEnergy = avg7d,
        recentLowEnergyStreak = streak
    )
}

/** Сколько дней подряд (заканчивая сегодня) энергия была FATIGUED. */
private suspend fun computeLowEnergyStreak(daysBack: Int): Int {
    val entries = wellnessRepo.observeRecent(daysBack).first()
        .filter { it.energy != null }
        .sortedByDescending { it.timestamp }
    var streak = 0
    val dayMs = 24L * 60L * 60L * 1000L
    val today = System.currentTimeMillis()
    for (i in 0 until daysBack) {
        val dayStart = today - (i + 1) * dayMs
        val dayEnd = today - i * dayMs
        val entryForDay = entries.firstOrNull { it.timestamp in dayStart..dayEnd }
            ?: break
        if (entryForDay.energy == EnergyLevel.FATIGUED) streak++ else break
    }
    return streak
}
```

⚠️ Логика `computeLowEnergyStreak` опирается на то что юзер делает хотя бы одну запись в день. Если юзер пропустил день — streak обрывается. Это **правильное поведение** — иначе будет считать что юзер уставший когда он просто не зашёл в приложение.

## B.4 DI-обновление

Hilt сам подхватит новую зависимость в конструкторе. Дополнительной регистрации не нужно — `WellnessRepository` уже забинден в `RepositoryModule.kt`.

## B.5 Тесты

Создать `app/src/test/java/com/forma/app/data/repository/ReviewRepositoryWellnessTest.kt` с 3 кейсами:

1. **`buildWellnessContext` возвращает null когда нет данных** — пустой fake `WellnessRepository` → null
2. **`buildWellnessContext` собирает корректный контекст** — fake с pre+post записями → правильно заполненный `WellnessContext`
3. **`computeLowEnergyStreak` считает корректно** — 3 дня FATIGUED подряд + 1 день NORMAL → streak = 3

⚠️ Тесты делать через **fake** реализации репозиториев. **Не тестировать сам OpenAI вызов** — это интеграция.

## Приёмка Block B

- ✅ `WellnessContext` модель создана в `data/remote/`
- ✅ `OpenAiService.enrichEngineReview` принимает новый параметр
- ✅ Системный промпт расширен инструкциями по wellness
- ✅ Пользовательский промпт добавляет блок WELLNESS_CONTEXT когда контекст не null
- ✅ `ReviewRepositoryImpl` собирает контекст и передаёт в AI
- ✅ Если wellness-данных нет — AI получает `null` и работает как раньше (обратная совместимость)
- ✅ 3 новых юнит-теста для логики `buildWellnessContext` и `computeLowEnergyStreak`
- ✅ Старые тесты ProgressionEngine/SanityValidator/Wellness/CoachContent проходят без изменений

---

# Block C. Техдолг

## C.1 Решить судьбу `FeedbackScreen`

Файл `presentation/screens/workout/FeedbackScreen.kt` сейчас:
- Не подключён в навигации
- Не в дизайн-системе (стандартные Material компоненты, нет `BgBlack`/`FormaCard`/`PrimaryButton`)
- CRLF переносы (Windows) вместо LF

**Решение: удалить.** Этап 6 плана LiveCoach (фидбек между упражнениями) сейчас НЕ в скоупе, и сырой набросок только засоряет репо.

При появлении этапа 6 этот UI будет переписан с нуля в дизайн-системе по новому ТЗ.

Также удалить связанную модель если она нигде больше не используется:

```bash
# Проверить использования и удалить если только в FeedbackScreen
grep -rn "ExerciseFeedback" app/src/main/java/
```

Если `ExerciseFeedback` используется только в `FeedbackScreen` — удалить и модель тоже (`domain/model/ExerciseFeedback.kt`).

## C.2 Обновить комментарии в `BackupSnapshot`

Сейчас в `BackupSnapshot.kt`:

```kotlin
companion object {
    /**
     * v1 — изначальный формат
     * v2 — добавлен `sex` в UserProfile. Старые v1 снапшоты импортируются нормально:
     *      Json игнорирует отсутствующее поле и берёт default `Sex.UNSPECIFIED`.
     */
    const val CURRENT_VERSION = 4
}
```

Проблема: версии 3 и 4 не описаны.

**Обновить до:**

```kotlin
companion object {
    /**
     * v1 — изначальный формат
     * v2 — добавлен `sex` в UserProfile. Старые v1 снапшоты импортируются нормально:
     *      Json игнорирует отсутствующее поле и берёт default `Sex.UNSPECIFIED`.
     * v3 — добавлен `coachContentHistory` (история показов коуч-карточек).
     *      Старые снапшоты импортируются с пустым списком истории.
     * v4 — добавлен `wellnessLog` (записи о самочувствии). Старые снапшоты
     *      импортируются с пустым списком wellness — старая БД не имела
     *      этих данных, и это нормально.
     */
    const val CURRENT_VERSION = 4
}
```

## Приёмка Block C

- ✅ `FeedbackScreen.kt` удалён
- ✅ Если `ExerciseFeedback` использовался только там — удалена доменная модель
- ✅ `BackupSnapshot` комментарии обновлены под текущую версию

---

# Block D. UX/UI правки выявленные при ревью кода

Эти замечания не критичны, но улучшают пользовательский опыт.

## D.1 PreWorkoutPicker — нет кнопки «Пропустить»

В `WorkoutScreen` overlay появляется при первом открытии сессии и **обязывает выбрать энергию**. Если юзер случайно нажал «Начать тренировку» вместо детальной просмотра — он застрял на overlay.

### Решение

Добавить мелкую ссылку «Пропустить пока» под основной картой:

```kotlin
Spacer(Modifier.height(16.dp))
Text(
    text = "Пропустить пока",
    color = TextSecondary,
    style = MaterialTheme.typography.labelMedium,
    modifier = Modifier
        .clickable {
            haptic()
            onSkip()
        }
        .padding(8.dp)
)
```

В VM:

```kotlin
fun skipPreWorkoutPicker() {
    // Сохраняем «маркер пропуска» — пустую запись с energy=null
    // чтобы overlay не появлялся повторно для этой сессии
    viewModelScope.launch {
        wellnessRepo.save(
            WellnessEntry(
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                type = WellnessTriggerType.PRE_WORKOUT,
                energy = null    // null = «пропущено»
            )
        )
        _ui.update { it.copy(showPreWorkoutPicker = false) }
    }
}
```

Логика `getPreWorkoutFor` уже корректно вернёт запись (с energy=null) и `checkPreWorkoutPicker` не будет показывать overlay снова.

## D.2 RIR в SetEntry — кнопка «Не помню»

В `LIVECOACH_DESIGN.md` была спецификация: «Есть кнопка-выход "не помню" — RIR=null, LiveCoach реагирует с низкой уверенностью». Сейчас в SetEntry RIR всегда дефолт=2 и сохраняется как Int.

### Решение

В UI SetEntry рядом с RIR-картой добавить мелкий toggle:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = if (rirSkipped) "RIR не указан" else "Не помню RIR",
        color = if (rirSkipped) AccentLime else TextSecondary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clickable { rirSkipped = !rirSkipped }
            .padding(8.dp)
    )
}
```

При сохранении подхода:

```kotlin
vm.completeSetAndAnalyze(
    ...
    rir = if (rirSkipped) null else rir,
    ...
)
```

Когда `rir = null` — `LiveCoach.analyze()` уже корректно вернёт `verdict = INSUFFICIENT` без suggestion. То есть **бэкенд готов, нужен только UI-флажок**.

## D.3 RestScreen — индикатор количества карт в колоде

Сейчас юзер видит карточку контента, но не понимает сколько ещё там есть. Если случайно лишний раз тапнет — пройдёт мимо интересной карты.

### Решение

В `CoachContentCard` добавить в заголовок индикатор позиции:

```kotlin
Row {
    Icon(...)
    Spacer(Modifier.width(8.dp))
    Text("ТЕХНИКА", color = AccentLime, ...)
    Spacer(Modifier.weight(1f))
    Text(
        text = "${currentIndex + 1} / ${deckSize}",
        color = TextSecondary,
        style = MaterialTheme.typography.labelSmall
    )
}
```

Где `currentIndex` и `deckSize` приходят из VM через `RestCardModel.Coach`:

```kotlin
data class Coach(
    val type: CoachContentType,
    val content: CoachContentItem,
    val currentIndex: Int,    // НОВОЕ
    val deckSize: Int          // НОВОЕ
) : RestCardModel
```

VM строит модель из существующих state-полей `factIndex/factDeck.size` и т.д.

## D.4 PostWorkoutWellness — сейчас можно скипнуть кнопкой Back

При нажатии системной кнопки Back на экране анкеты юзер уходит на Home **без сохранения wellness и без AI-разбора**. Это терять данные.

### Решение

Перехватывать `BackHandler` на экране и:
- Если ничего не выбрано — отпускать как сейчас
- Если что-то выбрано — показать диалог «Не сохранять прогресс? Данные тренировки останутся, но без разбора»

```kotlin
val hasInput = ui.energy != null
BackHandler(enabled = hasInput) {
    // Показать AlertDialog с подтверждением
}
```

⚠️ Это **дополнительная вежливость**, не критичная. Если делать сложно — оставить как есть.

## D.5 RestScreen — фокус на «Продолжить тренировку» когда таймер закончился

Сейчас когда таймер дошёл до 0, кнопка «Продолжить тренировку» появляется **под** карточками контента. Юзер может не сразу её увидеть.

### Решение

При `restCountdown == 0` менять стиль кнопки на полностью ярко-лаймовый и добавить лёгкую анимацию (pulse). Или выводить её **выше** карточек.

Я бы предпочёл **выше** карточек — максимальная видимость, минимум усилий.

```kotlin
if (ui.restCountdown == null || ui.restCountdown == 0) {
    nextPending?.let { ... 
        PrimaryButton(text = "Продолжить тренировку", ...)
    }
}
```

## Приёмка Block D

- ✅ D.1 — В PreWorkoutPicker есть «Пропустить пока», работает корректно
- ✅ D.2 — В SetEntry есть toggle «Не помню RIR», при выборе сохраняет null
- ✅ D.3 — Карточка контента в RestScreen показывает позицию `n/N`
- ✅ D.4 (опционально) — BackHandler на PostWorkoutWellness
- ✅ D.5 — Кнопка «Продолжить тренировку» появляется над контентом при `restCountdown == 0`

---

# Раздел E. Очерёдность работ

Чтобы можно было сдавать инкрементально:

1. **Block C** — техдолг (15 минут): удалить FeedbackScreen, обновить комменты
2. **Block A** — тесты LiveCoach (2-3 часа): изолированный, ничего не ломает
3. **Block B** — wellness в AI (3-4 часа): сложный, но изолированный
4. **Block D.1** + **D.2** + **D.3** — основные UX-правки (1-2 часа)
5. **Block D.4** + **D.5** — необязательные улучшения

Каждый блок должен компилироваться и не ломать существующее.

---

# Раздел F. Стиль и общие правила

- Hilt для DI
- Coroutines для асинхронности
- kotlinx.serialization для DTO
- Material 3 + Compose для UI
- Цвета и типографика — через `presentation/theme`
- FormaCard / PrimaryButton / SecondaryButton / TertiaryButton переиспользуем
- Logger тег `Forma.Review` для AI-разбора, `Forma.Wellness` для wellness, `Forma.LiveCoach` для движка
- Truth (`com.google.common.truth.Truth.assertThat`) для тестов
- Fakes для тестов (никаких mockk/mockito)

---

# Раздел G. Что НЕ делаем

❌ Этап 6 LiveCoach — picker между упражнениями (`FeedbackScreen` всё равно удаляем)
❌ Heatmap + XP система (отдельный план в `HEATMAP_AND_XP_DESIGN.md`)
❌ Дизайн-ревью внешнего вида (Block D — только функциональные UX-улучшения видимые из кода)
❌ Перевод на другую LLM-модель
❌ Голосовые подсказки

После закрытия этого ТЗ:
- Этап 3 LiveCoach **полностью** закрыт (с тестами)
- Этап 5 LiveCoach (wellness в AI) закрыт
- В работе остаётся только этап 6 (между упражнениями) и потом heatmap/XP
