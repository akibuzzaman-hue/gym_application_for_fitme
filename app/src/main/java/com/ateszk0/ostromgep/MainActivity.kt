package com.ateszk0.ostromgep

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ateszk0.ostromgep.ui.screens.*
import com.ateszk0.ostromgep.ui.theme.*
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {

            val workoutViewModel: WorkoutViewModel = viewModel()
            val currentThemeName by workoutViewModel.appTheme.collectAsState()
            
            val themeColor = when(currentThemeName) { 
                "Piros" -> Color(0xFFFF453A)
                "Sárga" -> Color(0xFFFFD60A)
                "Zöld" -> Color(0xFF32D74B)
                else -> Color(0xFF0A84FF) 
            }

            val customColorScheme = darkColorScheme(
                primary = themeColor, 
                onPrimary = Color.White, 
                secondary = themeColor, 
                background = DarkBackground, 
                surface = SurfaceDark, 
                onSurface = Color.White, 
                primaryContainer = SurfaceDark, 
                onPrimaryContainer = Color.White
            )
            
            MaterialTheme(colorScheme = customColorScheme) { 
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) { 
                    OstromgepApp(workoutViewModel, themeColor) 
                } 
            }
        }
    }
}

enum class AppScreen { Home, Workout, Profile, ExercisesList, Calendar, Statistics }

@Composable
fun OstromgepApp(viewModel: WorkoutViewModel, themeColor: Color) {
    var currentScreen by remember { mutableStateOf(AppScreen.Profile) }
    var isWorkoutActive by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(isWorkoutActive) {
        val intent = Intent(context, WorkoutService::class.java)
        if (isWorkoutActive) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }

    if (isWorkoutActive) {
        ActiveWorkoutScreen(viewModel = viewModel, themeColor = themeColor, onFinishWorkout = { isWorkoutActive = false })
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) }, 
                        label = { Text("Home") }, 
                        selected = currentScreen == AppScreen.Home, 
                        onClick = { currentScreen = AppScreen.Home }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, indicatorColor = Color.Transparent, selectedTextColor = themeColor)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.FitnessCenter, null) }, 
                        label = { Text("Workout") }, 
                        selected = currentScreen == AppScreen.Workout, 
                        onClick = { currentScreen = AppScreen.Workout }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, indicatorColor = Color.Transparent, selectedTextColor = themeColor)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, null) }, 
                        label = { Text("Profile") }, 
                        selected = currentScreen == AppScreen.Profile, 
                        onClick = { currentScreen = AppScreen.Profile }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, indicatorColor = Color.Transparent, selectedTextColor = themeColor)
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (currentScreen) {
                    AppScreen.Home -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Hamarosan...", color = TextGray) }
                    AppScreen.Workout -> WorkoutTab(viewModel, themeColor, onStart = { isWorkoutActive = true })
                    AppScreen.Profile -> DashboardProfile(
                        viewModel, 
                        themeColor, 
                        onNavigateToExercises = { currentScreen = AppScreen.ExercisesList },
                        onNavigateToCalendar = { currentScreen = AppScreen.Calendar },
                        onNavigateToStatistics = { currentScreen = AppScreen.Statistics }
                    )
                    AppScreen.ExercisesList -> ExercisesScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Profile })
                    AppScreen.Calendar -> CalendarScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Profile })
                    AppScreen.Statistics -> StatisticsScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Profile })
                }
            }
        }
    }
}