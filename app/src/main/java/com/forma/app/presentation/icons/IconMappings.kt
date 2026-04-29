package com.forma.app.presentation.icons

import androidx.annotation.DrawableRes
import com.forma.app.R
import com.forma.app.domain.livecoach.CoachContentType
import com.forma.app.domain.wellness.EnergyLevel

@DrawableRes
fun CoachContentType.iconRes(): Int = when (this) {
    CoachContentType.TECHNIQUE -> R.drawable.ic_coach_technique
    CoachContentType.MISTAKE -> R.drawable.ic_coach_mistake
    CoachContentType.MOTIVATION -> R.drawable.ic_coach_motivation
    CoachContentType.FACT -> R.drawable.ic_coach_fact
    CoachContentType.TIP -> R.drawable.ic_coach_tip
}

@DrawableRes
fun EnergyLevel.iconRes(): Int = when (this) {
    EnergyLevel.FATIGUED -> R.drawable.ic_energy_fatigued
    EnergyLevel.NORMAL -> R.drawable.ic_energy_normal
    EnergyLevel.ENERGIZED -> R.drawable.ic_energy_energized
}
