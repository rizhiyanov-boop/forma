# ТЗ: UX/UI и текстовая полировка

**Тип задачи:** улучшения качества UI и текста.
**Источник:** ревью реальных скриншотов из текущей сборки (29 апреля 2026).
**Приёмка:** компиляция без ошибок, тесты проходят, визуальное качество приложения значительно выше.
**Не делается:** новая функциональность. Только полировка существующего.

---

## Структура

ТЗ из 3 блоков, делать в любом порядке:

- **Блок A** — UI/UX-фиксы (5 критичных + 13 средних/мелких)
- **Блок B** — Текстовые правки в коде
- **Блок C** — Контентные правки в JSON-пулах

Все блоки независимые, можно параллелить.

---

# Блок A. UI/UX

## A.1 🔴 Дублирование «Следующий шаг» в WorkoutScreen

### Проблема

В `WorkoutScreen.kt` сейчас:
- **Сверху**: `FormaCard` «Следующий шаг» с CTA «Открыть подход» (градиент)
- **В списке**: первая карточка упражнения с подходом «Подход 1 → Заполнить»

**Это один и тот же подход.** Юзер видит призыв к одному и тому же действию **дважды**.

### Решение

**Убрать `FormaCard` «Следующий шаг» сверху полностью.** Оставить только список упражнений.

В списке — **подсветить активный pending** так чтобы он выделялся:
- Карточка с активным первым pending получает `elevated = true`
- Текст «Заполнить» в активной строке — `AccentLime` Bold вместо обычного
- Возможно тонкая лаймовая полоска слева у активной карточки упражнения

### Как реализовать

```kotlin
// В WorkoutScreen — удалить блок "Следующий шаг" целиком (весь FormaCard с этим текстом).

// В ExerciseFlowCard добавить параметр:
@Composable
private fun ExerciseFlowCard(
    log: ExerciseLog,
    isActive: Boolean = false,    // НОВОЕ — это упражнение содержит активный pending
    onOpenSet: (SetLog) -> Unit
) {
    FormaCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = isActive,
        padding = PaddingValues(14.dp)
    ) {
        // ...
    }
}

// В рендере списка:
items(s.exerciseLogs) { log ->
    val isActiveExercise = nextPending?.exerciseId == log.exerciseId
    ExerciseFlowCard(
        log = log,
        isActive = isActiveExercise,
        onOpenSet = { ... }
    )
}

// Внутри ExerciseFlowCard для каждого set:
val isFirstPending = !done && set.id == log.sets.firstOrNull { !it.isCompleted }?.id
val isActivePending = isActive && isFirstPending

if (isActivePending) {
    ActionChip(
        text = "Заполнить",
        textColor = AccentLime,
        onClick = { onOpenSet(set) }
    )
} else if (!done) {
    ActionChip(
        text = "Заполнить",
        onClick = { onOpenSet(set) }
    )
} else {
    Text("Готово", color = AccentLime, ...)
}
```

⚠️ Параметр `textColor` нужно добавить в `ActionChip` если его нет, или использовать другой компонент для активного состояния.

## A.2 🔴 AdaptationCard слабая визуально

### Проблема

В `RestScreen.kt` карточка реакции LiveCoach (`AdaptationCard`) выглядит идентично обычным coach-карточкам контента — то же `FormaCard`, тот же фон, те же отступы. Только eyebrow «КОРРЕКТИРОВКА» отличается. **Главная фича приложения визуально не выделена.**

Также кнопки `TertiaryButton` «Оставить текущий · Принять» с разделителем `•` — старомодно и слабо.

### Решение

**1. Визуально выделить AdaptationCard:**
- `elevated = true`
- Лаймовый бордер 1.5dp вокруг карточки
- Заголовок (`reaction.shortMessage`) увеличить до `titleMedium` Bold (вместо `bodyMedium`)

