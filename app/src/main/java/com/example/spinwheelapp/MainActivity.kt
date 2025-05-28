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

    // Get current location and meal time
    val currentCity = remember { "Hong Kong" } // You can implement location detection here
    val currentMealTime = remember {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (currentHour) {
            in 5..10 -> "Breakfast"
            in 11..14 -> "Lunch"
            in 17..21 -> "Dinner"
            else -> "Late Night Snack"
        }
    }

    // Food-themed wheel segments matching the design
    val wheelSegments = remember {
        listOf(
            // WheelSegment("Cake", "🍰", Color(0xFFB8D4B8)), // Light green - Original
            WheelSegment("Hotpot", "🍲", Color(0xFFB8D4B8)), // Light green - Changed from Cake
            WheelSegment("Ramen", "🍜", Color(0xFFF4D49A)), // Light yellow
            WheelSegment("Sushi", "🍣", Color(0xFFB8D4B8)), // Light green
            // WheelSegment("Dango", "🍡", Color(0xFFE8B4B8)), // Light pink - Original
            WheelSegment("SiChuan Style", "🌶️", Color(0xFFE8B4B8)), // Light pink - Changed from Dango
            WheelSegment("Bento", "🍱", Color(0xFFF4D49A)), // Light yellow
            // WheelSegment("Donut", "🍩", Color(0xFFD4B8D4)), // Light purple - Original
            WheelSegment("BBQ", "🍖", Color(0xFFD4B8D4)), // Light purple - Changed from Donut
            WheelSegment("Dumplings", "🥟", Color(0xFFB8D4B8)), // Light green
            // WheelSegment("Pancakes", "🥞", Color(0xFFE8B4B8)) // Light pink - Original
            WheelSegment("HK Style", "🥠", Color(0xFFE8B4B8)) // Light pink - Changed from Pancakes
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
            // Draw gradient background (warm peach to warm blue)
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFB07A), // Warm peach
                    Color(0xFFFFA07A), // Light salmon
                    Color(0xFF9BB8CD)  // Warmer, muted blue (changed from bright sky blue)
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
            // Location and meal time display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5DC).copy(alpha = 0.9f) // Beige with transparency
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📍 $currentCity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8B4513)
                    )
                    Text(
                        text = "🕐 Time for $currentMealTime",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF8B4513).copy(alpha = 0.8f)
                    )
                }
            }
            // Main wheel with shadow and chopstick pointer
            Box(
                modifier = Modifier.size(320.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawSpinWheel(
                        segments = wheelSegments,
                        rotationAngle = animatedAngle.value
                    )

                    // Draw detailed chopstick pointer extending from center
                    drawDetailedChopstickPointer()
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Styled button matching the design
            Button(
                onClick = {
                    if (!isSpinning) {
                        // Ensure at least 3 full rotations (1080 degrees) plus random extra spins
                        val minRotation = 1080f // 3 full rotations minimum
                        val extraRotation = Random.nextInt(1080, 3600).toFloat() // 3-10 additional rotations
                        val targetRotation = minRotation + extraRotation
                        isSpinning = true
                        scope.launch {
                            animatedAngle.animateTo(
                                animatedAngle.value + targetRotation,
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

fun DrawScope.drawDetailedChopstickPointer() {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val wheelRadius = size.minDimension / 2f - 10f

    // Calculate chopstick length to extend from center to edge
    val chopstickLength = wheelRadius * 0.85f
    val chopstickWidth = 8f
    val chopstickTipWidth = 3f

    // Main chopstick body (tapered from center to tip)
    val chopstickPath = Path().apply {
        // Start from center (bottom of chopstick)
        moveTo(centerX - chopstickWidth/2, centerY)
        lineTo(centerX + chopstickWidth/2, centerY)

        // Taper to tip (pointing upward)
        lineTo(centerX + chopstickTipWidth/2, centerY - chopstickLength)
        lineTo(centerX - chopstickTipWidth/2, centerY - chopstickLength)
        close()
    }

    // Draw chopstick shadow first
    val shadowPath = Path().apply {
        moveTo(centerX - chopstickWidth/2 + 2f, centerY + 2f)
        lineTo(centerX + chopstickWidth/2 + 2f, centerY + 2f)
        lineTo(centerX + chopstickTipWidth/2 + 2f, centerY - chopstickLength + 2f)
        lineTo(centerX - chopstickTipWidth/2 + 2f, centerY - chopstickLength + 2f)
        close()
    }

    drawPath(
        path = shadowPath,
        color = Color(0x40000000) // Semi-transparent black shadow
    )

    // Draw chopstick with realistic wood gradient
    val chopstickGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE6D3A3), // Light bamboo color
            Color(0xFFD2B48C), // Medium wood
            Color(0xFFB8860B), // Dark gold
            Color(0xFF8B4513)  // Dark wood
        ),
        start = Offset(centerX - chopstickWidth, centerY),
        end = Offset(centerX + chopstickWidth, centerY)
    )

    drawPath(
        path = chopstickPath,
        brush = chopstickGradient
    )

    // Draw chopstick outline
    drawPath(
        path = chopstickPath,
        color = Color(0xFF654321),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Draw detailed wood grain lines
    val grainPaint = android.graphics.Paint().apply {
        color = Color(0xFF8B4513).copy(alpha = 0.6f).toArgb()
        strokeWidth = 1f
        isAntiAlias = true
    }

    // Vertical grain lines along the chopstick
    for (i in 0..6) {
        val progress = i / 6f
        val yPos = centerY - (chopstickLength * progress)
        val width = chopstickWidth * (1f - progress * 0.6f) // Taper the grain lines too

        drawContext.canvas.nativeCanvas.drawLine(
            centerX - width * 0.3f, yPos,
            centerX + width * 0.3f, yPos,
            grainPaint
        )
    }

    // Add horizontal wood rings (natural bamboo segments)
    val ringPaint = android.graphics.Paint().apply {
        color = Color(0xFF654321).copy(alpha = 0.8f).toArgb()
        strokeWidth = 2f
        isAntiAlias = true
    }

    for (i in 1..4) {
        val progress = i / 5f
        val yPos = centerY - (chopstickLength * progress)
        val width = chopstickWidth * (1f - progress * 0.6f)

        drawContext.canvas.nativeCanvas.drawLine(
            centerX - width/2, yPos,
            centerX + width/2, yPos,
            ringPaint
        )
    }

    // Add chopstick tip detail (darker tip)
    val tipPath = Path().apply {
        val tipHeight = 15f
        moveTo(centerX - chopstickTipWidth/2, centerY - chopstickLength + tipHeight)
        lineTo(centerX + chopstickTipWidth/2, centerY - chopstickLength + tipHeight)
        lineTo(centerX + chopstickTipWidth/2, centerY - chopstickLength)
        lineTo(centerX - chopstickTipWidth/2, centerY - chopstickLength)
        close()
    }

    drawPath(
        path = tipPath,
        color = Color(0xFF654321) // Darker tip
    )

    // Add a small highlight on the chopstick for 3D effect
    val highlightPath = Path().apply {
        val highlightWidth = 2f
        moveTo(centerX - highlightWidth/2, centerY)
        lineTo(centerX - highlightWidth/4, centerY - chopstickLength * 0.8f)
        lineTo(centerX + highlightWidth/4, centerY - chopstickLength * 0.8f)
        lineTo(centerX + highlightWidth/2, centerY)
        close()
    }

    drawPath(
        path = highlightPath,
        color = Color(0xFFF5F5DC).copy(alpha = 0.7f) // Light highlight
    )
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