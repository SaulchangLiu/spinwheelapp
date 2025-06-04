package com.example.spinwheelapp

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import java.net.URLEncoder
import java.util.*

// Data class to hold wheel segment information
data class WheelSegment(
    val text: String,
    val emoji: String,
    val color: Color,
    val isSelected: Boolean = false
)

// Available food options
data class FoodOption(
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

        // Handle shared wheel data
        val sharedWheelData = handleIncomingIntent(intent)

        setContent {
            MaterialTheme {
                SpinWheelApp(initialSharedData = sharedWheelData)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val sharedWheelData = handleIncomingIntent(intent)
        // Update the app with new shared data
        // This would require state management in a real app
    }

    private fun handleIncomingIntent(intent: Intent): List<FoodOption>? {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            uri?.let {
                val wheelData = it.getQueryParameter("wheel")
                wheelData?.let { data ->
                    return parseSharedWheelData(data)
                }
            }
        }
        return null
    }

    private fun parseSharedWheelData(data: String): List<FoodOption> {
        // Parse the shared wheel data (implement based on your sharing format)
        // For now, returning a sample implementation
        return try {
            val items = data.split(",")
            items.mapNotNull { item ->
                val parts = item.split("|")
                if (parts.size >= 3) {
                    FoodOption(
                        text = parts[0],
                        emoji = parts[1],
                        color = Color(parts[2].toLong(16) or 0xFF000000)
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// Location utility functions
suspend fun getCurrentLocation(context: Context): String {
    return withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasLocationPermission) {
                return@withContext "Unknown Location"
            }

            // Check if location services are enabled
            val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isLocationEnabled) {
                return@withContext "Location Disabled"
            }

            // Try to get last known location
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)

            for (provider in providers) {
                try {
                    val location = locationManager.getLastKnownLocation(provider)
                    location?.let {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)

                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            return@withContext address.locality ?: address.subAdminArea ?: address.adminArea ?: "Unknown City"
                        }
                    }
                } catch (e: Exception) {
                    // Continue to next provider
                    continue
                }
            }

            "Location Not Available"
        } catch (e: Exception) {
            "Hong Kong" // Fallback to original
        }
    }
}

@Composable
fun SpinWheelApp(initialSharedData: List<FoodOption>? = null) {
    var currentScreen by remember { mutableStateOf("selection") } // "selection", "wheel", or "result"
    var selectedFoodOptions by remember { mutableStateOf<List<FoodOption>>(initialSharedData ?: emptyList()) }
    var spinResult by remember { mutableStateOf<FoodOption?>(null) }

    // If we have shared data, go directly to wheel screen
    LaunchedEffect(initialSharedData) {
        if (initialSharedData != null && initialSharedData.isNotEmpty()) {
            selectedFoodOptions = initialSharedData
            currentScreen = "wheel"
        }
    }

    when (currentScreen) {
        "selection" -> {
            FoodSelectionScreen(
                onConfirm = { selectedOptions ->
                    selectedFoodOptions = selectedOptions
                    currentScreen = "wheel"
                }
            )
        }
        "wheel" -> {
            SpinWheelScreen(
                selectedFoodOptions = selectedFoodOptions,
                onBack = { currentScreen = "selection" },
                onSpinComplete = { result ->
                    spinResult = result
                    currentScreen = "result"
                }
            )
        }
        "result" -> {
            spinResult?.let { result ->
                SpinResultScreen(
                    result = result,
                    onBack = { currentScreen = "wheel" },
                    onNewSpin = { currentScreen = "selection" }
                )
            }
        }
    }
}

