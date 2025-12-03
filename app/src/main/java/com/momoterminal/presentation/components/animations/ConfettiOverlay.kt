package com.momoterminal.presentation.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.momoterminal.presentation.theme.MomoYellow
import com.momoterminal.presentation.theme.SuccessGreen
import com.momoterminal.presentation.theme.InfoBlue
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val startY: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

/**
 * Confetti celebration overlay for successful transactions.
 */
@Composable
fun ConfettiOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    durationMillis: Int = 2000
) {
    if (!isVisible) return
    
    val progress = remember { Animatable(0f) }
    val colors = listOf(MomoYellow, SuccessGreen, InfoBlue, Color(0xFFFF6B6B), Color(0xFF9B59B6))
    
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                startY = Random.nextFloat() * 0.3f,
                speed = 0.3f + Random.nextFloat() * 0.7f,
                size = 8f + Random.nextFloat() * 12f,
                color = colors.random()
            )
        }
    }
    
    LaunchedEffect(isVisible) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, easing = LinearEasing)
        )
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        particles.forEach { particle ->
            val currentY = particle.startY + progress.value * particle.speed * 1.5f
            if (currentY <= 1f) {
                val alpha = 1f - (progress.value * 0.8f)
                drawCircle(
                    color = particle.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                    radius = particle.size,
                    center = Offset(
                        x = particle.x * canvasWidth,
                        y = currentY * canvasHeight
                    )
                )
            }
        }
    }
}
