package com.example.silversticker.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object SilverMotion {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> emphasizedEnter(durationMillis: Int = 500) = tween<T>(
        durationMillis = durationMillis,
        easing = EmphasizedDecelerate
    )

    fun <T> emphasizedExit(durationMillis: Int = 220) = tween<T>(
        durationMillis = durationMillis,
        easing = EmphasizedAccelerate
    )

    fun <T> standard(durationMillis: Int = 300) = tween<T>(
        durationMillis = durationMillis,
        easing = Standard
    )

    fun <T> expressiveSpring() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> quickSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