@Composable
fun FoodSelectionScreen(onConfirm: (List<FoodOption>) -> Unit) {
    val context = LocalContext.current
    var currentCity by remember { mutableStateOf("Detecting...") }
    val scope = rememberCoroutineScope()

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                currentCity = getCurrentLocation(context)
            }
        } else {
            currentCity = "Permission Denied"
        }
    }

    // Check and request location on first composition
    LaunchedEffect(Unit) {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            currentCity = getCurrentLocation(context)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val availableFoodOptions = remember {
        listOf(
            FoodOption("Hotpot", "🍲", Color(0xFFB8D4B8)),
            FoodOption("Ramen", "🍜", Color(0xFFF4D49A)),
            FoodOption("Sushi", "🍣", Color(0xFFB8D4B8)),
            FoodOption("SiChuan Style", "🌶️", Color(0xFFE8B4B8)),
            FoodOption("Bento", "🍱", Color(0xFFF4D49A)),
            FoodOption("BBQ", "🍖", Color(0xFFD4B8D4)),
            FoodOption("Dumplings", "🥟", Color(0xFFB8D4B8)),
            FoodOption("HK Style", "🥠", Color(0xFFE8B4B8)),
            FoodOption("Pizza", "🍕", Color(0xFFFFB6C1)),
            FoodOption("Burger", "🍔", Color(0xFFDDA0DD)),
            FoodOption("Tacos", "🌮", Color(0xFFFFE4B5)),
            //FoodOption("Pasta", "🍝", Color(0xFFB0E0E6)),
            FoodOption("Korean BBQ", "🥩", Color(0xFFFFA07A)),
            FoodOption("Thai Food", "🍛", Color(0xFF98FB98)),
            FoodOption("Indian Curry", "🍛", Color(0xFFFFDEAD)),
            FoodOption("Mediterranean", "🥙", Color(0xFFE6E6FA)),
            FoodOption("Fried Chicken", "🍗", Color(0xFFFFD700)),
            FoodOption("Halal Food", "🍽️", Color(0xFF90EE90)),
            FoodOption("Pho", "🍜", Color(0xFFFFB6C1)),
            //FoodOption("Sandwiches", "🥪", Color(0xFFFAD5A5)),
            FoodOption("Steak", "🥩", Color(0xFFCD853F)),
            FoodOption("Seafood", "🦐", Color(0xFF87CEEB)),
            FoodOption("Vietnamese", "🇻🇳", Color(0xFFDDA0DD)),
            FoodOption("Chinese", "🥢", Color(0xFFFF6347)),
            FoodOption("Mexican", "🌶️", Color(0xFFFFE4E1)),
            FoodOption("Greek", "🫒", Color(0xFFF0E68C)),
            FoodOption("Turkish", "🥙", Color(0xFFDEB887)),
            FoodOption("Lebanese", "🧆", Color(0xFFE6E6FA)),
            FoodOption("Fish", "🐟", Color(0xFFB0C4DE)),
            //FoodOption("Salad", "🥗", Color(0xFF98FB98)),
            //FoodOption("Wraps", "🌯", Color(0xFFFFDEAD)),
            //FoodOption("Wings", "🍗", Color(0xFFFF8C00)),
            FoodOption("Noodles", "🍝", Color(0xFFFFE4B5)),
            //FoodOption("Rice Bowls", "🍚", Color(0xFFDDA0DD)),
            //FoodOption("Desserts", "🍰", Color(0xFFFFB6C1)),
            FoodOption("Breakfast", "🥞", Color(0xFFFFE4E1))
        )
    }

    var selectedOptions by remember { mutableStateOf<Set<FoodOption>>(emptySet()) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        // Background canvas for gradient and leaves
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw gradient background
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFB07A),
                    Color(0xFFFFA07A),
                    Color(0xFF9BB8CD)
                )
            )
            drawRect(gradient)
            drawFallingLeaves(fallingLeaves)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Location display with dynamic city
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5DC).copy(alpha = 0.9f)
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
                }
            }

            // Title
            Text(
                text = "Choose Your Food Options",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B4513),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Select at least 2 options for the wheel",
                fontSize = 16.sp,
                color = Color(0xFF8B4513).copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Food options grid - 3 items per row with more spacing
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing
            ) {
                items(availableFoodOptions.chunked(3)) { rowItems -> // 3 items per row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // Increased spacing
                    ) {
                        rowItems.forEach { option ->
                            FoodOptionCard(
                                option = option,
                                isSelected = selectedOptions.contains(option),
                                onToggle = {
                                    selectedOptions = if (selectedOptions.contains(option)) {
                                        selectedOptions - option
                                    } else {
                                        selectedOptions + option
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if not enough items to fill the row
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Selected count
            Text(
                text = "Selected: ${selectedOptions.size} options",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8B4513),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Confirm button
            Button(
                onClick = { onConfirm(selectedOptions.toList()) },
                enabled = selectedOptions.size >= 2,
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .height(56.dp)
                    .widthIn(min = 160.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD2B48C),
                    disabledContainerColor = Color(0xFFD2B48C).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = if (selectedOptions.size >= 2) "Confirm Selection" else "Select at least 2",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedOptions.size >= 2) Color(0xFF8B4513) else Color(0xFF8B4513).copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun FoodOptionCard(
    option: FoodOption,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp) // Increased height for better visibility
            .clickable { onToggle() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = Color(0xFF8B4513),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                option.color.copy(alpha = 0.9f) // Back to using option's original color
            } else {
                option.color.copy(alpha = 0.6f) // Back to using option's original color
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // Remove shadow by setting elevation to 0
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (isSelected) {
                        option.color.copy(alpha = 0.9f)
                    } else {
                        option.color.copy(alpha = 0.6f)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp), // Increased padding
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = option.emoji,
                    fontSize = 28.sp, // Increased emoji size
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = option.text,
                    fontSize = 14.sp, // Increased text size
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8B4513),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun SpinWheelScreen(
    selectedFoodOptions: List<FoodOption>,
    onBack: () -> Unit,
    onSpinComplete: (FoodOption) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val animatedAngle = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var currentCity by remember { mutableStateOf("Detecting...") }

    // Get current location for wheel screen too
    LaunchedEffect(Unit) {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            currentCity = getCurrentLocation(context)
        } else {
            currentCity = "Location Permission Required"
        }
    }

    // Convert selected options to wheel segments
    val wheelSegments = remember(selectedFoodOptions) {
        selectedFoodOptions.map { option ->
            WheelSegment(
                text = option.text,
                emoji = option.emoji,
                color = option.color
            )
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Background canvas for gradient and leaves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFB07A),
                    Color(0xFFFFA07A),
                    Color(0xFF9BB8CD)
                )
            )
            drawRect(gradient)
            drawFallingLeaves(fallingLeaves)
        }

        // Share button in top right corner
        IconButton(
            onClick = {
                shareWheel(context, selectedFoodOptions)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .shadow(4.dp, RoundedCornerShape(50))
                .background(
                    Color(0xFFD2B48C).copy(alpha = 0.9f),
                    RoundedCornerShape(50)
                )
                .size(48.dp)
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Share Wheel",
                tint = Color(0xFF8B4513),
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Location display with dynamic city
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5DC).copy(alpha = 0.9f)
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
                    drawDetailedChopstickPointer()
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Button row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8B4B8)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        "← Back",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    )
                }

                // Spin button
                Button(
                    onClick = {
                        if (!isSpinning) {
                            val minRotation = 1080f
                            val extraRotation = Random.nextInt(1080, 3600).toFloat()
                            val targetRotation = minRotation + extraRotation
                            val segmentAngle = 360f / selectedFoodOptions.size

                            isSpinning = true
                            scope.launch {
                                animatedAngle.animateTo(
                                    animatedAngle.value + targetRotation,
                                    animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing)
                                )

                                // Calculate which segment the pointer landed on
                                val finalAngle = (animatedAngle.value % 360 + 360) % 360
                                val adjustedAngle = (360 - finalAngle + 90) % 360 // Adjust for pointer position
                                val selectedIndex = ((adjustedAngle / segmentAngle).toInt()) % selectedFoodOptions.size
                                val selectedOption = selectedFoodOptions[selectedIndex]

                                isSpinning = false
                                onSpinComplete(selectedOption)
                            }
                        } else {
                            // If spinning, stop the animation quickly
                            scope.launch {
                                val currentAngle = animatedAngle.value
                                val segmentAngle = 360f / selectedFoodOptions.size
                                val quickStopRotation = currentAngle + Random.nextInt(180, 540).toFloat() // Quick additional rotation

                                animatedAngle.animateTo(
                                    quickStopRotation,
                                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing) // Quick stop
                                )

                                // Calculate which segment the pointer landed on
                                val finalAngle = (animatedAngle.value % 360 + 360) % 360
                                val adjustedAngle = (360 - finalAngle + 90) % 360 // Adjust for pointer position
                                val selectedIndex = ((adjustedAngle / segmentAngle).toInt()) % selectedFoodOptions.size
                                val selectedOption = selectedFoodOptions[selectedIndex]

                                isSpinning = false
                                onSpinComplete(selectedOption)
                            }
                        }
                    },
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .height(56.dp)
                        .widthIn(min = 160.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD2B48C)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        if (isSpinning) "Quick Stop!" else "Food Magic!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    )
                }
            }
        }
    }
}

