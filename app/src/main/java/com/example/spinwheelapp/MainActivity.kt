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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val animatedAngle = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }

    val colors = listOf(
        Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta,
        Color.Cyan, Color.Gray, Color.LightGray, Color.DarkGray, Color.Black
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val sweep = 360f / 10
            for (i in 0 until 10) {
                drawArc(
                    color = colors[i % colors.size],
                    startAngle = i * sweep + (animatedAngle.value % 360),
                    sweepAngle = sweep,
                    useCenter = true
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

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
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Spin!", fontSize = 20.sp)
        }
    }
}