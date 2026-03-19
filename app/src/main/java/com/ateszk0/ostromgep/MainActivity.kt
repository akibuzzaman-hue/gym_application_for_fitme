package com.ateszk0.ostromgep

import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.model.ExerciseSessionData
import com.ateszk0.ostromgep.model.OverloadPrompt
import com.ateszk0.ostromgep.model.WorkoutSetData
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel

val DarkBackground = Color(0xFF000000); val SurfaceDark = Color(0xFF1C1C1E); val CompletedGreen = Color(0xFF2E7D32); val TextGray = Color(0xFF8E8E93); val InputBackground = Color(0xFF2C2C2E); val WarmupYellow = Color(0xFFE6C229)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val workoutViewModel: WorkoutViewModel = viewModel()
            val currentThemeName by workoutViewModel.appTheme.collectAsState()
            val themeColor = when(currentThemeName) { "Piros" -> Color(0xFFFF453A); "Sárga" -> Color(0xFFFFD60A); "Zöld" -> Color(0xFF32D74B); else -> Color(0xFF0A84FF) }

            MaterialTheme(colorScheme = darkColorScheme(background = DarkBackground, surface = SurfaceDark, primary = themeColor)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { OstromgepApp(workoutViewModel, themeColor) }
            }
        }
    }
}

enum class AppScreen { Home, Workout, Profile, ExercisesList }

@Composable
fun OstromgepApp(viewModel: WorkoutViewModel, themeColor: Color) {
    var currentScreen by remember { mutableStateOf(AppScreen.Profile) }
    var isWorkoutActive by remember { mutableStateOf(false) }

    if (isWorkoutActive) {
        ActiveWorkoutScreen(viewModel = viewModel, themeColor = themeColor, onFinishWorkout = { isWorkoutActive = false })
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = currentScreen == AppScreen.Home, onClick = { currentScreen = AppScreen.Home }, colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, selectedTextColor = themeColor, unselectedIconColor = TextGray, unselectedTextColor = TextGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(icon = { Icon(Icons.Default.FitnessCenter, null) }, label = { Text("Workout") }, selected = currentScreen == AppScreen.Workout, onClick = { currentScreen = AppScreen.Workout }, colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, selectedTextColor = themeColor, unselectedIconColor = TextGray, unselectedTextColor = TextGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") }, selected = currentScreen == AppScreen.Profile, onClick = { currentScreen = AppScreen.Profile }, colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, selectedTextColor = themeColor, unselectedIconColor = TextGray, unselectedTextColor = TextGray, indicatorColor = Color.Transparent))
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (currentScreen) {
                    AppScreen.Home -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Hamarosan...", color = TextGray) }
                    AppScreen.Workout -> WorkoutTab(viewModel, themeColor, onStart = { isWorkoutActive = true })
                    AppScreen.Profile -> DashboardProfile(viewModel, themeColor, onNavigateToExercises = { currentScreen = AppScreen.ExercisesList })
                    AppScreen.ExercisesList -> ExercisesScreen(viewModel, themeColor, onBack = { currentScreen = AppScreen.Profile })
                }
            }
        }
    }
}

@Composable
fun DashboardProfile(viewModel: WorkoutViewModel, themeColor: Color, onNavigateToExercises: () -> Unit) {
    val history by viewModel.workoutHistory.collectAsState()
    val chartData = viewModel.getChartData()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(SurfaceDark), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = TextGray, modifier = Modifier.size(40.dp)) }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Ostromgép Harcos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column { Text("Workouts", color = TextGray, fontSize = 12.sp); Text("${history.size}", color = Color.White, fontWeight = FontWeight.Bold) }
                        Column { Text("Followers", color = TextGray, fontSize = 12.sp); Text("1", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            Text("Utolsó edzések (Volumen)", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                chartData.forEach { heightRatio -> Box(modifier = Modifier.width(24.dp).fillMaxHeight(heightRatio).background(themeColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))) }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            Text("Dashboard", color = TextGray); Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { DashboardButton("Statistics", Icons.Default.BarChart, Modifier.weight(1f)) {}; DashboardButton("Exercises", Icons.Default.FitnessCenter, Modifier.weight(1f)) { onNavigateToExercises() } }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { DashboardButton("Measures", Icons.Default.Accessibility, Modifier.weight(1f)) {}; DashboardButton("Calendar", Icons.Default.CalendarMonth, Modifier.weight(1f)) {} }
            Spacer(modifier = Modifier.height(32.dp))
        }
        item { Text("Workouts", color = TextGray); Spacer(modifier = Modifier.height(8.dp)) }
        items(history.reversed().take(5)) { workout ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Edzésnapló", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Column { Text("Time", color = TextGray, fontSize = 12.sp); Text("${workout.durationSeconds/60}m", color = Color.White) }
                        Column { Text("Volume", color = TextGray, fontSize = 12.sp); Text("${workout.totalVolume.toInt()} kg", color = Color.White) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    workout.exercises.take(3).forEach { ex -> Text("${ex.sets.size} sets ${ex.name}", color = TextGray, fontSize = 14.sp) }
                }
            }
        }
    }
}