@Composable
fun SpinResultScreen(
    result: FoodOption,
    onBack: () -> Unit,
    onNewSpin: () -> Unit
) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Background canvas for gradient and leaves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFB07A),
                    Color(0xFFFFA07A),
                    Color(0xFF9BB8CD)
                )
            )
            drawRect(gradient)
            drawFallingLeaves(fallingLeaves)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Result display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = result.color.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 You Got 🎉",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = result.emoji,
                        fontSize = 80.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = result.text,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Button row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back to wheel button
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8B4B8)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        "← Back",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    )
                }

                // New spin button
                Button(
                    onClick = onNewSpin,
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD2B48C)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        "New Spin",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    )
                }
            }
        }
    }
}

// Helper function to share wheel data
fun shareWheel(context: Context, selectedOptions: List<FoodOption>) {
    val wheelData = selectedOptions.joinToString(",") { option ->
        "${option.text}|${option.emoji}|${option.color.value.toString(16)}"
    }

    val appUrl = "https://yourapp.com/wheel?wheel=${URLEncoder.encode(wheelData, "UTF-8")}"
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.example.spinwheelapp"

    val shareText = """
        🎲 Check out my food wheel! Spin to decide what to eat:
        
        $appUrl
        
        Don't have the app? Download it here: $playStoreUrl
        
        #FoodWheel #WhatToEat
    """.trimIndent()

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "My Food Wheel - What should we eat?")
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Food Wheel"))
}