**2. Кнопки сделать полноразмерными:**
- Заменить две `TertiaryButton` с `•` на Row из двух полноценных кнопок
- «Оставить текущий» — `SecondaryButton` (с border'ом)
- «Принять» — `PrimaryButton` (но компактный, без `fillMaxWidth`)

**3. Убрать красный цвет для DECREASE.**

В коде сейчас:
```kotlin
val isDecrease = reaction.nextSetSuggestion?.type == WeightAdjustment.DECREASE
val accent = if (isDecrease) ErrorRed else AccentLime
```

Это **неправильное** дизайн-решение — DECREASE это **нормальная** адаптация под комфорт (философия LiveCoach), не «плохо». Юзер видит красный → думает «что-то плохое случилось».

Заменить на:
```kotlin
val accent = AccentLime  // для всех типов adaptation
```

Красный оставить **только** для `verdict == TOO_HEAVY` или `verdict == UNUSUAL` — там реальный alarm.

### Реализация

```kotlin
@Composable
private fun AdaptationCard(
    reaction: SetReaction,
    status: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // Цвет акцента: красный только для реальных алармов
    val isAlarm = reaction.verdict == SetVerdict.TOO_HEAVY || 
                  reaction.verdict == SetVerdict.UNUSUAL
    val accent = if (isAlarm) WarnAmber else AccentLime
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceElevated)
            .border(1.5.dp, accent, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Eyebrow + status badge как сейчас
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "КОРРЕКЦИЯ",  // см. блок B — короче
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (status != "PENDING") {
                    Box(
                        modifier = Modifier
                            .background(SurfaceHighlight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (status == "APPLIED") "ПРИНЯТО" else "ОСТАВЛЕНО",
                            color = if (status == "APPLIED") AccentLime else TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Главное сообщение — крупнее и Bold
            Text(
                reaction.shortMessage,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium  // вместо bodyMedium
            )
            
            reaction.nextSetSuggestion?.rationale?.let {
                Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            // Кнопки — полноразмерные в Row
            if (status == "PENDING" && reaction.nextSetSuggestion != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = "Оставить",
                        modifier = Modifier.weight(1f),
                        hapticOnClick = HapticType.Soft,
                        onClick = onDecline
                    )
                    PrimaryButton(
                        text = "Принять",
                        modifier = Modifier.weight(1f),
                        hapticOnClick = HapticType.Strong,
                        onClick = onAccept
                    )
                }
            }
        }
    }
}
```

⚠️ `PrimaryButton` сейчас всегда `fillMaxWidth().height(56.dp)` — может быть слишком высокий для inline в Row. Возможно нужно сделать вариант компактнее (44dp) — добавить параметр `compact: Boolean = false`. Или оставить 56dp если визуально ок.

## A.3 🔴 «Заменить упражнение» — постоянный лаймовый шум

### Проблема

В `WorkoutDetailScreen.kt` каждая карточка упражнения имеет **постоянно видимую** строку «↔ Заменить упражнение» в лаймовом цвете. На экране плана из 8 упражнений — 8 ярко-лаймовых строк, отвлекающих от просмотра тренировки.

Replace — **исключительная** функция (травма, нет оборудования, скучно). Не должна быть визуально равноправна с просмотром.

### Решение

**Убрать постоянно видимую строку. Сделать через long-press (1 секунда удержания) на карточке.**

При long-press открывается тот же bottom sheet что и сейчас.

### Реализация

```kotlin
@Composable
private fun ExerciseRow(
    exercise: Exercise,
    onOpen: () -> Unit,
    onReplace: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    FormaCard(padding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onReplace()
                    }
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MuscleBadge(exercise)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, ...)
                Spacer(Modifier.height(2.dp))
                Text(buildSetsLabel(exercise), color = TextSecondary, ...)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(22.dp))
        }
    }
}
```

⚠️ Импорт: `androidx.compose.foundation.combinedClickable` — стандартный Compose API.

**Discoverability** — юзеры не сразу узнают про long-press. Решение: при первом открытии WorkoutDetailScreen в сессии показать **subtle hint** внизу экрана:

```kotlin
// Например — небольшая надпись над "Начать тренировку":
Text(
    "Удерживай упражнение, чтобы заменить",
    color = TextTertiary,
    style = MaterialTheme.typography.labelSmall,
    modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
    textAlign = TextAlign.Center
)
```

Hint показывать **только если юзер ни разу не использовал replace** (флаг в SharedPreferences/DataStore: `has_used_replace`). После первого использования — больше не показываем.

⚠️ Это **дополнительная** работа на флаг. Если делать сложно — можно просто убрать постоянный лаймовый текст и не показывать hint вообще. Юзеры через time узнают.

## A.4 🔴 PlayArrow для «Готово» в Home

### Проблема

В `HomeScreen.kt` функция `WorkoutRow` для выполненных тренировок показывает **зелёную PlayArrow** иконку. Это семантически неверно — Play = «начать», а не «готово». Юзер видит и думает «можно нажать чтобы начать ещё раз».

### Решение

Заменить на `Icons.Rounded.Check` (галочка).

```kotlin
// В WorkoutRow:
if (isDone) {
    Icon(Icons.Rounded.Check, null, tint = AccentLime)
}
```

### Дополнительно — A.4.1 «Готово» бейдж тоже выглядит как CTA

Бейдж справа с градиентом + чёрным жирным текстом «Готово» **визуально равен** primary button «Открыть тренировку» в TodayCard. Юзер может тапать.

Решение: убрать градиент бейджа, сделать в `SurfaceHighlight` фон + лаймовый текст:

```kotlin
if (isDone) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHighlight)  // вместо AccentGradient
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "Готово",
            color = AccentLime,             // вместо AccentDark
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
```

## A.5 🔴 Эмодзи иконки в Pre/Post wellness picker'ах

### Проблема

Иконки в `res/drawable/ic_energy_*.xml` нарисованы как **белые системные смайлики** — белый круг + чёрные глаза + рот. Без цветовой дифференциации между состояниями. На скрине 3 одинаковых белых кружка, отличаются только формой рта.

Это:
- Выглядит дёшево, как Material default emojis
- Не в дизайн-системе проекта (всё остальное — лайм + тёмно-серое)
- Не даёт **быстрой визуальной классификации** между состояниями

### Решение

**Переделать SVG-иконки** под дизайн-систему. Без круглых смайликов. Использовать **абстрактные визуальные метафоры**:

**FATIGUED** — три горизонтальные затухающие линии (сигнал слабый):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="32dp"
    android:height="32dp"
    android:viewportWidth="32"
    android:viewportHeight="32">
    <!-- Три линии, ширина уменьшается, цвет приглушенный -->
    <path android:fillColor="#FF6B6B70" android:pathData="M6,10h20v3H6z"/>
    <path android:fillColor="#FF6B6B70" android:pathData="M9,16h14v3H9z"/>
    <path android:fillColor="#FF6B6B70" android:pathData="M12,22h8v3h-8z"/>
</vector>
```

**NORMAL** — три равные горизонтальные линии (стабильный сигнал):
```xml
<vector ...>
    <path android:fillColor="#FFA1A1AA" android:pathData="M6,10h20v3H6z"/>
    <path android:fillColor="#FFA1A1AA" android:pathData="M6,16h20v3H6z"/>
    <path android:fillColor="#FFA1A1AA" android:pathData="M6,22h20v3H6z"/>
</vector>
```

**ENERGIZED** — три горизонтальные растущие линии (сигнал сильный):
```xml
<vector ...>
    <path android:fillColor="#FFD4FF3A" android:pathData="M12,10h8v3h-8z"/>
    <path android:fillColor="#FFD4FF3A" android:pathData="M9,16h14v3H9z"/>
    <path android:fillColor="#FFD4FF3A" android:pathData="M6,22h20v3H6z"/>
</vector>
```

Это даёт:
- **Цветовая дифференциация**: серый (низкий сигнал) → светло-серый (норма) → лайм (высокий сигнал)
- **Форма**: затухающая → стабильная → растущая
- В стиле приложения (геометрия, не картинки)
- Всё ещё узнаваемо как «низкий/средний/высокий»

⚠️ Альтернативная метафора если три-линии не нравится: **bar chart с 3 столбцами**:
- FATIGUED — 1 серый бар на 3 позициях, остальные пустые
- NORMAL — 2 светло-серых бара
- ENERGIZED — 3 лаймовых бара

Эта метафора более universal, как сигнал WiFi или battery level.

### Размер

В коде иконки используются как `Icon` без явного размера — Material берёт 24dp по умолчанию. Можно увеличить до 32dp в самом UI:

```kotlin
Icon(
    painter = painterResource(level.iconRes()),
    contentDescription = null,
    tint = Color.Unspecified,
    modifier = Modifier.size(32.dp)  // НОВОЕ
)
```

### Текст под иконкой

В Post-workout кнопках выбранный вариант имеет **лаймовый текст**, невыбранные — `TextSecondary`. Хорошо. Но иконка не меняет цвет при selected — она всегда «своего цвета». Это окей для паттерна «иконка = состояние» (FATIGUED всегда серая, ENERGIZED всегда лайм).

---

## A.6 🟡 Eyebrow смещён вправо в TopBar

### Проблема

На экранах Review и Progress eyebrow («РАЗБОР» / «ПРОГРЕСС») смещён вправо относительно заголовка под ним. Должны быть выровнены по одной left-edge.

### Решение

Найти TopBar в `ReviewScreen.kt` и в `ProgressScreen.kt` (или где они), проверить отступы. Скорее всего eyebrow обёрнут в `Column` с дополнительным `padding(start = X)`. Убрать этот padding или применить его к обоим элементам.

```kotlin
// Должно быть так:
Column(modifier = Modifier.padding(start = 0.dp)) {  // одинаковый отступ
    Text("РАЗБОР", color = AccentLime, style = labelMedium, ...)
    Text("Тренировка завершена", color = TextPrimary, fontWeight = Black, ...)
}
```

Также проверить `WorkoutDetailScreen` (там тоже eyebrow + title) — на скрине Image 2 («СРЕДА» + «Среда — лёгкий») визуально нормально, но всё равно сверить.

## A.7 🟡 Текст обрезается в pills

### Проблема

В `ReplaceExerciseSheet` chip «Нет оборудован» обрезает «Нет оборудования» — последние буквы съедаются границей.

В `ProgressScreen` filter pills для рабочего веса — «Вертикал…» обрезается за правым краем.

### Решение

**Для ReplaceExerciseSheet:** проверить что 4 chip'а в `Row` с `weight(1f)` — слишком тесно. Решения:

```kotlin
// Вариант 1: горизонтальный скролл
Row(
    modifier = Modifier
        .horizontalScroll(rememberScrollState())
        .fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    ReasonChip("Боль или травма", ...)
    ReasonChip("Нет инвентаря", ...)        // короче чем "Нет оборудования"
    ReasonChip("Слишком сложно", ...)
    ReasonChip("Скучно", ...)
}

// Вариант 2: FlowRow (Compose Foundation 1.4+)
FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    ReasonChip("Боль или травма", ...)
    // ... остальные
}
```

⚠️ Я бы выбрал **FlowRow** — chip'ы переносятся на новую строку при необходимости. Не нужен скролл, всё видно.

**Для ProgressScreen filter pills:** там список упражнений динамический, может быть много. **Только горизонтальный скролл подойдёт:**

```kotlin
LazyRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 20.dp)
) {
    items(exercises) { exercise ->
        ExerciseFilterPill(
            text = exercise.name,
            selected = selectedId == exercise.id,
            onClick = { selectedId = exercise.id }
        )
    }
}
```

## A.8 🟡 Цвета eyebrow по типу coach контента

### Проблема

В `RestScreen.CoachContentCard` все типы (TECHNIQUE, MISTAKE, MOTIVATION, FACT, TIP) имеют одинаковый лаймовый eyebrow. Юзер не различает типы по цвету — только по маленькой иконке слева.

### Решение

Цвет eyebrow и иконки разный по типу:

```kotlin
private fun CoachContentType.accentColor(): Color = when (this) {
    CoachContentType.TECHNIQUE -> AccentLime         // главное — техника
    CoachContentType.MISTAKE -> WarnAmber            // предупреждение
    CoachContentType.MOTIVATION -> AccentMint        // мятный — поддержка
    CoachContentType.FACT -> InfoBlue                // нейтральная инфа
    CoachContentType.TIP -> AccentMint               // совет — мятный
}

