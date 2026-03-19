package com.ateszk0.ostromgep

import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.Timer // Explicit Timer import!
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
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

val DarkBackground = Color(0xFF000000); val SurfaceDark = Color(0xFF1C1C1E); val CompletedGreen = Color(0xFF32D74B); val TextGray = Color(0xFF8E8E93); val InputBackground = Color(0xFF2C2C2E); val WarmupYellow = Color(0xFFFFD60A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val workoutViewModel: WorkoutViewModel = viewModel()
            val currentThemeName by workoutViewModel.appTheme.collectAsState()
            val themeColor = when(currentThemeName) { "Piros" -> Color(0xFFFF453A); "Sárga" -> Color(0xFFFFD60A); "Zöld" -> Color(0xFF32D74B); else -> Color(0xFF0A84FF) }

            val customColorScheme = darkColorScheme(primary = themeColor, onPrimary = Color.White, secondary = themeColor, background = DarkBackground, surface = SurfaceDark, onSurface = Color.White, primaryContainer = SurfaceDark, onPrimaryContainer = Color.White)
            MaterialTheme(colorScheme = customColorScheme) { Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) { OstromgepApp(workoutViewModel, themeColor) } }
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
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = currentScreen == AppScreen.Home, onClick = { currentScreen = AppScreen.Home }, colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, indicatorColor = Color.Transparent, selectedTextColor = themeColor))
                    NavigationBarItem(icon = { Icon(Icons.Default.FitnessCenter, null) }, label = { Text("Workout") }, selected = currentScreen == AppScreen.Workout, onClick = { currentScreen = AppScreen.Workout }, colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, indicatorColor = Color.Transparent, selectedTextColor = themeColor))
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") }, selected = currentScreen == AppScreen.Profile, onClick = { currentScreen = AppScreen.Profile }, colors = NavigationBarItemDefaults.colors(selectedIconColor = themeColor, indicatorColor = Color.Transparent, selectedTextColor = themeColor))
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
    val history by viewModel.workoutHistory.collectAsState(); val chartData = viewModel.getChartData()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(SurfaceDark), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = TextGray, modifier = Modifier.size(40.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column { Text("Ostromgép Harcos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White); Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { Column { Text("Workouts", color = TextGray, fontSize = 12.sp); Text("${history.size}", color = Color.White, fontWeight = FontWeight.Bold) }; Column { Text("Followers", color = TextGray, fontSize = 12.sp); Text("1", color = Color.White, fontWeight = FontWeight.Bold) } } } }; Spacer(modifier = Modifier.height(24.dp)) }
        item { Text("Utolsó edzések (Volumen)", color = Color.White, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) { chartData.forEach { heightRatio -> Box(modifier = Modifier.width(24.dp).fillMaxHeight(heightRatio).background(themeColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))) } }; Spacer(modifier = Modifier.height(24.dp)) }
        item { Text("Dashboard", color = TextGray); Spacer(modifier = Modifier.height(8.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { DashboardButton("Statistics", Icons.Default.BarChart, Modifier.weight(1f)) {}; DashboardButton("Exercises", Icons.Default.FitnessCenter, Modifier.weight(1f)) { onNavigateToExercises() } }; Spacer(modifier = Modifier.height(8.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { DashboardButton("Measures", Icons.Default.Accessibility, Modifier.weight(1f)) {}; DashboardButton("Calendar", Icons.Default.CalendarMonth, Modifier.weight(1f)) {} }; Spacer(modifier = Modifier.height(32.dp)) }
        item { Text("Workouts", color = TextGray); Spacer(modifier = Modifier.height(8.dp)) }
        items(history.reversed().take(5)) { workout -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) { Column(modifier = Modifier.padding(16.dp)) { Text("Edzésnapló", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp); Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) { Column { Text("Time", color = TextGray, fontSize = 12.sp); Text("${workout.durationSeconds/60}m", color = Color.White) }; Column { Text("Volume", color = TextGray, fontSize = 12.sp); Text("${workout.totalVolume.toInt()} kg", color = Color.White) } }; Spacer(modifier = Modifier.height(8.dp)); workout.exercises.take(3).forEach { ex -> Text("${ex.sets.size} sets ${ex.name}", color = TextGray, fontSize = 14.sp) } } } }
    }
}

@Composable
fun DashboardButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(64.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SurfaceDark)) { Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color.White); Spacer(modifier = Modifier.width(12.dp)); Text(text, color = Color.White, fontWeight = FontWeight.Bold) } }
}