fun DrawScope.drawFallingLeaves(leaves: List<FallingLeaf>) {
    leaves.forEach { leaf ->
        val x = leaf.x * size.width
        val y = leaf.y * size.height

        rotate(leaf.rotation, pivot = Offset(x, y)) {
            val leafColor = Color(0xFFD2691E).copy(alpha = leaf.alpha)
            val leafSize = 20f * leaf.size

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
    val chopstickLength = wheelRadius * 0.85f
    val chopstickWidth = 8f
    val chopstickTipWidth = 3f

    val chopstickPath = Path().apply {
        moveTo(centerX - chopstickWidth/2, centerY)
        lineTo(centerX + chopstickWidth/2, centerY)
        lineTo(centerX + chopstickTipWidth/2, centerY - chopstickLength)
        lineTo(centerX - chopstickTipWidth/2, centerY - chopstickLength)
        close()
    }

    val shadowPath = Path().apply {
        moveTo(centerX - chopstickWidth/2 + 2f, centerY + 2f)
        lineTo(centerX + chopstickWidth/2 + 2f, centerY + 2f)
        lineTo(centerX + chopstickTipWidth/2 + 2f, centerY - chopstickLength + 2f)
        lineTo(centerX - chopstickTipWidth/2 + 2f, centerY - chopstickLength + 2f)
        close()
    }

    drawPath(
        path = shadowPath,
        color = Color(0x40000000)
    )

    val chopstickGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE6D3A3),
            Color(0xFFD2B48C),
            Color(0xFFB8860B),
            Color(0xFF8B4513)
        ),
        start = Offset(centerX - chopstickWidth, centerY),
        end = Offset(centerX + chopstickWidth, centerY)
    )

    drawPath(
        path = chopstickPath,
        brush = chopstickGradient
    )

    drawPath(
        path = chopstickPath,
        color = Color(0xFF654321),
        style = Stroke(width = 1.5.dp.toPx())
    )

    val grainPaint = android.graphics.Paint().apply {
        color = Color(0xFF8B4513).copy(alpha = 0.6f).toArgb()
        strokeWidth = 1f
        isAntiAlias = true
    }

    for (i in 0..6) {
        val progress = i / 6f
        val yPos = centerY - (chopstickLength * progress)
        val width = chopstickWidth * (1f - progress * 0.6f)

        drawContext.canvas.nativeCanvas.drawLine(
            centerX - width * 0.3f, yPos,
            centerX + width * 0.3f, yPos,
            grainPaint
        )
    }

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
        color = Color(0xFF654321)
    )

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
        color = Color(0xFFF5F5DC).copy(alpha = 0.7f)
    )
}