// В CoachContentCard:
@Composable
private fun CoachContentCard(
    type: CoachContentType,
    content: CoachContentItem,
    indexInDeck: Int,    // для индикатора n/N
    deckSize: Int,
    onClick: () -> Unit
) {
    FormaCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), padding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(type.iconRes()),
                    contentDescription = null,
                    tint = type.accentColor()    // НОВОЕ — цвет иконки
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    type.displayName.uppercase(),
                    color = type.accentColor(),  // НОВОЕ — цвет eyebrow
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${indexInDeck + 1}/$deckSize",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(content.text, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

⚠️ Body всегда `TextPrimary` — единый цвет для читаемости. Только eyebrow и иконка цветные.

⚠️ **Иконки в drawable** — сейчас `ic_coach_*.xml` имеют hardcoded цвет внутри. Нужно проверить: либо переделать на `tint`-friendly (одноцветные path с белым), либо принять что цвет иконки из drawable.

Проверить через `cat /res/drawable/ic_coach_technique.xml` — если там много цветов, оставить как есть и красить только eyebrow. Если одноцветная — добавить `tint = type.accentColor()` в `Icon(...)`.

## A.9 🟡 Header «Отдых» дублирует eyebrow «отдых»

### Проблема

В `RestScreen.kt`:
- Header сверху: ArrowBack + «Отдых» (titleLarge)
- В `RestTimerHero`: eyebrow «ОТДЫХ» (labelMedium ALL CAPS) над таймером