@Composable
fun ExercisesScreen(viewModel: WorkoutViewModel, themeColor: Color, onBack: () -> Unit) {
    val library by viewModel.exerciseLibrary.collectAsState(); var search by remember { mutableStateOf("") }; var showNewExerciseDialog by remember { mutableStateOf(false) }; var exerciseToEdit by remember { mutableStateOf<ExerciseDef?>(null) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }; Text("Exercises", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White); TextButton(onClick = { showNewExerciseDialog = true }) { Text("Create", color = themeColor) } }
        Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search exercise") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true); Spacer(modifier = Modifier.height(16.dp))
        LazyColumn { val filtered = library.filter { it.name.contains(search, ignoreCase = true) }.sortedBy { it.name }; items(filtered) { exDef -> Row(modifier = Modifier.fillMaxWidth().clickable { exerciseToEdit = exDef }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceDark), contentAlignment = Alignment.Center) { Icon(Icons.Default.FitnessCenter, null, tint = TextGray) }; Spacer(modifier = Modifier.width(16.dp)); Column { Text(exDef.name, color = Color.White, fontSize = 18.sp); Text("Target: ${exDef.minReps}-${exDef.maxReps} reps", color = TextGray, fontSize = 12.sp) } }; Icon(Icons.Default.Edit, null, tint = TextGray) } } }
    }
    if (showNewExerciseDialog) { var n by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showNewExerciseDialog = false }, title = { Text("Saját gyakorlat") }, text = { OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Gyakorlat neve") }) }, confirmButton = { Button(onClick = { viewModel.createCustomExercise(n); showNewExerciseDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Hozzáadás", color = Color.White) } }, dismissButton = { TextButton(onClick = { showNewExerciseDialog = false }) { Text("Mégse", color = themeColor) } }) }
    exerciseToEdit?.let { ex -> RepRangeDialog(ex.name, ex.minReps, ex.maxReps, themeColor, { exerciseToEdit = null }, { min, max -> viewModel.updateExerciseRepRange(ex.name, min, max); exerciseToEdit = null }) }
}