fun DrawScope.drawSpinWheel(segments: List<WheelSegment>, rotationAngle: Float) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = size.minDimension / 2f - 10f
    val sweepAngle = 360f / segments.size

    segments.forEachIndexed { index, segment ->
        val startAngle = index * sweepAngle + (rotationAngle % 360)

        drawArc(
            color = segment.color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true
        )

        drawArc(
            color = Color(0xFF8B4513),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            style = Stroke(width = 3.dp.toPx())
        )
    }

    drawCircle(
        color = Color(0xFF8B4513),
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
        style = Stroke(width = 6.dp.toPx())
    )

    segments.forEachIndexed { index, segment ->
        val angleInRadians = Math.toRadians(
            (index * sweepAngle + sweepAngle / 2 + (rotationAngle % 360)).toDouble()
        )

        // Doubled the emoji size from 80f to 160f
        val emojiRadius = radius * 0.45f
        val emojiX = centerX + (emojiRadius * cos(angleInRadians)).toFloat()
        val emojiY = centerY + (emojiRadius * sin(angleInRadians)).toFloat()

        val textRadius = radius * 0.75f
        val textX = centerX + (textRadius * cos(angleInRadians)).toFloat()
        val textY = centerY + (textRadius * sin(angleInRadians)).toFloat()

        val emojiPaint = android.graphics.Paint().apply {
            textSize = 160f  // Doubled from 80f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        drawContext.canvas.nativeCanvas.drawText(
            segment.emoji,
            emojiX,
            emojiY + emojiPaint.textSize / 3,
            emojiPaint
        )

        val textPaint = android.graphics.Paint().apply {
            color = Color(0xFF8B4513).toArgb()
            textSize = 64f  // Doubled from 32f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(3f, 1f, 1f, Color(0x60000000).toArgb())
        }

        drawContext.canvas.nativeCanvas.drawText(
            segment.text,
            textX,
            textY + textPaint.textSize / 3,
            textPaint
        )
    }

    drawCircle(
        color = Color(0xFF8B4513),
        radius = 35f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )

    drawCircle(
        color = Color(0xFFD2B48C),
        radius = 28f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )

    drawCircle(
        color = Color(0xFF8B4513),
        radius = 22f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )

    val chopstickPaint = android.graphics.Paint().apply {
        color = Color(0xFFD2B48C).toArgb()
        strokeWidth = 2f
        isAntiAlias = true
    }

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

    drawCircle(
        color = Color(0xFF654321),
        radius = 8f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )
}