**Дублирование слова «отдых» в двух местах одного экрана.**

### Решение

Убрать eyebrow «ОТДЫХ» в `RestTimerHero`. Header выше уже даёт контекст. Над цифрами таймера ничего, или просто полное отсутствие eyebrow.

```kotlin
@Composable
private fun RestTimerHero(seconds: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = "%d:%02d".format(seconds / 60, seconds % 60),
            color = AccentLime,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 96.sp,
            style = MaterialTheme.typography.displayLarge
        )
    }
}
```

## A.10 🟡 RIR null state — два разных способа

### Проблема

В SetEntryScreen два разных визуальных языка для отсутствия RIR:
- Image 7: значение `—` (длинное тире) + лаймовая надпись «RIR не указан» (статус)
- Image 8: значение `1` (число) + лаймовая надпись «Не помню RIR» (toggle)

Два разных текста, две разные семантики, визуально похоже.

### Решение

**Один toggle везде**, единая семантика. Реализация уже есть в коде (на Image 8) — оставить её. Image 7 (отображение `—` + текст «RIR не указан») удалить.

Alright, проверю код:

```kotlin
// В SetEntryScreen должна быть одна стабильная логика:

var rirSkipped by remember(set?.id) { mutableStateOf(false) }
var rir by remember(set?.id) { mutableIntStateOf(2) }

// При сохранении:
val rirToSave = if (rirSkipped) null else rir
vm.completeSetAndAnalyze(rir = rirToSave, ...)

// UI:
NumberAdjustCard(
    title = "RIR",
    value = if (rirSkipped) "—" else rir.toString(),
    onMinus = { 
        if (rirSkipped) {
            rirSkipped = false  // тап на минус выключает skipped
        } else if (rir > 0) {
            rir -= 1 
        }
    },
    onPlus = { 
        if (rirSkipped) {
            rirSkipped = false  // тап на плюс выключает skipped
        } else if (rir < 10) {
            rir += 1 
        }
    }
)

// Toggle — одинаковый текст, меняется состояние
Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End
) {
    Text(
        text = if (rirSkipped) "RIR не указан" else "Не помню RIR",
        color = if (rirSkipped) TextSecondary else AccentLime,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clickable { rirSkipped = !rirSkipped }
            .padding(8.dp)
    )
}
```