@Composable
fun WorkoutTab(viewModel: WorkoutViewModel, themeColor: Color, onStart: () -> Unit) {
    val templates by viewModel.savedTemplates.collectAsState(); var showSettingsDialog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Workout", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White); IconButton(onClick = { showSettingsDialog = true }) { Icon(Icons.Default.Settings, null, tint = TextGray) } }
        Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { viewModel.startEmptyWorkout(); onStart() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("QUICK START", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        Spacer(modifier = Modifier.height(32.dp)); Text("My Templates", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White); Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(templates) { template -> var showMenu by remember { mutableStateOf(false) }; Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.startWorkoutFromTemplate(template); onStart() }, colors = CardDefaults.cardColors(containerColor = SurfaceDark)) { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(template.templateName, fontSize = 18.sp, color = Color.White); Box { Icon(Icons.Default.MoreVert, null, tint = TextGray, modifier = Modifier.clickable { showMenu = true }.padding(8.dp)); DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceDark)) { DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = { showMenu = false; viewModel.deleteTemplate(template.id) }) } } } } } }
    }
    if (showSettingsDialog) { SettingsDialog(viewModel, themeColor) { showSettingsDialog = false } }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActiveWorkoutScreen(viewModel: WorkoutViewModel, themeColor: Color, onFinishWorkout: () -> Unit) {
    val context = LocalContext.current; val haptic = LocalHapticFeedback.current
    val exercises by viewModel.activeExercises.collectAsState(); val totalSeconds by viewModel.totalSeconds.collectAsState()
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState(); val library by viewModel.exerciseLibrary.collectAsState(); val prompts by viewModel.overloadPrompts.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }; var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showPlateCalculator by remember { mutableStateOf(false) }; var showSettingsDialog by remember { mutableStateOf(false) }
    var exerciseToEditRepRange by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalVolume = exercises.flatMap { it.sets }.filter { it.isCompleted && !it.isWarmup }.sumOf { (it.kg.toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }
    val completedSetsCount = exercises.flatMap { it.sets }.count { it.isCompleted }

    var lastTimerValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(restTimerSeconds) { if (lastTimerValue > 0 && restTimerSeconds == 0) { try { RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play() } catch (e: Exception) {} }; lastTimerValue = restTimerSeconds }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }; var dragOffset by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Log Workout", color = Color.White) }, actions = { IconButton(onClick = { showPlateCalculator = true }) { Icon(Icons.Default.Calculate, null, tint = themeColor) }; Button(onClick = { showSaveTemplateDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Finish", color = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)) },
        bottomBar = {
            Column {
                if (restTimerSeconds > 0) Row(modifier = Modifier.fillMaxWidth().background(themeColor).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row { TimerAdjustButton("-15") { viewModel.adjustRestTimer(-15) }; Text("%02d:%02d".format(restTimerSeconds/60, restTimerSeconds%60), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp)); TimerAdjustButton("+15") { viewModel.adjustRestTimer(15) } }; Button(onClick = { viewModel.skipRestTimer() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f))) { Text("Skip", color = Color.White) } }
                Row(modifier = Modifier.fillMaxWidth().background(DarkBackground).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { showSettingsDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) { Text("Settings", color = Color.White) }
                    Button(onClick = { viewModel.finishWorkout(null); onFinishWorkout() }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) { Text("Discard", color = Color.Red) }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(innerPadding)) {
            item { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { StatItem("Duration", "%02d:%02d".format(totalSeconds/60, totalSeconds%60)); StatItem("Volume", "${totalVolume.toInt()} kg"); StatItem("Sets", "$completedSetsCount") } }
            itemsIndexed(exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                val isDragged = draggedIndex == index; val zIndex = if (isDragged) 1f else 0f; val scale = if (isDragged) 1.02f else 1f; val alpha = if (isDragged) 0.8f else 1f; val translationY = if (isDragged) dragOffset else 0f; val elevation = if (isDragged) 8.dp else 0.dp
                Box(modifier = Modifier.fillMaxWidth().zIndex(zIndex).graphicsLayer { this.translationY = translationY; this.scaleX = scale; this.scaleY = scale; this.alpha = alpha }.shadow(elevation, RoundedCornerShape(8.dp)).background(if (isDragged) SurfaceDark.copy(0.5f) else Color.Transparent).animateItemPlacement()) {
                    ExerciseBlock(
                        exercise, index, exercises.size, themeColor,
                        { viewModel.moveExerciseUp(index) }, { viewModel.moveExerciseDown(index) },
                        { s -> viewModel.updateSet(exercise.id, s) }, { s -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleSetComplete(exercise.id, s) }, { viewModel.addSet(exercise.id) }, { id -> viewModel.deleteSet(exercise.id, id) }, { sec -> viewModel.updateExerciseRestTime(exercise.id, sec) }, { id -> viewModel.toggleWarmup(exercise.id, id) }, { note -> viewModel.updateExerciseNote(exercise.id, note) }, { viewModel.deleteExercise(exercise.id) },
                        { exerciseToEditRepRange = exercise.name }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            item { Button(onClick = { showBottomSheet = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Icon(Icons.Default.Add, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Add Exercise", color = Color.White) }; Spacer(modifier = Modifier.height(32.dp)) }
        }
        if (prompts.isNotEmpty()) ProgressiveOverloadDialog(prompts, themeColor, { p -> viewModel.applyOverloadPrompts(p) }, { viewModel.dismissOverloadPrompts() })
        if (showSettingsDialog) SettingsDialog(viewModel, themeColor) { showSettingsDialog = false }
        if (showPlateCalculator) PlateCalculatorDialog({ showPlateCalculator = false }, themeColor)
        exerciseToEditRepRange?.let { n -> val def = library.find { it.name == n }; RepRangeDialog(n, def?.minReps ?: 8, def?.maxReps ?: 12, themeColor, { exerciseToEditRepRange = null }, { min, max -> viewModel.updateExerciseRepRange(n, min, max); exerciseToEditRepRange = null }) }
        if (showBottomSheet) ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState, containerColor = SurfaceDark) { LazyColumn { items(library) { exDef -> ListItem(headlineContent = { Text(exDef.name, color = Color.White) }, modifier = Modifier.clickable { viewModel.addNewExerciseBlock(exDef.name); showBottomSheet = false }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) } } }
        if (showSaveTemplateDialog) { var n by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showSaveTemplateDialog = false }, title = { Text("Befejezés", color = Color.White) }, text = { OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Sablon neve") }) }, confirmButton = { Button(onClick = { viewModel.finishWorkout(if (n.isNotBlank()) n else null); showSaveTemplateDialog = false; onFinishWorkout() }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Befejezés", color = Color.White) } }, dismissButton = { TextButton(onClick = { showSaveTemplateDialog = false }) { Text("Mégse", color = themeColor) } }) }
    }
}

