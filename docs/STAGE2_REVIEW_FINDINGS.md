# Финальное ревью реализации Stage 2 (Wellness + UI + техдолг)

Дата: 2026-04-28
Архив: forma-review-clean-20260428-1847.zip
Исходное ТЗ: TZ_STAGE2_WELLNESS_AND_UI.md

## Что сделано относительно ТЗ

### Block A — UI/UX-фиксы — ✅ полностью

- A.1 Динамический шаг веса — **сделано лучше чем в ТЗ**: `smartWeightStepFor` берёт max(percentStep, weightGrid). Сетка по типу оборудования (машины 5 кг, штанга 2.5 кг, гантели 1 кг). `snapToGrid` округляет вес до доступных значений.
- A.1 RIR fallback из плана — точно по ТЗ через `getTargetRirForSet`.
- A.2 RestTimerHero — 96sp ExtraBold AccentLime через `Modifier.weight(1f)`.
- A.3 Haptic feedback — enum `HapticType { Soft, Strong, None }` в Components, дефолты Strong/None для PrimaryButton/SecondaryButton.

### Block B — Wellness — ✅ полностью

- Доменные модели, БД (v8 — на 1 больше плана из-за SetReactions), DAO, Repository.
- BackupSnapshot v4 с `wellnessLog`.
- Pre-workout overlay в WorkoutScreen, не появляется повторно для той же сессии.
- PostWorkoutWellnessScreen с 4 секциями, submit заблокирован пока energy не выбран.
- Nav wiring корректный.
- 4 теста на WellnessRepository через fake DAO.
- Settings warning обновлён на v7→v8.

### Block C — Техдолг — ✅ полностью

- C.1 markShownOnce через shownInThisSession Set — устраняет дубликаты в БД.
- C.2 ARG_SESSION_ID/ARG_EXERCISE_ID унифицированы в Route.companion.

## Что сделано СВЕРХ ТЗ

### Shared VM между Workout/SetEntry/Rest

В FormaNavGraph все три экрана используют `getBackStackEntry(Route.Workout.build(sessionId))` → `hiltViewModel(workoutEntry)`. Это решает мою главную архитектурную критику из прошлого ревью — теперь VM один на всю сессию тренировки, состояние шарится, колоды коуч-контента не пересоздаются.

### LiveCoach core движок реакций (этап 3 плана!)

`domain/livecoach/LiveCoach.kt` — детерминированный движок:
- Модели: `SetContext`, `SetVerdict`, `Confidence`, `WeightAdjustment`, `NextSetTip`, `SetReaction`, `WellnessSnapshot`
- Логика по 4 шагам из LIVECOACH_DESIGN: вердикт → wellness корректировка → история → решение по next set
- Suggestion даём только после 2-го подхода (резонная защита от случайных первых попыток)

### SetReactions хранение

`set_reactions` таблица + DAO, интегрировано в `WorkoutViewModel.analyzeSetAndPrepareReactionInternal()`.

### Иконки

`res/drawable/ic_coach_*.xml` (5 типов) и `ic_energy_*.xml` (3 уровня) — векторные иконки в дизайн-системе.

### `EXERCISE_CONTENT_REVIEW_PROMPT.md`

Prompt-template для генерации новых JSON-пулов через AI. Хороший процесс.

### ExerciseDetailScreen

Полноценный экран деталей упражнения (259 строк), подключён через nav.

## Что не сделано / в техдолге

### LiveCoach core без тестов

Класс `LiveCoach` с нетривиальной логикой 7 вердиктов и 4 типами рекомендаций — без юнит-тестов. Если этап 3 завершается официально — нужно покрытие минимум 10-12 кейсов.

### FeedbackScreen — сырой набросок

`presentation/screens/workout/FeedbackScreen.kt` (для фидбека между упражнениями, этап 6 плана):
- Не подключён в nav
- Не в дизайн-системе (стандартные Material компоненты, нет BgBlack, нет FormaCard, нет PrimaryButton)
- CRLF переносы (Windows формат) вместо LF

Решение: либо доработать в стиле проекта, либо удалить пока не дойдёт черёд этапа 6.

### BackupSnapshot — устаревшие комментарии

В `BackupSnapshot.kt` companion упоминает только v1→v2, не v3 и не v4. Минорно, но при росте версий комментарий должен поспевать.

## Итог

Stage 2 закрыт **полностью + сверх плана**. Готово к слиянию и тесту на устройстве.

Следующий шаг: либо ручной тест на устройстве, либо ТЗ на этап 3 (LiveCoach UI — карточка реакции в SetEntry/Rest, корректировка веса с кнопкой Да/Нет, тесты LiveCoach). Так как ядро движка уже сделано, ТЗ на этап 3 будет короткое — UI + тесты.