Логика:
- По умолчанию RIR показан как число, тап toggle → переходит в «не помню» (`—` + статус серым)
- Из «не помню» можно вернуться: либо тап toggle, либо тап `+`/`-` (это intuitive — пытаешься изменить значение, значит выходим из null state)

## A.11 🟡 «Завершить тренировку» — fixed footer

### Проблема

В `WorkoutScreen.kt` кнопка «Завершить тренировку» в LazyColumn последним item. На длинной тренировке (8+ упражнений) приходится скроллить чтобы её увидеть.

### Решение

Вынести из LazyColumn в фиксированный footer:

```kotlin
Column(Modifier.fillMaxSize()) {
    // ... TopBar, ProgressBar
    
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(s.exerciseLogs) { log -> ExerciseFlowCard(...) }
    }
    
    // Fixed footer
    Box(
        Modifier
            .fillMaxWidth()
            .background(BgBlack)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        PrimaryButton(
            text = if (ui.isFinishing) "Завершаю…" else "Завершить тренировку",
            enabled = !ui.isFinishing,
            hapticOnClick = HapticType.Strong,
            onClick = { vm.finish(onFinished) }
        )
    }
}
```

Дополнительно: **показывать кнопку только когда есть смысл**. Если выполнено 0% подходов — нет смысла «завершать». 

Можно условие: показывать когда `s.completionRatio > 0` (хотя бы 1 подход выполнен). Если 0 — кнопка либо disabled, либо отсутствует.

## A.12 🟡 «Закрыть и не показывать» — destructive как secondary

### Проблема

В `ReviewScreen.kt` кнопка «Закрыть и не показывать» — `SecondaryButton`. После неё **рекомендации потеряны для этого review навсегда**. Это destructive action которая выглядит слишком заметно.

### Решение

Сделать `TertiaryButton` (мелкий текст):

```kotlin
if (r != null && !r.isDismissed && r.recommendations.isNotEmpty()) {
    Spacer(Modifier.height(4.dp))
    TertiaryButton(
        text = "Скрыть рекомендации",  // см. блок B про текст
        textColor = TextTertiary,
        modifier = Modifier.fillMaxWidth(),
        hapticOnClick = HapticType.Soft,
        onClick = {
            vm.dismiss()
            onDone()
        }
    )
}
```

⚠️ TertiaryButton сейчас align=`CenterStart`. Для footer-action лучше центрировать:

```kotlin
@Composable
fun TertiaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = TextSecondary,
    align: Alignment.Horizontal = Alignment.Start,  // НОВЫЙ параметр
    hapticOnClick: HapticType = HapticType.Soft,
    onClick: () -> Unit
) { ... }
```

Или просто обернуть в Box с `contentAlignment = Center`:

```kotlin
Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    TertiaryButton(text = "Скрыть рекомендации", ...)
}
```

⚠️ Дополнительная мысль: можно добавить **AlertDialog подтверждение**:

«Скрыть рекомендации? Их нельзя будет вернуть для этой тренировки.»

Если делать — стандартный Material AlertDialog. Это **дополнительная вежливость**, можно отложить.

## A.13 🟡 Видео-плейсхолдер в ExerciseDetail

### Проблема

В `ExerciseDetailScreen.kt` блок «Видео-демонстрация скоро будет» — серый прямоугольник с play-иконкой посередине. Если видео не будет в ближайшие месяцы, юзер каждый раз видит мёртвое пространство.

### Решение

**Скрыть блок целиком пока видео нет.** Когда видео появится — добавить flag в `Exercise.videoUrl` и показывать только если не null:

```kotlin
exercise.videoUrl?.let { url ->
    VideoPlayer(url = url, modifier = ...)
}
```

Сейчас в `Exercise` модели нет поля `videoUrl` — добавлять пока не нужно. Просто **удалить блок плейсхолдера** из `ExerciseDetailScreen.kt`.

## A.14 🟡 SetEntry — пустота под кнопкой

### Проблема

