package com.ateszk0.ostromgep

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.core.content.ContextCompat
import androidx.compose.animation.with
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Vertical lock — always portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
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
            val isWorkoutActive by workoutViewModel.isWorkoutActive.collectAsState()

            // Keep screen on during workout
            val window = (androidx.compose.ui.platform.LocalContext.current as? android.app.Activity)?.window
            LaunchedEffect(isWorkoutActive) {
                if (isWorkoutActive) {
                    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            
            val themeColor = when(currentThemeName) { 
                "Piros" -> Color(0xFFFF453A)
                "Sárga" -> Color(0xFFFFD60A)
                "Zöld" -> Color(0xFF32D74B)
                "Lila" -> Color(0xFFAF52DE)
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
            
            val workoutSummary by workoutViewModel.workoutSummary.collectAsState()

            MaterialTheme(colorScheme = customColorScheme) { 
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) { 
                    OstromgepApp(workoutViewModel, themeColor) 
                }
                // Wrap-up dialog shown after saving workout
                workoutSummary?.let { summary ->
                    com.ateszk0.ostromgep.ui.components.WorkoutWrapUpDialog(
                        summary = summary,
                        themeColor = themeColor,
                        onDismiss = { workoutViewModel.clearWorkoutSummary() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

enum class AppScreen { Home, Workout, Profile, ExercisesList, Calendar, Statistics, RoutineEditor, ExploreRoutines, WorkoutLog }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OstromgepApp(viewModel: WorkoutViewModel, themeColor: Color) {
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var workoutLogTimestampToShow by remember { mutableStateOf<Long?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Collect workout state from ViewModel (survives config changes and process death)
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()
    val isWorkoutMinimized by viewModel.isWorkoutMinimized.collectAsState()

    // Handle notification tap: open workout screen
    val activity = context as? android.app.Activity
    LaunchedEffect(Unit) {
        val openWorkout = activity?.intent?.action == "ACTION_OPEN_WORKOUT"
        if (openWorkout && isWorkoutActive) {
            viewModel.setWorkoutMinimized(false)
            activity?.intent?.action = null // consume
        }
    }
    
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

    androidx.compose.animation.AnimatedContent(
        targetState = isWorkoutActive && !isWorkoutMinimized,
        transitionSpec = {
            if (targetState) {
                androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn() togetherWith 
                androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.95f)
            } else {
                androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.95f) togetherWith
                androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
            }
        },
        label = "workout_fullscreen"
    ) { showFullscreenWorkout ->
        if (showFullscreenWorkout) {
            ActiveWorkoutScreen(
                viewModel = viewModel, 
                themeColor = themeColor, 
                onFinishWorkout = { viewModel.setWorkoutActive(false) },
                onMinimize = { viewModel.setWorkoutMinimized(true) }
            )
        } else {
            Scaffold(
                bottomBar = {
                    androidx.compose.foundation.layout.Column {
                        // Floating workout bar when minimized
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isWorkoutActive && isWorkoutMinimized,
                            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                        ) {
                            val totalSeconds by viewModel.totalSeconds.collectAsState()
                        val exercises by viewModel.activeExercises.collectAsState()
                        val mins = totalSeconds / 60
                        val secs = totalSeconds % 60
                        val currentExerciseName = exercises.lastOrNull()?.name ?: ""
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceDark)
                                .clickable { viewModel.setWorkoutMinimized(false) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Up arrow to restore
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Restore workout",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // Workout info
                            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF32D74B)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Workout ${mins}min ${secs}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (currentExerciseName.isNotEmpty()) {
                                    Text(currentExerciseName, color = com.ateszk0.ostromgep.ui.theme.TextGray, fontSize = 12.sp)
                                }
                            }
                            // Discard button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray)
                                    .clickable { showDiscardDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Discard workout",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
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
            } // End of bottomBar Column
        } // End of bottomBar lambda
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (showDiscardDialog) {
                    AlertDialog(
                        onDismissRequest = { showDiscardDialog = false },
                        title = { Text(stringResource(R.string.discard_dialog_title), color = Color.White) },
                        text = { Text(stringResource(R.string.discard_dialog_text), color = com.ateszk0.ostromgep.ui.theme.TextGray) },
                        confirmButton = {
                            Button(onClick = { 
                                viewModel.discardWorkout()
                                showDiscardDialog = false
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                Text(stringResource(R.string.discard_confirm), color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.cancel_btn), color = themeColor) }
                        }
                    )
                }

                androidx.compose.animation.AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220, delayMillis = 90)) +
                        androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(220, delayMillis = 90))) with
                        androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(90))
                    },
                    label = "screen_transition"
                ) { targetScreen ->
                    when (targetScreen) {
                        AppScreen.Home -> HomeScreen(viewModel, themeColor, onNavigateToWorkout = { 
                            currentScreen = AppScreen.Workout
                            viewModel.setWorkoutActive(true)
                        })
                        AppScreen.Workout -> WorkoutTab(viewModel, themeColor, onStart = { viewModel.setWorkoutActive(true) }, onNavigateToRoutineEditor = { currentScreen = AppScreen.RoutineEditor }, onNavigateToExplore = { currentScreen = AppScreen.ExploreRoutines })
                        AppScreen.RoutineEditor -> RoutineEditorScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Workout })
                        AppScreen.ExploreRoutines -> ExploreRoutinesScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Workout })
                        AppScreen.Profile -> DashboardProfile(
                            viewModel, 
                            themeColor, 
                            onNavigateToExercises = { currentScreen = AppScreen.ExercisesList },
                            onNavigateToCalendar = { currentScreen = AppScreen.Calendar },
                            onNavigateToStatistics = { currentScreen = AppScreen.Statistics },
                            onNavigateToWorkoutLog = { timestamp ->
                                workoutLogTimestampToShow = timestamp
                                currentScreen = AppScreen.WorkoutLog
                            }
                        )
                        AppScreen.ExercisesList -> ExercisesScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Profile })
                        AppScreen.Calendar -> CalendarScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Profile })
                        AppScreen.Statistics -> StatisticsScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Profile })
                        AppScreen.WorkoutLog -> WorkoutLogScreen(
                            viewModel = viewModel,
                            themeColor = themeColor,
                            onBack = { currentScreen = AppScreen.Profile },
                            onNavigateToActiveWorkout = {
                                currentScreen = AppScreen.Workout
                                viewModel.setWorkoutActive(true)
                            },
                            initialEntryTimestamp = workoutLogTimestampToShow
                        )
                    }
                }
            }
        }
        }
    }
}