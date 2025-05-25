package com.example.spinwheelapp

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Data class to hold wheel segment information
data class WheelSegment(
    val text: String,
    val emoji: String,
    val color: Color
)

// Falling leaf data
data class FallingLeaf(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val size: Float,
    val alpha: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SpinWheelScreen()
            }
        }
    }
}

@Composable
fun SpinWheelScreen() {
    val scope = rememberCoroutineScope()
    val animatedAngle = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }

    // Food-themed wheel segments matching the design
    val wheelSegments = remember {
        listOf(
            WheelSegment("Cake", "🍰", Color(0xFFB8D4B8)), // Light green
            WheelSegment("Ramen", "🍜", Color(0xFFF4D49A)), // Light yellow
            WheelSegment("Sushi", "🍣", Color(0xFFB8D4B8)), // Light green
            WheelSegment("Dango", "🍡", Color(0xFFE8B4B8)), // Light pink
            WheelSegment("Bento", "🍱", Color(0xFFF4D49A)), // Light yellow
            WheelSegment("Donut", "🍩", Color(0xFFD4B8D4)), // Light purple
            WheelSegment("Dumplings", "🥟", Color(0xFFB8D4B8)), // Light green
            WheelSegment("Pancakes", "🥞", Color(0xFFE8B4B8)) // Light pink
        )
    }

    // Generate random falling leaves
    val fallingLeaves = remember {
        List(15) {
            FallingLeaf(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                rotation = Random.nextFloat() * 360f,
                size = Random.nextFloat() * 0.5f + 0.5f,
                alpha = Random.nextFloat() * 0.3f + 0.1f
            )
        }
    }

    // Gradient background with falling leaves
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background canvas for gradient and leaves
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw gradient background (warm peach to sky blue)
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFB07A), // Warm peach
                    Color(0xFFFFA07A), // Light salmon
                    Color(0xFF87CEEB)  // Sky blue
                )
            )
            drawRect(gradient)

            // Draw falling leaves
            drawFallingLeaves(fallingLeaves)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main wheel with shadow
            Box(
                modifier = Modifier.size(320.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawSpinWheel(
                        segments = wheelSegments,
                        rotationAngle = animatedAngle.value
                    )
                }

                // Chopstick pointer positioned at top
                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(y = (-140).dp)
                ) {
                    drawChopstickPointer()
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Styled button matching the design
            Button(
                onClick = {
                    if (!isSpinning) {
                        val targetRotation = Random.nextInt(3600, 7200).toFloat()
                        isSpinning = true
                        scope.launch {
                            animatedAngle.animateTo(
                                targetRotation,
                                animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing)
                            )
                            isSpinning = false
                        }
                    }
                },
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .height(56.dp)
                    .widthIn(min = 160.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD2B48C) // Sandy brown matching design
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    "Let's Eat!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513) // Dark brown text
                )
            }
        }
    }
}

fun DrawScope.drawFallingLeaves(leaves: List<FallingLeaf>) {
    leaves.forEach { leaf ->
        val x = leaf.x * size.width
        val y = leaf.y * size.height

        rotate(leaf.rotation, pivot = Offset(x, y)) {
            // Draw leaf shape (simplified maple leaf)
            val leafColor = Color(0xFFD2691E).copy(alpha = leaf.alpha) // Orange with transparency
            val leafSize = 20f * leaf.size

            // Simple leaf shape using arcs
            drawCircle(
                color = leafColor,
                radius = leafSize,
                center = Offset(x, y)
            )
            drawCircle(
                color = leafColor,
                radius = leafSize * 0.7f,
                center = Offset(x - leafSize * 0.3f, y - leafSize * 0.3f)
            )
            drawCircle(
                color = leafColor,
                radius = leafSize * 0.7f,
                center = Offset(x + leafSize * 0.3f, y - leafSize * 0.3f)
            )
        }
    }
}