После RIR-карточки и до кнопки «Сохранить и отдых» — половина экрана пустая. Это потенциал для контекстной фичи.

### Решение

Это **не блокер** для текущего ТЗ. Но можно добавить **мини-график предыдущих подходов** этого упражнения:

```kotlin
// Если есть данные по предыдущим тренировкам этого упражнения
val previousSets = vm.getPreviousSetsFor(exerciseId, last = 5)
if (previousSets.isNotEmpty()) {
    PreviousSetsHint(sets = previousSets)
}
```

`PreviousSetsHint` — компактный line chart 5 последних рабочих подходов. Высота 80dp, минимальный текст.

⚠️ **Это новая фича.** Можно отложить если ТЗ велико. Пометить как **«если есть время»**.

## A.15 🟡 Stats-карточки в Progress — нет gap

### Проблема

На `ProgressScreen` 3 stats-карточки сверху прижаты друг к другу.

### Решение

Проверить `Row` со stats-карточками — добавить `horizontalArrangement = Arrangement.spacedBy(10.dp)`.

Скорее всего проблема в том что в `Row` нет explicit spacing.

## A.16 🟡 Бары мышечных групп — min-width

### Проблема

В Progress «Группы мышц» бар для «Кор» (значение 72) при максимуме «Спина» (5000) — едва видимая полоска.

### Решение

Добавить **минимальную ширину бара** относительно контейнера:

```kotlin
val maxValue = muscleGroups.maxOf { it.volume }
val ratio = (group.volume / maxValue).toFloat()
val barWidthFraction = ratio.coerceAtLeast(0.04f)  // минимум 4% ширины

Box(
    modifier = Modifier
        .fillMaxWidth(barWidthFraction)
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(AccentGradient)
)
```

`coerceAtLeast(0.04f)` гарантирует что даже самый малый бар будет 4% ширины — видимая полоска.

## A.17 🟡 Empty state в Progress

### Проблема

На скрине у юзера 1 ненулевая точка в графике «Объём по неделям» — остальные 8 нулевые. График выглядит странно.

### Решение

Если у юзера **меньше 2 ненулевых точек данных** в любом графике — показывать **empty state карточку** вместо графика:

```kotlin
@Composable
private fun ChartOrEmpty(data: List<ChartPoint>, content: @Composable () -> Unit) {
    val nonZeroPoints = data.count { it.value > 0 }
    if (nonZeroPoints < 2) {
        FormaCard(padding = PaddingValues(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.BarChart, null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Мало данных",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Сделай ещё пару тренировок — здесь появится динамика",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        content()
    }
}
```

## A.18 🟡 Иконка настроек на Home — слишком приглушенная

### Проблема

На Home (Image 1) IconButton настроек справа сверху — `tint = TextSecondary` (приглушенно-серый). Выглядит как декорация, а не интерактивный элемент.

### Решение

Сделать **с фоном кружком** для подчёркивания тапаемости:

```kotlin
Box(
    modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(SurfaceDark)
        .clickable { onOpenSettings() },
    contentAlignment = Alignment.Center
) {
    Icon(
        Icons.Rounded.Settings,
        contentDescription = "Настройки",
        tint = TextSecondary,
        modifier = Modifier.size(20.dp)
    )
}
```

Кружок-фон делает кнопку **похожей на кнопку**, а не на текст.

---

# Блок B. Тексты в коде

## B.1 🔴 Hardcoded строки

### B.1.1 «Нет оборудован» → «Нет инвентаря»

В `WorkoutDetailScreen.kt` (или где определены reasons для replace):

```kotlin
// Найти:
"Нет оборудован"     // обрезается
// или:
"Нет оборудования"   // длинное

// Заменить на:
"Нет инвентаря"      // короткое и помещается
```

### B.1.2 «Сохранить и отдых» → «Записать подход»

В `SetEntryScreen.kt`:
```kotlin
PrimaryButton(
    text = "Записать подход",   // вместо "Сохранить и отдых"
    ...
)
```

### B.1.3 «Заменить упражнение» → удалить

После A.3 эта строка вообще не появляется в UI (long-press вместо постоянной кнопки). Удалить её из кода.

В bottom sheet оставить «Замена упражнения» как заголовок раздела (там это уместно).

### B.1.4 «КОРРЕКТИРОВКА» → «КОРРЕКЦИЯ»

В `RestScreen.kt`:
```kotlin
// AdaptationCard eyebrow
Text(
    "КОРРЕКЦИЯ",   // вместо "КОРРЕКТИРОВКА"
    color = accent,
    ...
)
```

### B.1.5 «Закрыть и не показывать» → «Скрыть рекомендации»

В `ReviewScreen.kt`:
```kotlin
TertiaryButton(
    text = "Скрыть рекомендации",   // вместо "Закрыть и не показывать"
    ...
)
```

### B.1.6 «Подобрать варианты» → «Найти замены»