@Composable
fun DashboardButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(64.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color.White); Spacer(modifier = Modifier.width(12.dp)); Text(text, color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

// --- ÚJ: Itt is lehet Rep Range-et állítani ---
@Composable
fun ExercisesScreen(viewModel: WorkoutViewModel, themeColor: Color, onBack: () -> Unit) {
    val library by viewModel.exerciseLibrary.collectAsState()
    var search by remember { mutableStateOf("") }
    var showNewExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<ExerciseDef?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("Exercises", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = { showNewExerciseDialog = true }) { Text("Create", color = themeColor) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search exercise") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            val filtered = library.filter { it.name.contains(search, ignoreCase = true) }.sortedBy { it.name }
            items(filtered) { exDef ->
                Row(modifier = Modifier.fillMaxWidth().clickable { exerciseToEdit = exDef }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceDark), contentAlignment = Alignment.Center) { Icon(Icons.Default.FitnessCenter, null, tint = TextGray) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(exDef.name, color = Color.White, fontSize = 18.sp)
                            Text("Target: ${exDef.minReps}-${exDef.maxReps} reps", color = TextGray, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextGray)
                }
            }
        }
    }

    if (showNewExerciseDialog) {
        var customExName by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showNewExerciseDialog = false }, title = { Text("Saját gyakorlat") }, text = { OutlinedTextField(value = customExName, onValueChange = { customExName = it }, label = { Text("Gyakorlat neve") }) }, confirmButton = { Button(onClick = { viewModel.createCustomExercise(customExName); showNewExerciseDialog = false }) { Text("Hozzáadás") } }, dismissButton = { TextButton(onClick = { showNewExerciseDialog = false }) { Text("Mégse") } })
    }

    exerciseToEdit?.let { ex ->
        RepRangeDialog(
            exerciseName = ex.name, initialMin = ex.minReps, initialMax = ex.maxReps, themeColor = themeColor,
            onDismiss = { exerciseToEdit = null },
            onSave = { min, max -> viewModel.updateExerciseRepRange(ex.name, min, max); exerciseToEdit = null }
        )
    }
}