@Composable
fun ProgressiveOverloadDialog(initialPrompts: List<OverloadPrompt>, themeColor: Color, onApply: (List<OverloadPrompt>) -> Unit, onDismiss: () -> Unit) {
    var statePrompts by remember { mutableStateOf(initialPrompts) }
    AlertDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false), modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f).clip(RoundedCornerShape(16.dp)).background(SurfaceDark),
        title = { Text("Progresszív Túlterhelés", fontWeight = FontWeight.Bold, color = themeColor) },
        text = {
            LazyColumn {
                item { Text("Javasolt változtatások a múltkori edzésed alapján:", color = TextGray, modifier = Modifier.padding(bottom = 16.dp)) }
                items(statePrompts) { prompt ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = DarkBackground)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = prompt.isSelected, onCheckedChange = { chk -> statePrompts = statePrompts.map { if (it.exerciseId == prompt.exerciseId) it.copy(isSelected = chk) else it } }, colors = CheckboxDefaults.colors(checkedColor = themeColor)); Text(prompt.name, fontWeight = FontWeight.Bold, color = Color.White) }
                            if (prompt.isSelected) {
                                if (!prompt.requiresWeightIncrease) { Text("🎯 Cél: +1 ismétlés (${prompt.newRepsStr} ism)", color = CompletedGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp)) }
                                else {
                                    Text("🔥 Súlyemelés javasolt!", color = WarmupYellow, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp))
                                    Row(modifier = Modifier.padding(start = 48.dp, top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(0.5, 1.0, 2.5, 5.0).forEach { inc ->
                                            OutlinedButton(onClick = { statePrompts = statePrompts.map { if (it.exerciseId == prompt.exerciseId) it.copy(newWeightStr = OverloadPrompt.formatWeight(prompt.oldWeight + inc)) else it } }, modifier = Modifier.weight(1f), border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(themeColor)), contentPadding = PaddingValues(0.dp)) { Text("+$inc", fontSize = 10.sp, color = Color.White) }
                                        }
                                    }
                                    OutlinedTextField(value = prompt.newWeightStr, onValueChange = { v -> statePrompts = statePrompts.map { if (it.exerciseId == prompt.exerciseId) it.copy(newWeightStr = v) else it } }, label = { Text("Új Súly (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.padding(start = 48.dp, top = 8.dp).fillMaxWidth(), textStyle = TextStyle(color = Color.White))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onApply(statePrompts) }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Alkalmaz", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Mégse", color = themeColor) } }
    )
}

@Composable
fun RepRangeDialog(exerciseName: String, initialMin: Int, initialMax: Int, themeColor: Color, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var minI by remember { mutableStateOf(initialMin.toString()) }; var maxI by remember { mutableStateOf(initialMax.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Céltartomány: $exerciseName", color = Color.White) }, text = { Column { Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { OutlinedTextField(value = minI, onValueChange = { minI = it }, label = { Text("Min") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); OutlinedTextField(value = maxI, onValueChange = { maxI = it }, label = { Text("Max") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) } } }, confirmButton = { Button(onClick = { onSave(minI.toIntOrNull() ?: 8, maxI.toIntOrNull() ?: 12) }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Mentés", color = Color.White) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Mégse", color = themeColor) } })
}

@Composable
fun SettingsDialog(viewModel: WorkoutViewModel, currentThemeColor: Color, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Beállítások", color = Color.White) }, text = {
        Column { Text("Alkalmazás Témája:", fontWeight = FontWeight.Bold, color = Color.White); Spacer(modifier = Modifier.height(16.dp));
            listOf("Kék", "Piros", "Sárga", "Zöld").forEach { themeName ->
                val color = when(themeName) { "Piros" -> Color(0xFFFF453A); "Sárga" -> Color(0xFFFFD60A); "Zöld" -> Color(0xFF32D74B); else -> Color(0xFF0A84FF) };
                Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setTheme(themeName) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color)); Spacer(modifier = Modifier.width(16.dp)); Text(themeName, fontSize = 16.sp, color = Color.White) } } } },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = currentThemeColor)) { Text("Bezárás", color = Color.White) } }
    )
}