В `ReplaceExerciseSheet`:
```kotlin
PrimaryButton(
    text = "Найти замены",   // вместо "Подобрать варианты"
    ...
)
```

### B.1.7 «Опционально: укажи причину…» → «Хочешь — расскажи почему. Так точнее»

В `ReplaceExerciseSheet`:
```kotlin
Text(
    "Хочешь — расскажи почему. Так точнее.",
    color = TextSecondary,
    ...
)
```

### B.1.8 «Например: болит плечо, нет скамьи…» → без многоточия

```kotlin
placeholder = "Например: болит плечо, нет скамьи"
```

### B.1.9 Loading-состояния — безличный тон

**Сейчас от первого лица (AI говорит):**
- «Загружаю программу…»
- «Составляю программу…»
- «Анализирую тренировку…»
- «Завершаю тренировку…»

**Заменить на безличные:**
- «Загрузка программы…»
- «Составление программы…»
- «Анализ тренировки…»
- «Завершение тренировки…»

⚠️ Это **спорное** решение. Если хочется сохранить «AI-личность» — оставить от первого лица **везде где AI реально что-то делает**. Я бы выбрал безличный — продукт это **инструмент**, а не псевдо-друг.

### B.1.10 «На главную» → «К следующей тренировке»

В `ReviewScreen.kt` главный CTA:
```kotlin
PrimaryButton(
    text = "К следующей тренировке",
    leadingIcon = null,    // убрать галочку Check — она не уместна в этом тексте
    onClick = onDone
)
```

⚠️ Если у юзера сегодня нет следующей запланированной — текст не работает. Условие:

```kotlin
val nextWorkoutText = if (vm.hasNextWorkoutToday()) {
    "К следующей тренировке"
} else {
    "Готово"
}
```

Или **оставить «На главную»** — это безопаснее. Я бы выбрал **«Готово»** — короткое, всегда верное.

### B.1.11 Программа описание на Home

Сейчас:
> «Push/Pull сплит на 3 дня. Понедельник — основной (ширина + бицепс), среда — лёгкий (техника + памп), пятница — силовой (жим + закрепление).»

Длинно. Сократить:
```kotlin
// В preset/MyProgramPreset.kt:
description = "Push/Pull сплит на 3 дня. Понедельник — основной, среда — лёгкий, пятница — силовой."
```

Подробности дней юзер увидит при тапе на конкретный день.

## B.2 🟡 Стиль голоса — гайдлайн

В файле `docs/TONE_OF_VOICE.md` зафиксировать стиль:

```markdown
# Тон голоса в Forma

## Контексты и тон

### Карточки в моменте (отдых, советы, ошибки)
- Командное «ты»
- Императив: «Веди локтями», «Подними локти»
- Кратко (5-15 слов)

### AI-разбор после тренировки
- Уважительный «ты»
- Описательно, не приказно: «Махи отрабатывал чётко»
- Полные предложения

### Loading и системные сообщения
- Безличный тон
- «Загрузка программы», не «Загружаю»
- Без эмоций

### Кнопки CTA
- Глагол императивом
- Кратко (1-3 слова)
- Конкретно: «Записать подход», не «Сохранить»

## Что не делать

- ❌ Никаких "Опционально", "Подобрать варианты" (англицизмы где есть русский эквивалент)
- ❌ Никаких многоточий в плейсхолдерах
- ❌ Никаких «не стыд», «не позорно» (отрицательные конструкции)
- ❌ Никаких длинных ALL CAPS слов (>10 символов в caps)
```

---

# Блок C. Контентные правки в JSON-пулах

## Задача

Пройтись по всем 11 файлам в `app/src/main/assets/coach_content/` и применить style guide:

- **Длина**: 5-15 слов на карточку (сейчас часто 20-30)
- **Тон**: командное «ты», императив
- **Без двойных отрицаний**: «не стыд» → «нормально»
- **Без сложных терминов** без объяснения

## C.1 Конкретные правки которые я нашёл

### C.1.1 squat.json (или где middle-delta-side-raise карточки)

**Найти:**
> «Подъём чуть перед корпусом часто называют плоскостью лопатки. Многим плечам там комфортнее, чем строго сбоку»

**Заменить на:**
> «Поднимай чуть вперёд от корпуса — в плоскости лопатки. Так дельтам комфортнее, чем строго сбоку»

### C.1.2 То же упражнение, УСТАНОВКА

**Найти:**
> «Маленькие гантели здесь не стыд. Средняя дельта любит точность, а не демонстрацию силы»

**Заменить на:**
> «Маленький вес работает лучше большого. Средней дельте нужна точность, не сила»

### C.1.3 ТЕХНИКА (то же упражнение)

**Найти:**
> «Веди движение локтями, а не кистями. Представь, что локти поднимают гантели в стороны»

**Заменить на:**
> «Веди локтями, не кистями. Локти поднимают — кисти просто держат»

