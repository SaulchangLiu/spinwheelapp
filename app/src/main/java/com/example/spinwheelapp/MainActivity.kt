package com.example.spinwheelapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawArc
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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
    var angle by remember { mutableStateOf(0f) }
    var isSpinning by remember { mutableStateOf(false) }

    val colors = listOf(
        Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta,
        Color.Cyan, Color.Gray, Color.LightGray, Color.DarkGray, Color.Black
    )

    val spinAnim = rememberInfiniteTransition(label = "spin")
    val animatedAngle by spinAnim.animateFloat(
        initialValue = angle,
        targetValue = angle + 3600,
        animationSpec = if (isSpinning)
            tween(durationMillis = 4000, easing = FastOutSlowInEasing)
        else
            tween(durationMillis = 0),
        label = "spin"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val sweep = 360f / 10
            for (i in 0 until 10) {
                drawArc(
                    color = colors[i % colors.size],
                    startAngle = i * sweep + animatedAngle % 360,
                    sweepAngle = sweep,
                    useCenter = true
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!isSpinning) {
                    angle = Random.nextInt(3600, 7200).toFloat()
                    isSpinning = true
                }
            },
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Spin!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }

    LaunchedEffect(animatedAngle) {
        if (isSpinning && animatedAngle >= angle) {
            isSpinning = false
        }
    }
}
