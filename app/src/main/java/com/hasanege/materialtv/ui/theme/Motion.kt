package com.hasanege.materialtv.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay

// Material 3 Expressive Motion Specifications
object MotionTokens {
    // Duration tokens
    const val DurationShort1 = 50
    const val DurationShort2 = 100
    const val DurationShort3 = 150
    const val DurationShort4 = 200
    const val DurationMedium1 = 250
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350
    const val DurationMedium4 = 400
    const val DurationLong1 = 450
    const val DurationLong2 = 500
    const val DurationLong3 = 550
    const val DurationLong4 = 600
    const val DurationExtraLong1 = 700
    const val DurationExtraLong2 = 800
    const val DurationExtraLong3 = 900
    const val DurationExtraLong4 = 1000

    // Easing curves for expressive motion
    object Easing {
        // Emphasized easing - for expressive, attention-grabbing motion
        val Emphasized = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val EmphasizedDecelerate = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val EmphasizedAccelerate = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
        
        // Standard easing - for typical transitions
        val Standard = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val StandardDecelerate = androidx.compose.animation.core.CubicBezierEasing(0f, 0f, 0f, 1f)
        val StandardAccelerate = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0f, 1f, 1f)
    }

    // Spring configurations for expressive bouncy animations
    object Springs {
        val Bouncy = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
        
        val Smooth = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
        
        val Quick = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )
    }
}

// Predefined animation specs for common use cases
object ExpressiveAnimations {
    // Fast animations for immediate feedback
    fun <T> fast(): FiniteAnimationSpec<T> = tween(
        durationMillis = MotionTokens.DurationShort4,
        easing = MotionTokens.Easing.Emphasized
    )
    
    // Medium animations for standard transitions
    fun <T> medium(): FiniteAnimationSpec<T> = tween(
        durationMillis = MotionTokens.DurationMedium2,
        easing = MotionTokens.Easing.Emphasized
    )
    
    // Slow animations for expressive, attention-grabbing motion
    fun <T> slow(): FiniteAnimationSpec<T> = tween(
        durationMillis = MotionTokens.DurationLong2,
        easing = MotionTokens.Easing.EmphasizedDecelerate
    )
    
    // Enter animations
    fun <T> enter(): FiniteAnimationSpec<T> = tween(
        durationMillis = MotionTokens.DurationMedium3,
        easing = MotionTokens.Easing.EmphasizedDecelerate
    )
    
    // Exit animations
    fun <T> exit(): FiniteAnimationSpec<T> = tween(
        durationMillis = MotionTokens.DurationShort4,
        easing = MotionTokens.Easing.EmphasizedAccelerate
    )
    
    // Transition specs for AnimatedContent
    val enterTransition = fadeIn(tween(300, easing = MotionTokens.Easing.Emphasized)) +
            scaleIn(tween(300, easing = MotionTokens.Easing.Emphasized), initialScale = 0.9f)

    val exitTransition = fadeOut(tween(300, easing = MotionTokens.Easing.Emphasized)) +
            scaleOut(tween(300, easing = MotionTokens.Easing.Emphasized), targetScale = 0.9f)
}

@Composable
fun Modifier.animateStaggeredEntry(index: Int, durationMillis: Int = 300): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay((index * 30L).coerceAtMost(300L))
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis, easing = MotionTokens.Easing.Emphasized),
        label = "staggered_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "staggered_scale"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        this.scaleX = scale
        this.scaleY = scale
    }
}