@Composable
fun WorkoutTab(viewModel: WorkoutViewModel, themeColor: Color, onStart: () -> Unit) {
    val templates by viewModel.savedTemplates.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Workout", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            IconButton(onClick = { showSettingsDialog = true }) { Icon(Icons.Default.Settings, null, tint = TextGray) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.startEmptyWorkout(); onStart() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("QUICK START", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        Spacer(modifier = Modifier.height(32.dp))
        Text("My Templates", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White); Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates) { template ->
                var showMenu by remember { mutableStateOf(false) }
                Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.startWorkoutFromTemplate(template); onStart() }, colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(template.templateName, fontSize = 18.sp, color = Color.White)
                        Box {
                            Icon(Icons.Default.MoreVert, null, tint = TextGray, modifier = Modifier.clickable { showMenu = true }.padding(8.dp))
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceDark)) {
                                DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = { showMenu = false; viewModel.deleteTemplate(template.id) })
                            }
                        }
                    }
                }
            }
        }
    }
    if (showSettingsDialog) { SettingsDialog(viewModel = viewModel, currentThemeColor = themeColor, onDismiss = { showSettingsDialog = false }) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActiveWorkoutScreen(viewModel: WorkoutViewModel, themeColor: Color, onFinishWorkout: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val exercises by viewModel.activeExercises.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val library by viewModel.exerciseLibrary.collectAsState()
    val overloadPrompts by viewModel.overloadPrompts.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showPlateCalculator by remember { mutableStateOf(false) }
    var exerciseToEditRepRange by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalVolume = exercises.flatMap { it.sets }.filter { it.isCompleted && !it.isWarmup }.sumOf { (it.kg.toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }
    val completedSetsCount = exercises.flatMap { it.sets }.count { it.isCompleted }

    var lastTimerValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(restTimerSeconds) {
        if (lastTimerValue > 0 && restTimerSeconds == 0) {
            try { val ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)); ringtone?.play() } catch (e: Exception) {}
        }
        lastTimerValue = restTimerSeconds
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Log Workout") }, actions = { IconButton(onClick = { showPlateCalculator = true }) { Icon(Icons.Default.Calculate, null, tint = themeColor) }; Button(onClick = { showSaveTemplateDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Finish", color = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)) },
        bottomBar = {
            Column {
                if (restTimerSeconds > 0) Row(modifier = Modifier.fillMaxWidth().background(themeColor).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row { TimerAdjustButton("-15") { viewModel.adjustRestTimer(-15) }; Text("%02d:%02d".format(restTimerSeconds/60, restTimerSeconds%60), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp)); TimerAdjustButton("+15") { viewModel.adjustRestTimer(15) } }; Button(onClick = { viewModel.skipRestTimer() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f))) { Text("Skip") } }
                Row(modifier = Modifier.fillMaxWidth().background(DarkBackground).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) { Text("Settings") }; Button(onClick = { viewModel.finishWorkout(null); onFinishWorkout() }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) { Text("Discard", color = Color.Red) } }
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(innerPadding)) {
            item { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { StatItem("Duration", "%02d:%02d".format(totalSeconds/60, totalSeconds%60)); StatItem("Volume", "${totalVolume.toInt()} kg"); StatItem("Sets", "$completedSetsCount") } }
            itemsIndexed(exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                val isDragged = draggedIndex == index; val zIndex = if (isDragged) 1f else 0f; val scale = if (isDragged) 1.02f else 1f; val alpha = if (isDragged) 0.8f else 1f; val translationY = if (isDragged) dragOffset else 0f; val elevation = if (isDragged) 8.dp else 0.dp
                Box(modifier = Modifier.fillMaxWidth().zIndex(zIndex).graphicsLayer { this.translationY = translationY; this.scaleX = scale; this.scaleY = scale; this.alpha = alpha }.shadow(elevation, RoundedCornerShape(8.dp)).background(if (isDragged) SurfaceDark.copy(0.5f) else Color.Transparent).animateItemPlacement()) {
                    ExerciseBlock(
                        exercise, index, exercises.size, themeColor, { draggedIndex = index }, { delta -> dragOffset += delta; val itemHeight = 350f; if (dragOffset > itemHeight * 0.5f && index < exercises.size - 1) { viewModel.swapExercises(index, index + 1); draggedIndex = index + 1; dragOffset -= itemHeight; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } else if (dragOffset < -itemHeight * 0.5f && index > 0) { viewModel.swapExercises(index, index - 1); draggedIndex = index - 1; dragOffset += itemHeight; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }, { draggedIndex = null; dragOffset = 0f },
                        { s -> viewModel.updateSet(exercise.id, s) }, { s -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleSetComplete(exercise.id, s) }, { viewModel.addSet(exercise.id) }, { id -> viewModel.deleteSet(exercise.id, id) }, { sec -> viewModel.updateExerciseRestTime(exercise.id, sec) }, { id -> viewModel.toggleWarmup(exercise.id, id) }, { note -> viewModel.updateExerciseNote(exercise.id, note) }, { viewModel.deleteExercise(exercise.id) },
                        onEditRepRange = { exerciseToEditRepRange = exercise.name } // ÚJ callback
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            item { Button(onClick = { showBottomSheet = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Icon(Icons.Default.Add, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Add Exercise", color = Color.White) }; Spacer(modifier = Modifier.height(32.dp)) }
        }

        // --- ÚJ: OVERLOAD DIALOG (HA VAN JAVASLAT) ---
        if (overloadPrompts.isNotEmpty()) {
            ProgressiveOverloadDialog(
                initialPrompts = overloadPrompts,
                themeColor = themeColor,
                onApply = { finalPrompts -> viewModel.applyOverloadPrompts(finalPrompts) },
                onDismiss = { viewModel.dismissOverloadPrompts() }
            )
        }

        exerciseToEditRepRange?.let { exName ->
            val def = library.find { it.name == exName }
            RepRangeDialog(
                exerciseName = exName, initialMin = def?.minReps ?: 8, initialMax = def?.maxReps ?: 12, themeColor = themeColor,
                onDismiss = { exerciseToEditRepRange = null },
                onSave = { min, max -> viewModel.updateExerciseRepRange(exName, min, max); exerciseToEditRepRange = null }
            )
        }

        if (showPlateCalculator) { PlateCalculatorDialog(onDismiss = { showPlateCalculator = false }, themeColor = themeColor) }
        if (showBottomSheet) { ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState, containerColor = SurfaceDark) { LazyColumn { items(library) { exDef -> ListItem(headlineContent = { Text(exDef.name, color = Color.White) }, modifier = Modifier.clickable { viewModel.addNewExerciseBlock(exDef.name); showBottomSheet = false }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) } } } }
        if (showSaveTemplateDialog) { var name by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showSaveTemplateDialog = false }, title = { Text("Befejezés") }, text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Sablon neve") }) }, confirmButton = { Button(onClick = { viewModel.finishWorkout(if (name.isNotBlank()) name else null); showSaveTemplateDialog = false; onFinishWorkout() }) { Text("Mentés") } }) }
    }
}

// --- ÚJ: A PROGRESSZÍV TÚLTERHELÉS VARÁZSLÓ UI-JA ---
@Composable
fun ProgressiveOverloadDialog(initialPrompts: List<OverloadPrompt>, themeColor: Color, onApply: (List<OverloadPrompt>) -> Unit, onDismiss: () -> Unit) {
    var statePrompts by remember { mutableStateOf(initialPrompts) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f).clip(RoundedCornerShape(16.dp)).background(SurfaceDark),
        title = { Text("Progresszív Túlterhelés", fontWeight = FontWeight.Bold, color = themeColor) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { Text("A következő gyakorlatoknál elérted a célt. Adjuk hozzá az extra terhelést az e-havi edzéstervedhez?", color = TextGray, modifier = Modifier.padding(bottom = 16.dp)) }

                items(statePrompts) { prompt ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = DarkBackground)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = prompt.isSelected, onCheckedChange = { chk -> statePrompts = statePrompts.map { if (it.exerciseId == prompt.exerciseId) it.copy(isSelected = chk) else it } }, colors = CheckboxDefaults.colors(checkedColor = themeColor))
                                Text(prompt.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                            Text("Legutóbb: ${prompt.oldWeight}kg x ${prompt.oldReps} ism.", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(start = 48.dp))

                            if (prompt.isSelected) {
                                Spacer(modifier = Modifier.height(12.dp))
                                if (!prompt.requiresWeightIncrease) {
                                    // Sima ismétlés növelés
                                    Text("🎯 Új Cél: ${prompt.newWeightStr}kg x ${prompt.newRepsStr} ism.", color = CompletedGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp))
                                } else {
                                    // SÚLYEMELÉS SZÜKSÉGES!
                                    Text("🔥 Elérted a maxot (${prompt.maxReps})! Súlyemelés javasolt:", color = WarmupYellow, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp, bottom = 8.dp))
                                    Row(modifier = Modifier.padding(start = 48.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val increments = listOf(0.5, 1.0, 2.5, 5.0)
                                        increments.forEach { inc ->
                                            Button(onClick = { statePrompts = statePrompts.map { if (it.exerciseId == prompt.exerciseId) it.copy(newWeightStr = OverloadPrompt.formatWeight(prompt.oldWeight + inc)) else it } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark), contentPadding = PaddingValues(0.dp)) { Text("+$inc", fontSize = 12.sp) }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = prompt.newWeightStr,
                                        onValueChange = { v -> statePrompts = statePrompts.map { if (it.exerciseId == prompt.exerciseId) it.copy(newWeightStr = v) else it } },
                                        label = { Text("Új Súly (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.padding(start = 48.dp).fillMaxWidth()
                                    )
                                    Text("Új Cél: ${prompt.newWeightStr}kg x ${prompt.minReps} ism.", color = CompletedGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp, top = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onApply(statePrompts) }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Alkalmaz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kihagyás", color = TextGray) } }
    )
}

// --- ÚJ: Rep Range beállító Dialógus ---
@Composable
fun RepRangeDialog(exerciseName: String, initialMin: Int, initialMax: Int, themeColor: Color, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var minInput by remember { mutableStateOf(initialMin.toString()) }
    var maxInput by remember { mutableStateOf(initialMax.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Céltartomány: $exerciseName") },
        text = {
            Column {
                Text("Állítsd be, hány ismétlést szeretnél elérni, mielőtt súlyt emelsz.", color = TextGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(value = minInput, onValueChange = { minInput = it }, label = { Text("Minimum") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxInput, onValueChange = { maxInput = it }, label = { Text("Maximum") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(minInput.toIntOrNull() ?: 8, maxInput.toIntOrNull() ?: 12) }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Mentés") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Mégse") } }
    )
}

// ... IDE JÖNNEK A VÁLTOZATLAN RÉSZEK (SettingsDialog, PlateCalculatorDialog, StatItem, TimerAdjustButton, CustomTextField) ...

@Composable
fun SettingsDialog(viewModel: WorkoutViewModel, currentThemeColor: Color, onDismiss: () -> Unit) { val themes = listOf("Kék", "Piros", "Sárga", "Zöld"); AlertDialog(onDismissRequest = onDismiss, title = { Text("Beállítások") }, text = { Column { Text("Alkalmazás Témája:", fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp)); themes.forEach { themeName -> val color = when(themeName) { "Piros" -> Color(0xFFFF453A); "Sárga" -> Color(0xFFFFD60A); "Zöld" -> Color(0xFF32D74B); else -> Color(0xFF0A84FF) }; Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setTheme(themeName) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color)); Spacer(modifier = Modifier.width(16.dp)); Text(themeName, fontSize = 16.sp) } } } }, confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = currentThemeColor)) { Text("Bezárás") } }) }
@Composable
fun PlateCalculatorDialog(onDismiss: () -> Unit, themeColor: Color) { var weightInput by remember { mutableStateOf("") }; var barWeight by remember { mutableStateOf("20") }; val platesAvailable = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25); val targetWeight = weightInput.toDoubleOrNull() ?: 0.0; val bar = barWeight.toDoubleOrNull() ?: 20.0; val perSide = (targetWeight - bar) / 2.0; val platesNeeded = mutableMapOf<Double, Int>(); var remaining = perSide; if (remaining > 0) { for (plate in platesAvailable) { val count = (remaining / plate).toInt(); if (count > 0) { platesNeeded[plate] = count; remaining -= count * plate } } }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Tányér Kalkulátor") }, text = { Column(modifier = Modifier.fillMaxWidth()) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = weightInput, onValueChange = { weightInput = it }, label = { Text("Célsúly (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)); OutlinedTextField(value = barWeight, onValueChange = { barWeight = it }, label = { Text("Rúd (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)) }; Spacer(modifier = Modifier.height(16.dp)); if (perSide > 0) { Text("Oldalanként kell:", fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp)); platesNeeded.forEach { (plate, count) -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("${plate} kg tárcsa:", color = TextGray); Text("${count} db", fontWeight = FontWeight.Bold) } }; if (remaining > 0.1) Text("Nem kerekíthető pontosan, maradék: ${"%.2f".format(remaining)} kg", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) } else if (targetWeight > 0) Text("A súly kevesebb, mint a rúd súlya!", color = Color.Red) } }, confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Bezárás") } }) }
@Composable
fun StatItem(label: String, value: String) { Column { Text(text = label, color = TextGray, fontSize = 12.sp); Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
@Composable
fun TimerAdjustButton(text: String, onClick: () -> Unit) { Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.2f)).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, isCompleted: Boolean, themeColor: Color) { BasicTextField(value = value, onValueChange = onValueChange, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), cursorBrush = SolidColor(themeColor), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (isCompleted) Color.Transparent else InputBackground).padding(vertical = 8.dp)) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseBlock(
    exercise: ExerciseSessionData, index: Int, totalExercises: Int, themeColor: Color, onDragStart: () -> Unit, onDragDelta: (Float) -> Unit, onDragEnd: () -> Unit, onSetUpdate: (WorkoutSetData) -> Unit, onSetCompleteToggle: (WorkoutSetData) -> Unit, onAddSet: () -> Unit, onDeleteSet: (Int) -> Unit, onUpdateRestTime: (Int) -> Unit, onToggleWarmup: (Int) -> Unit, onNoteUpdate: (String) -> Unit, onDeleteExercise: () -> Unit,
    onEditRepRange: () -> Unit // ÚJ
) {
    val haptic = LocalHapticFeedback.current; var showRestPickerDialog by remember { mutableStateOf(false) }; var showOptionsMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().pointerInput(exercise.id) { detectDragGesturesAfterLongPress(onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onDragStart() }, onDrag = { change, dragAmount -> change.consume(); onDragDelta(dragAmount.y) }, onDragEnd = onDragEnd, onDragCancel = onDragEnd) }) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White)); Spacer(modifier = Modifier.width(12.dp)); Text(text = exercise.name, color = themeColor, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.clickable { showRestPickerDialog = true }.background(SurfaceDark, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Timer, null, tint = TextGray, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("${exercise.restTimerDuration}s", color = TextGray, fontSize = 12.sp) }
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextGray, modifier = Modifier.clickable { showOptionsMenu = true }.padding(4.dp))
                    DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }, modifier = Modifier.background(SurfaceDark)) {
                        // ÚJ: Céltartomány beállítása
                        DropdownMenuItem(text = { Text("Céltartomány (Rep Range)", color = Color.White) }, onClick = { showOptionsMenu = false; onEditRepRange() })
                        DropdownMenuItem(text = { Text("Gyakorlat Törlése", color = Color.Red) }, onClick = { showOptionsMenu = false; onDeleteExercise() })
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) { if (exercise.note.isEmpty()) { Text("Add notes here...", color = TextGray.copy(alpha = 0.5f), fontSize = 12.sp) }; BasicTextField(value = exercise.note, onValueChange = onNoteUpdate, textStyle = TextStyle(color = TextGray, fontSize = 12.sp), modifier = Modifier.fillMaxWidth(), cursorBrush = SolidColor(themeColor)) }
        Spacer(modifier = Modifier.height(12.dp)); Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Text("SET", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.1f), textAlign = TextAlign.Center); Text("PREVIOUS", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center); Text("KG", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center); Text("REPS", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center); Text("RPE", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.15f), textAlign = TextAlign.Center); Text("✔", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.1f), textAlign = TextAlign.Center) }; Spacer(modifier = Modifier.height(4.dp))

        exercise.sets.forEach { set ->
            val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDeleteSet(set.id); true } else false })
            key(set.id) {
                SwipeToDismissBox(state = dismissState, enableDismissFromStartToEnd = false, enableDismissFromEndToStart = exercise.sets.size > 1, backgroundContent = { val color by animateColorAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent, label = "swipe"); Box(Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Default.Delete, null, tint = Color.White) } }, content = {
                    val rowBg = if (set.isCompleted) CompletedGreen.copy(alpha = 0.2f) else Color.Transparent
                    Row(modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = set.setLabel, color = if (set.isWarmup) WarmupYellow else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.1f).clickable { onToggleWarmup(set.id) }, textAlign = TextAlign.Center)
                        Text(text = set.previousText, color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(modifier = Modifier.weight(0.2f).padding(horizontal = 4.dp)) { CustomTextField(value = set.kg, onValueChange = { onSetUpdate(set.copy(kg = it)) }, isCompleted = set.isCompleted, themeColor = themeColor) }
                        Box(modifier = Modifier.weight(0.2f).padding(horizontal = 4.dp)) { CustomTextField(value = set.reps, onValueChange = { onSetUpdate(set.copy(reps = it)) }, isCompleted = set.isCompleted, themeColor = themeColor) }
                        Box(modifier = Modifier.weight(0.15f).padding(horizontal = 4.dp)) { CustomTextField(value = set.rpe, onValueChange = { onSetUpdate(set.copy(rpe = it)) }, isCompleted = set.isCompleted, themeColor = themeColor) }
                        Box(modifier = Modifier.weight(0.1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(if (set.isCompleted) CompletedGreen else InputBackground).clickable { onSetCompleteToggle(set) }, contentAlignment = Alignment.Center) { if (set.isCompleted) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                })
            }
        }
        Spacer(modifier = Modifier.height(8.dp)); Text(text = "+ Add Set", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark).clickable { onAddSet() }.padding(vertical = 12.dp), textAlign = TextAlign.Center)
    }

    if (showRestPickerDialog) { var tempTime by remember { mutableStateOf(exercise.restTimerDuration.toString()) }; AlertDialog(onDismissRequest = { showRestPickerDialog = false }, title = { Text("Pihenőidő (másodperc)") }, text = { OutlinedTextField(value = tempTime, onValueChange = { tempTime = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }, confirmButton = { Button(onClick = { onUpdateRestTime(tempTime.toIntOrNull() ?: 90); showRestPickerDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("OK", color = Color.White) } }, dismissButton = { TextButton(onClick = { showRestPickerDialog = false }) { Text("Mégse", color = themeColor) } }) }
}