fun DrawScope.drawChopstickPointer() {
    val centerX = size.width / 2f
    val centerY = size.height / 2f

    // Main chopstick body
    val chopstickPath = Path().apply {
        moveTo(centerX - 3f, centerY + 30f) // Bottom left
        lineTo(centerX + 3f, centerY + 30f) // Bottom right
        lineTo(centerX + 1.5f, centerY - 30f) // Top right (tapered)
        lineTo(centerX - 1.5f, centerY - 30f) // Top left (tapered)
        close()
    }

    // Draw chopstick with gradient
    val chopstickGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFD2B48C), // Light wood
            Color(0xFF8B4513)  // Dark wood
        ),
        start = Offset(centerX - 5f, centerY),
        end = Offset(centerX + 5f, centerY)
    )

    drawPath(
        path = chopstickPath,
        brush = chopstickGradient
    )

    // Draw chopstick outline
    drawPath(
        path = chopstickPath,
        color = Color(0xFF654321),
        style = Stroke(width = 1.dp.toPx())
    )

    // Wood grain lines
    val grainPaint = android.graphics.Paint().apply {
        color = Color(0xFF8B4513).toArgb()
        strokeWidth = 0.5f
        isAntiAlias = true
    }

    for (i in 0..3) {
        val y = centerY - 20f + (i * 12f)
        drawContext.canvas.nativeCanvas.drawLine(
            centerX - 2f, y,
            centerX + 2f, y,
            grainPaint
        )
    }
}

fun DrawScope.drawSpinWheel(segments: List<WheelSegment>, rotationAngle: Float) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = size.minDimension / 2f - 10f // Leave some margin
    val sweepAngle = 360f / segments.size

    // Draw wheel segments
    segments.forEachIndexed { index, segment ->
        val startAngle = index * sweepAngle + (rotationAngle % 360)

        // Draw the colored arc
        drawArc(
            color = segment.color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true
        )

        // Draw segment borders
        drawArc(
            color = Color(0xFF8B4513), // Dark brown border
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            style = Stroke(width = 3.dp.toPx())
        )
    }

    // Draw outer border circle
    drawCircle(
        color = Color(0xFF8B4513), // Dark brown
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
        style = Stroke(width = 6.dp.toPx())
    )

    // Draw emojis and text (larger sizes)
    segments.forEachIndexed { index, segment ->
        val angleInRadians = Math.toRadians(
            (index * sweepAngle + sweepAngle / 2 + (rotationAngle % 360)).toDouble()
        )

        // Calculate emoji position (closer to center)
        val emojiRadius = radius * 0.45f
        val emojiX = centerX + (emojiRadius * cos(angleInRadians)).toFloat()
        val emojiY = centerY + (emojiRadius * sin(angleInRadians)).toFloat()

        // Calculate text position (further from center)
        val textRadius = radius * 0.75f
        val textX = centerX + (textRadius * cos(angleInRadians)).toFloat()
        val textY = centerY + (textRadius * sin(angleInRadians)).toFloat()

        // Draw emoji (even larger)
        val emojiPaint = android.graphics.Paint().apply {
            textSize = 80f // Increased from 60f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        drawContext.canvas.nativeCanvas.drawText(
            segment.emoji,
            emojiX,
            emojiY + emojiPaint.textSize / 3,
            emojiPaint
        )

        // Draw text (larger)
        val textPaint = android.graphics.Paint().apply {
            color = Color(0xFF8B4513).toArgb() // Dark brown text
            textSize = 32f // Increased from 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            // Add subtle shadow
            setShadowLayer(3f, 1f, 1f, Color(0x60000000).toArgb())
        }

        drawContext.canvas.nativeCanvas.drawText(
            segment.text,
            textX,
            textY + textPaint.textSize / 3,
            textPaint
        )
    }

    // Draw wooden chopstick bundle center
    // Outer wooden ring
    drawCircle(
        color = Color(0xFF8B4513), // Dark brown wood
        radius = 35f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )

    // Inner wooden base
    drawCircle(
        color = Color(0xFFD2B48C), // Light wood
        radius = 28f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )

    // Chopstick bundle center
    drawCircle(
        color = Color(0xFF8B4513), // Dark wood
        radius = 22f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )

    // Individual chopstick lines (wooden texture effect)
    val chopstickPaint = android.graphics.Paint().apply {
        color = Color(0xFFD2B48C).toArgb()
        strokeWidth = 2f
        isAntiAlias = true
    }

    // Draw chopstick lines in a bundle pattern
    for (i in 0..7) {
        val angle = (i * 45f) * Math.PI / 180
        val startX = centerX + (12f * cos(angle)).toFloat()
        val startY = centerY + (12f * sin(angle)).toFloat()
        val endX = centerX + (20f * cos(angle)).toFloat()
        val endY = centerY + (20f * sin(angle)).toFloat()

        drawContext.canvas.nativeCanvas.drawLine(
            startX, startY, endX, endY, chopstickPaint
        )
    }

    // Center binding (where chopsticks are tied together)
    drawCircle(
        color = Color(0xFF654321), // Darker brown for binding
        radius = 8f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )
}