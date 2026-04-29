# Forma — экспорт от 28.04.2026

Полная версия проекта на момент конца сессии работы с Claude.

## Что в архиве

### Код (`/app`, `build.gradle.kts`, `gradle/`, etc.)

Снимок проекта из архива `forma-review-clean-20260428-1847.zip` который ты присылал — 100 Kotlin-файлов, 11 JSON контентных пулов.

**Состояние кода:**
- ✅ Этап 1 LiveCoach (контентный пул) — закрыт
- ✅ Этап 2 LiveCoach (Wellness данные) — закрыт
- 🟡 Этап 3 LiveCoach (core движок реакций) — ядро + UI сделаны, **тесты не написаны**
- ✅ Этап 4 LiveCoach (рефакторинг WorkoutScreen на 3 экрана) — закрыт
- ❌ Этап 5 LiveCoach (Wellness в AI-разборе) — **не сделан**
- ❌ Этап 6 LiveCoach (picker между упражнениями) — **не сделан**
- ❌ UX/UI полировка (топ-5 проблем + ещё 13) — **не сделана**

### Документы (`/docs`)

#### Дизайн-документы (живые планы)

- **`LIVECOACH_DESIGN.md`** — полный дизайн LiveCoach (6 этапов плана). Из него растут все ТЗ Stage 1-6.
- **`HEATMAP_AND_XP_DESIGN.md`** — дизайн системы heatmap нагрузки и XP-уровней. Это **самый большой блок не начатый ни по коду ни по ТЗ**. 7 подэтапов, ~5-6 дней работы. Перед стартом нужно ответить на 4 открытых вопроса в начале документа.

#### Готовые ТЗ для Claude Code (по приоритету)

1. **`TZ_STAGE3_FINALIZATION_AND_AI_WELLNESS.md`** — финализация LiveCoach core: 15 юнит-тестов движка LiveCoach + интеграция wellness в AI-разбор + UX-правки + техдолг. ~6-8 часов работы Claude Code.

2. **`TZ_STAGE6_BETWEEN_EXERCISE_PICKER.md`** — picker «Как самочувствие?» между упражнениями. Зависит от Stage 3 (нужны тесты LiveCoach как фундамент). ~полдня работы.

3. **`TZ_UX_UI_AND_TEXT_POLISH.md`** — большое ТЗ на UX/UI и тексты. 18 UI-фиксов + 11 текстовых правок + контентная вычитка. ~12-15 часов работы Claude Code (или 8-10 без опциональной мини-графики и вычитки JSON).

#### Анализ и контекст

- **`STAGE2_REVIEW_FINDINGS.md`** — отчёт по итогам Stage 2 ревью. Содержит сводку что сделано/что в техдолге.
- **`LIVECOACH_REVIEW_CHANGELOG.md`** — changelog от Claude Code из последнего apply.
- **`EXERCISE_CONTENT_REVIEW_PROMPT.md`** — prompt-template для генерации новых JSON-пулов через AI.

## Рекомендованный порядок работы

### Сейчас можно стартовать в любой:

**Путь A — финализировать LiveCoach до конца плана:**
1. Передать Claude Code `TZ_STAGE3_FINALIZATION_AND_AI_WELLNESS.md`
2. Проверить результат на устройстве
3. Передать `TZ_STAGE6_BETWEEN_EXERCISE_PICKER.md`
4. После этого LiveCoach **закрыт полностью**, все 6 этапов

**Путь B — UX-полировка (видимое качество):**
1. Передать Claude Code `TZ_UX_UI_AND_TEXT_POLISH.md`
2. Проверить результат на устройстве — увидеть как сильно изменилось visual quality

**Я бы советовал A → B** или **A + B параллельно**: A это функциональность, B это качество. Оба делаются на одной кодовой базе.

### Дальше:

**Heatmap+XP** (`HEATMAP_AND_XP_DESIGN.md`) — самый большой оставшийся блок. Перед стартом ответить на 4 открытых вопроса в документе.

После heatmap — **production polish** (Play Billing, Firebase analytics, Crashlytics, API прокси для OpenAI ключа, локализация EN, бэкап в облако, push, privacy policy).

## Известные проблемы

- **`hs_err_pid*.log`** в репозитории — JVM crash dumps, можно удалить и добавить в `.gitignore`. Если они часто появляются — у тебя нестабильный билд (мало RAM для Gradle daemon)
- **Кривые папки** `{data/{local/...` могли остаться в `app/src/main/java/com/forma/app/` от старых артефактов brace expansion. Проверить и удалить.

## Стек

- Kotlin 2.0.21 + Jetpack Compose
- AGP 8.7.3, Gradle 8.10.2
- compileSdk 35, minSdk 26
- Hilt + Room + Retrofit
- OpenAI: gpt-4o-mini / gpt-4o / o4-mini
- БД v8 (после Stage 2)
- BackupSnapshot v4

## Контакт с Claude

Если открываешь новую сессию — приложи последний транскрипт из `/mnt/transcripts/` для контекста, плюс этот архив. Я смогу быстро войти в курс дела.
