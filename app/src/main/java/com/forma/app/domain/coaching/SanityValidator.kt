package com.forma.app.domain.coaching

/**
 * Финальная проверка алгоритмических рекомендаций перед применением/показом.
 *
 * Защищает от:
 *  - резких скачков веса (+5% / -15% за раз)
 *  - частых DELOAD-ов (не чаще 1 раза в 4 недели)
 *
 * Не "запрещает" рекомендации — только помечает флагом `isDisputed = true`
 * и опционально подменяет на более консервативную. UI может показывать
 * disputed рекомендации иначе (например, со значком ⚠ "проверь сам").
 */
class SanityValidator {

    /**
     * Проверяет рекомендацию, возвращает её же или модифицированную копию.
     *
     * @param rec проверяемая рекомендация
     * @param currentWeight текущий рабочий вес упражнения (для процентных лимитов)
     * @param daysSinceLastDeload сколько дней назад был последний DELOAD по этому
     *        упражнению. Если null или > 999 — DELOAD-ов не было.
     */
    fun validate(
        rec: EngineRecommendation,
        currentWeight: Double,
        daysSinceLastDeload: Int?
    ): SanityResult {
        return when (rec.action) {
            ProgressionAction.INCREASE_WEIGHT ->
                validateIncreaseWeight(rec, currentWeight)
            ProgressionAction.DECREASE_WEIGHT ->
                validateDecreaseWeight(rec, currentWeight)
            ProgressionAction.DELOAD ->
                validateDeload(rec, daysSinceLastDeload)
            // Эти типы не требуют sanity-проверки
            ProgressionAction.INCREASE_REPS,
            ProgressionAction.KEEP,
            ProgressionAction.INSUFFICIENT_DATA ->
                SanityResult(rec, isDisputed = false, reason = null)
        }
    }

    private fun validateIncreaseWeight(
        rec: EngineRecommendation,
        currentWeight: Double
    ): SanityResult {
        val newWeight = rec.newWeightKg ?: return SanityResult(rec, false, null)
        val growthPercent = (newWeight - currentWeight) / currentWeight
        return if (growthPercent > 0.05) {
            // Скачок > 5% — режем до 5%
            val cappedWeight = roundToHalfKg(currentWeight * 1.05)
            SanityResult(
                rec.copy(
                    newWeightKg = cappedWeight,
                    confidence = Confidence.LOW,
                    rationale = rec.rationale +
                        " (ограничено +5%: $currentWeight → $cappedWeight)"
                ),
                isDisputed = true,
                reason = "Скачок более 5% подозрителен — урезано до безопасного"
            )
        } else {
            SanityResult(rec, false, null)
        }
    }

    private fun validateDecreaseWeight(
        rec: EngineRecommendation,
        currentWeight: Double
    ): SanityResult {
        val newWeight = rec.newWeightKg ?: return SanityResult(rec, false, null)
        val dropPercent = (currentWeight - newWeight) / currentWeight
        return if (dropPercent > 0.15) {
            val cappedWeight = roundToHalfKg(currentWeight * 0.85)
            SanityResult(
                rec.copy(
                    newWeightKg = cappedWeight,
                    confidence = Confidence.LOW,
                    rationale = rec.rationale +
                        " (ограничено -15%: $currentWeight → $cappedWeight)"
                ),
                isDisputed = true,
                reason = "Снижение более 15% подозрительно — урезано"
            )
        } else {
            SanityResult(rec, false, null)
        }
    }

    private fun validateDeload(
        rec: EngineRecommendation,
        daysSinceLastDeload: Int?
    ): SanityResult {
        val days = daysSinceLastDeload ?: return SanityResult(rec, false, null)
        return if (days < 28) {
            // DELOAD < 4 недель назад — конвертируем в KEEP
            SanityResult(
                rec.copy(
                    action = ProgressionAction.KEEP,
                    newWeightKg = null,
                    newRepsMin = null,
                    newRepsMax = null,
                    confidence = Confidence.LOW,
                    rationale = "Недавно (${days} дн. назад) уже был DELOAD — " +
                        "ещё рано разгружать. Дай телу время адаптироваться."
                ),
                isDisputed = true,
                reason = "DELOAD не чаще раза в 4 недели"
            )
        } else {
            SanityResult(rec, false, null)
        }
    }

    private fun roundToHalfKg(weight: Double): Double =
        kotlin.math.round(weight * 2.0) / 2.0
}

/**
 * Результат валидации — или исходная рекомендация (всё ок),
 * или модифицированная (помечена disputed).
 */
data class SanityResult(
    val rec: EngineRecommendation,
    val isDisputed: Boolean,
    val reason: String?
)