### C.1.4 ФАКТ (то же упражнение)

**Найти:**
> «В махах длинный рычаг: небольшая гантель создаёт большую нагрузку на плечо. Поэтому вес растёт медленно»

**Оставить или сократить:**
> «В махах длинный рычаг — даже маленькая гантель грузит плечо. Поэтому вес растёт медленно»

### C.1.5 ТЕХНИКА (другое упражнение)

**Найти:**
> «В верхней точке задержись на мгновение. Если не можешь остановить гантели, вес слишком тяжёлый»

**Заменить на:**
> «В верхней точке задержись на мгновение. Не можешь остановить — вес тяжёлый»

## C.2 Полная вычитка всех 11 файлов

После конкретных правок выше — **пройтись по каждой карточке** во всех 11 файлах. Проверить:

1. **Длина**: если >20 слов — сократить
2. **Тон**: должно быть «ты», императив
3. **Двойные отрицания**: убрать
4. **Жаргон**: «памп», «деки», «суперсет» — оставить если целевая аудитория продвинутая, либо объяснить в первом упоминании

⚠️ Этот пункт **трудоёмкий** — может быть 100+ карточек. Можно делать **итеративно** — за один заход не всё, по 2-3 файла.

## C.3 EXERCISE_CONTENT_REVIEW_PROMPT.md

В существующий prompt-template (`forma/docs/` или `app/src/main/assets/`) для генерации новых пулов добавить **language guidelines**:

```markdown
## Language Style Rules

- 5-15 слов на карточку максимум
- Командное «ты», императив для action-карточек
- Без двойных отрицаний («не стыд», «не позорно»)
- Без излишних англицизмов где есть русский эквивалент
- Без многоточий внутри текста
- Терминологию вводить с объяснением при первом использовании
```

Чтобы будущие сгенерированные карточки сразу соответствовали стилю.

---

# Раздел D. Очерёдность работ

Чтобы можно было сдавать инкрементально:

1. **B.1.1-B.1.10** — текстовые правки в коде (1 час, изолированные)
2. **A.4 + A.4.1** — PlayArrow/бейдж в Home (15 минут)
3. **A.10** — RIR null state унификация (30 минут)
4. **A.7** — pills overflow (30 минут)
5. **A.6** — eyebrow alignment (15 минут)
6. **A.9** — убрать дублирование «Отдых» (5 минут)
7. **A.11** — fixed footer для Завершить (30 минут)
8. **A.12** — TertiaryButton для Скрыть рекомендации (15 минут)
9. **A.15 + A.16** — Progress polish (30 минут)
10. **A.17** — empty state Progress (45 минут)
11. **A.18** — иконка настроек кружком (15 минут)
12. **A.13** — удалить видео-плейсхолдер (5 минут)
13. **A.1** — дублирование «Следующий шаг» в Workout (1 час)
14. **A.2** — AdaptationCard визуальное усиление (1 час)
15. **A.3** — long-press для Replace (1 час)
16. **A.5** — переделать SVG иконки энергии (1 час)
17. **A.8** — цвета eyebrow по типу coach контента (45 минут)
18. **A.14** — мини-график на SetEntry (опционально, 2-3 часа)
19. **B.2** — TONE_OF_VOICE.md style guide (15 минут)
20. **C.1** — конкретные правки контента (30 минут)
21. **C.2** — полная вычитка всех 11 JSON (2-4 часа, итеративно)
22. **C.3** — обновить prompt-template (15 минут)

**Итого:** 12-15 часов работы Claude Code на полный объём.

Если урезать до **минимума без A.14 (мини-график) и C.2 (полная вычитка JSON)**: 8-10 часов.

---

# Раздел E. Что НЕ делается

❌ Новая функциональность
❌ Изменения дизайн-системы (палитра, типографика, базовые компоненты — остаются как есть)
❌ Анимации/transitions помимо существующих
❌ Звуковые эффекты
❌ Backend-зависимые улучшения (видео уроки, push notifications)
❌ Локализация на другие языки
❌ Полная переработка Onboarding (пока не критично)

---

# Раздел F. Стиль и общие правила

- Hilt для DI
- Coroutines для асинхронности
- Material 3 + Compose для UI
- Цвета и типографика — через `presentation/theme`
- Существующие компоненты `FormaCard`, `PrimaryButton`, `SecondaryButton`, `TertiaryButton`, `ActionChip` переиспользуем
- Iconography через `androidx.compose.material.icons.rounded.*` или drawable resources
- Logger тег `Forma.{Screen}` где уместно

После закрытия этого ТЗ:
- Все критичные UX-проблемы решены
- Все hardcoded строки в едином тоне
- Контент в карточках LiveCoach соответствует style guide
- Иконки в дизайн-системе (без системных смайликов)

Качество приложения вырастет на голову, готовность к beta-тестированию станет реальной.
