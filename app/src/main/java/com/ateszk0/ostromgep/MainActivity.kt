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
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
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
            val prefs = getSharedPreferences("ostromgep_prefs", android.content.Context.MODE_PRIVATE)
            val lang = prefs.getString("app_language", "en") ?: "en"
            getSystemService(android.app.LocaleManager::class.java).applicationLocales = android.os.LocaleList(java.util.Locale(lang))
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

enum class AppScreen { Home, Workout, Profile, ExercisesList, Calendar, Statistics, RoutineEditor }

@Composable
fun OstromgepApp(viewModel: WorkoutViewModel, themeColor: Color) {
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var isWorkoutActive by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Prevent exiting app from the bottom nav
    androidx.activity.compose.BackHandler(enabled = !isWorkoutActive && currentScreen in listOf(AppScreen.Home, AppScreen.Workout, AppScreen.Profile)) {
        // Do nothing, preventing app exit
    }

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
                NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) }, 
                        label = { Text(stringResource(R.string.nav_home)) }, 
                        selected = currentScreen == AppScreen.Home, 
                        onClick = { currentScreen = AppScreen.Home }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, indicatorColor = Color.Transparent, selectedTextColor = themeColor)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.FitnessCenter, null) }, 
                        label = { Text(stringResource(R.string.nav_workout)) }, 
                        selected = currentScreen == AppScreen.Workout, 
                        onClick = { currentScreen = AppScreen.Workout }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, indicatorColor = Color.Transparent, selectedTextColor = themeColor)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, null) }, 
                        label = { Text(stringResource(R.string.nav_profile)) }, 
                        selected = currentScreen == AppScreen.Profile, 
                        onClick = { currentScreen = AppScreen.Profile }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, indicatorColor = Color.Transparent, selectedTextColor = themeColor)
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (currentScreen) {
                    AppScreen.Home -> HomeScreen(viewModel, themeColor, onNavigateToWorkout = { 
                        currentScreen = AppScreen.Workout
                        isWorkoutActive = true 
                    })
                    AppScreen.Workout -> WorkoutTab(viewModel, themeColor, onStart = { isWorkoutActive = true }, onNavigateToRoutineEditor = { currentScreen = AppScreen.RoutineEditor })
                    AppScreen.RoutineEditor -> RoutineEditorScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Workout })
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