@Composable
fun PlateCalculatorDialog(onDismiss: () -> Unit, themeColor: Color) {
    var wI by remember { mutableStateOf("") }; var bW by remember { mutableStateOf("20") }
    val plates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25); val tW = wI.toDoubleOrNull() ?: 0.0; val b = bW.toDoubleOrNull() ?: 20.0; val pS = (tW - b) / 2.0; val needed = mutableMapOf<Double, Int>(); var rem = pS; if (rem > 0) { for (p in plates) { val c = (rem / p).toInt(); if (c > 0) { needed[p] = c; rem -= c * p } } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tányér Kalkulátor", color = Color.White) }, text = { Column { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = wI, onValueChange = { wI = it }, label = { Text("Célsúly (kg)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); OutlinedTextField(value = bW, onValueChange = { bW = it }, label = { Text("Rúd (kg)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }; Spacer(modifier = Modifier.height(16.dp)); if (pS > 0) { needed.forEach { (p, c) -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("$p kg tárcsa:", color = TextGray); Text("$c db", fontWeight = FontWeight.Bold, color = Color.White) } } } } }, confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Bezárás", color = Color.White) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseBlock(
    exercise: ExerciseSessionData, index: Int, total: Int, themeColor: Color,
    onMoveUp: () -> Unit, onMoveDown: () -> Unit,
    onSetUpdate: (WorkoutSetData) -> Unit, onSetCompleteToggle: (WorkoutSetData) -> Unit, onAddSet: () -> Unit, onDeleteSet: (Int) -> Unit, onUpdateRestTime: (Int) -> Unit, onToggleWarmup: (Int) -> Unit, onNoteUpdate: (String) -> Unit, onDeleteExercise: () -> Unit, onEditRepRange: () -> Unit
) {
    var showRest by remember { mutableStateOf(false) }; var showMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White)); Spacer(modifier = Modifier.width(12.dp));
                Text(text = exercise.name, color = themeColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.clickable { showRest = true }.background(SurfaceDark, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Timer, null, tint = TextGray, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("${exercise.restTimerDuration}s", color = TextGray, fontSize = 12.sp) }
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    Icon(Icons.Default.MoreVert, null, tint = TextGray, modifier = Modifier.clickable { showMenu = true }.padding(4.dp))
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceDark)) {
                        DropdownMenuItem(text = { Text("Move Up ↑", color = Color.White) }, onClick = { showMenu = false; onMoveUp() })
                        DropdownMenuItem(text = { Text("Move Down ↓", color = Color.White) }, onClick = { showMenu = false; onMoveDown() })
                        DropdownMenuItem(text = { Text("Rep Range", color = Color.White) }, onClick = { showMenu = false; onEditRepRange() })
                        DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = { showMenu = false; onDeleteExercise() })
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            if (exercise.note.isEmpty()) { Text("Add notes here...", color = TextGray.copy(alpha = 0.5f), fontSize = 12.sp) }
            BasicTextField(value = exercise.note, onValueChange = onNoteUpdate, textStyle = TextStyle(color = TextGray, fontSize = 12.sp), modifier = Modifier.fillMaxWidth(), cursorBrush = SolidColor(themeColor))
        }
        Spacer(modifier = Modifier.height(12.dp)); Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Text("SET", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.1f), textAlign = TextAlign.Center); Text("PREVIOUS", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center); Text("KG", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center); Text("REPS", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center); Text("RPE", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.15f), textAlign = TextAlign.Center); Text("✔", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.1f), textAlign = TextAlign.Center) }

        exercise.sets.forEach { set ->
            // --- ÚJ: SAJÁT, STABIL SWIPE-TO-REVEAL LOGIKA ---
            var isRevealed by remember { mutableStateOf(false) }
            var swipeOffset by remember { mutableFloatStateOf(0f) }
            val maxSwipe = -150f // Ennyi pixelre jön ki a kuka
            val animatedOffset by animateFloatAsState(targetValue = if (isRevealed) maxSwipe else 0f, label = "swipe")
            val finalOffset = if (swipeOffset != 0f) swipeOffset else animatedOffset

            key(set.id) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // HÁTTÉR: Piros kuka ikon
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Red)
                            .clickable { onDeleteSet(set.id) },
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(end = 24.dp))
                    }

                    // ELŐTÉR: Maga a széria
                    val rowBg = if (set.isCompleted) CompletedGreen.copy(alpha = 0.2f) else Color.Transparent
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(finalOffset.roundToInt(), 0) }
                            .fillMaxWidth()
                            .background(DarkBackground) // Eltakarja a piros hátteret
                            .background(rowBg)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { swipeOffset = if (isRevealed) maxSwipe else 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        // Csak balra engedjük húzni, maximum a kuka szélességéig
                                        swipeOffset = (swipeOffset + dragAmount).coerceIn(maxSwipe, 0f)
                                    },
                                    onDragEnd = {
                                        isRevealed = swipeOffset < maxSwipe / 2
                                        swipeOffset = 0f
                                    },
                                    onDragCancel = { swipeOffset = 0f }
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = set.setLabel, color = if (set.isWarmup) WarmupYellow else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.1f).clickable { onToggleWarmup(set.id) }, textAlign = TextAlign.Center)
                        Text(text = set.previousText, color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center, maxLines = 1)
                        Box(modifier = Modifier.weight(0.2f).padding(horizontal = 4.dp)) { CustomTextField(set.kg, { onSetUpdate(set.copy(kg = it)) }, set.isCompleted, themeColor) }
                        Box(modifier = Modifier.weight(0.2f).padding(horizontal = 4.dp)) { CustomTextField(set.reps, { onSetUpdate(set.copy(reps = it)) }, set.isCompleted, themeColor) }
                        Box(modifier = Modifier.weight(0.15f).padding(horizontal = 4.dp)) { CustomTextField(set.rpe, { onSetUpdate(set.copy(rpe = it)) }, set.isCompleted, themeColor) }
                        Box(modifier = Modifier.weight(0.1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(if (set.isCompleted) CompletedGreen else InputBackground).clickable { onSetCompleteToggle(set) }, contentAlignment = Alignment.Center) { if (set.isCompleted) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
        Text("+ Add Set", color = Color.White, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark).clickable { onAddSet() }.padding(vertical = 12.dp), textAlign = TextAlign.Center)
    }
    if (showRest) { var t by remember { mutableStateOf(exercise.restTimerDuration.toString()) }; AlertDialog(onDismissRequest = { showRest = false }, title = { Text("Pihenőidő (s)", color = Color.White) }, text = { OutlinedTextField(value = t, onValueChange = { t = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }, confirmButton = { Button(onClick = { onUpdateRestTime(t.toIntOrNull() ?: 90); showRest = false }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("OK", color = Color.White) } }) }
}

@Composable
fun StatItem(l: String, v: String) { Column { Text(l, color = TextGray, fontSize = 12.sp); Text(v, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
@Composable
fun TimerAdjustButton(t: String, onClick: () -> Unit) { Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.2f)).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text(t, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
@Composable
fun CustomTextField(v: String, onV: (String) -> Unit, comp: Boolean, theme: Color) { BasicTextField(value = v, onValueChange = onV, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), cursorBrush = SolidColor(theme), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (comp) Color.Transparent else InputBackground).padding(vertical = 8.dp)) }