package com.ateszk0.ostromgep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.ateszk0.ostromgep.model.OverloadPrompt
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.model.Equipment
import com.ateszk0.ostromgep.model.MuscleGroup
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.ateszk0.ostromgep.R
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun ProgressiveOverloadDialog(
    initialPrompts: List<OverloadPrompt>,
    themeColor: Color,
    onApply: (List<OverloadPrompt>) -> Unit,
    onDismiss: () -> Unit
) {
    var statePrompts by remember { mutableStateOf(initialPrompts) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark),
        title = { Text(stringResource(R.string.progressive_overload_title), fontWeight = FontWeight.Bold, color = themeColor) },
        text = {
            LazyColumn {
                item { Text(stringResource(R.string.progressive_overload_text), color = TextGray, modifier = Modifier.padding(bottom = 16.dp)) }
                items(statePrompts) { prompt ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = DarkBackground)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { 
                                Checkbox(
                                    checked = prompt.isSelected, 
                                    onCheckedChange = { chk -> statePrompts = statePrompts.map { if (it.exerciseId == prompt.exerciseId) it.copy(isSelected = chk) else it } }, 
                                    colors = CheckboxDefaults.colors(checkedColor = themeColor)
                                )
                                Text(prompt.name, fontWeight = FontWeight.Bold, color = Color.White) 
                            }
                            if (prompt.isSelected) {
                                if (!prompt.requiresWeightIncrease) { 
                                    Text(stringResource(R.string.target_plus_one) + " (${prompt.newRepsStr})", color = CompletedGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp)) 
                                } else {
                                    Text(stringResource(R.string.weight_increase_suggested), color = WarmupYellow, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp))
                                    Row(modifier = Modifier.padding(start = 48.dp, top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(0.5, 1.0, 2.5, 5.0).forEach { inc ->
                                            OutlinedButton(
                                                onClick = { statePrompts = statePrompts.map { if (it.exerciseId == prompt.exerciseId) it.copy(newWeightStr = OverloadPrompt.formatWeight(prompt.oldWeight + inc)) else it } }, 
                                                modifier = Modifier.weight(1f), 
                                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(themeColor)), 
                                                contentPadding = PaddingValues(0.dp)
                                            ) { 
                                                Text("+$inc", fontSize = 10.sp, color = Color.White) 
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = prompt.newWeightStr, 
                                        onValueChange = { v -> statePrompts = statePrompts.map { if (it.exerciseId == prompt.exerciseId) it.copy(newWeightStr = v) else it } }, 
                                        label = { Text(stringResource(R.string.new_weight_label)) }, 
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                        modifier = Modifier.padding(start = 48.dp, top = 8.dp).fillMaxWidth(), 
                                        textStyle = TextStyle(color = Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onApply(statePrompts) }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text(stringResource(R.string.apply_btn), color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn), color = themeColor) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditDialog(
    exercise: ExerciseDef, 
    themeColor: Color, 
    onDismiss: () -> Unit, 
    onSave: (String, Int, Int, String?, List<MuscleGroup>, Equipment) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    @Suppress("SENSELESS_COMPARISON")
    val safeMuscles = if (exercise.muscleGroups != null) exercise.muscleGroups.filterNotNull() else emptyList()
    @Suppress("SENSELESS_COMPARISON")
    val safeEquipment = if (exercise.equipment != null) exercise.equipment else Equipment.NONE
    @Suppress("SENSELESS_COMPARISON")
    val safeMinReps = if (exercise.minReps != null && exercise.minReps > 0) exercise.minReps else 8
    @Suppress("SENSELESS_COMPARISON")
    val safeMaxReps = if (exercise.maxReps != null && exercise.maxReps > 0) exercise.maxReps else 12
    var minI by remember { mutableStateOf(safeMinReps.toString()) }
    var maxI by remember { mutableStateOf(safeMaxReps.toString()) }
    var musc by remember { mutableStateOf(safeMuscles) }
    var equip by remember { mutableStateOf<Equipment>(safeEquipment) }
    
    var imgUri by remember { mutableStateOf(exercise.imageUri) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { imgUri = it.toString() }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
        exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.zIndex(20f)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            visible = false
                            onDismiss() 
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeColor)
                        }
                        Text(stringResource(R.string.edit_label) + " ${exercise.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = TextAlign.Center, maxLines = 1)
                    Text(
                        stringResource(R.string.save_btn), 
                        color = if (musc.isNotEmpty()) themeColor else TextGray, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = musc.isNotEmpty()) {
                            onSave(exercise.name, minI.toIntOrNull() ?: 8, maxI.toIntOrNull() ?: 12, imgUri, musc, equip)
                        }
                    )
                }
                if (exercise.isCustom && onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.delete_btn), color = Color.Red) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) { 
                    item {
                        Text(stringResource(R.string.rep_range_label), color = TextGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { 
                            OutlinedTextField(value = minI, onValueChange = { minI = it }, label = { Text(stringResource(R.string.min_label)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = Color.White))
                            OutlinedTextField(value = maxI, onValueChange = { maxI = it }, label = { Text(stringResource(R.string.max_label)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = Color.White)) 
                        } 
                    }
                    if (exercise.isCustom) {
                        item {
                            Text(stringResource(R.string.image_label), color = TextGray)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { launcher.launch("image/*") }) {
                                if (!imgUri.isNullOrEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = imgUri,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(DarkBackground), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.no_img_label), color = TextGray, fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(if (imgUri.isNullOrEmpty()) stringResource(R.string.pick_image) else stringResource(R.string.change_image), color = Color.White)
                            }
                        }
                    }
                    item {
                        Text(stringResource(R.string.muscle_groups_label), color = TextGray)
                    }
                    if (exercise.isCustom) {
                        items(MuscleGroup.values().toList()) { mg ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                                musc = if (musc.contains(mg)) musc - mg else musc + mg 
                            }) {
                                Checkbox(checked = musc.contains(mg), onCheckedChange = { 
                                    musc = if (it) musc + mg else musc - mg 
                                }, colors = CheckboxDefaults.colors(checkedColor = themeColor))
                                Spacer(modifier = Modifier.width(8.dp))
                                MuscleGroupIcon(muscleGroup = mg)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(mg.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }.replace("_", " "), color = Color.White)
                            }
                        }
                        item {
                            Text("Equipment", color = TextGray)
                        }
                        items(com.ateszk0.ostromgep.model.Equipment.values().toList()) { eq ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                                equip = eq
                            }) {
                                RadioButton(selected = equip == eq, onClick = { equip = eq }, colors = RadioButtonDefaults.colors(selectedColor = themeColor))
                                Spacer(modifier = Modifier.width(8.dp))
                                EquipmentIcon(equipment = eq)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(eq.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }.replace("_", " "), color = Color.White)
                            }
                        }
                    } else {
                        item {
                            val muscleText = safeMuscles.joinToString(", ") { it.name }
                            Text(if (muscleText.isEmpty()) "None" else muscleText, color = Color.White, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                            Text(stringResource(R.string.default_muscle_warning), color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                        }
                    }
                } 
            }
            }
        }
    }
}

@Composable
fun CreateExerciseDialog(
    themeColor: Color, 
    onDismiss: () -> Unit, 
    onSave: (String, Int, Int, String?, List<MuscleGroup>, Equipment) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var minI by remember { mutableStateOf("8") }
    var maxI by remember { mutableStateOf("12") }
    var musc by remember { mutableStateOf(emptyList<MuscleGroup>()) }
    var equip by remember { mutableStateOf<Equipment?>(null) }
    
    var imgUri by remember { mutableStateOf<String?>(null) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { imgUri = it.toString() }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
        exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.zIndex(20f)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            visible = false
                            onDismiss() 
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeColor)
                        }
                        Text(stringResource(R.string.custom_exercise_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
                    val e = equip
                    val canSave = name.isNotBlank() && musc.isNotEmpty() && e != null
                    Text(
                        stringResource(R.string.save_btn), 
                        color = if (canSave) themeColor else TextGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = canSave) {
                            onSave(name.trim(), minI.toIntOrNull() ?: 8, maxI.toIntOrNull() ?: 12, imgUri, musc, e!!)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) { 
                    item {
                        OutlinedTextField(
                            value = name, 
                            onValueChange = { name = it }, 
                            label = { Text(stringResource(R.string.exercise_name_label)) }, 
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White),
                            singleLine = true
                        )
                    }
                    item {
                        Text(stringResource(R.string.rep_range_label), color = TextGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { 
                            OutlinedTextField(value = minI, onValueChange = { minI = it }, label = { Text(stringResource(R.string.min_label)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = Color.White))
                            OutlinedTextField(value = maxI, onValueChange = { maxI = it }, label = { Text(stringResource(R.string.max_label)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = Color.White)) 
                        } 
                    }
                    item {
                        Text(stringResource(R.string.image_label), color = TextGray)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { launcher.launch("image/*") }) {
                            if (!imgUri.isNullOrEmpty()) {
                                coil.compose.AsyncImage(
                                    model = imgUri,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(DarkBackground), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.no_img_label), color = TextGray, fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(if (imgUri.isNullOrEmpty()) stringResource(R.string.pick_image) else stringResource(R.string.change_image), color = Color.White)
                        }
                    }
                    item {
                        Text(stringResource(R.string.muscle_groups_label), color = TextGray)
                    }
                    items(MuscleGroup.values().toList()) { mg ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                            musc = if (musc.contains(mg)) musc - mg else musc + mg 
                        }) {
                            Checkbox(checked = musc.contains(mg), onCheckedChange = { 
                                musc = if (it) musc + mg else musc - mg 
                            }, colors = CheckboxDefaults.colors(checkedColor = themeColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            MuscleGroupIcon(muscleGroup = mg)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(mg.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }.replace("_", " "), color = Color.White)
                        }
                    }
                    item {
                        Text("Equipment", color = TextGray)
                    }
                    items(com.ateszk0.ostromgep.model.Equipment.values().toList()) { eq ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                            equip = eq
                        }) {
                            RadioButton(selected = equip == eq, onClick = { equip = eq }, colors = RadioButtonDefaults.colors(selectedColor = themeColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            EquipmentIcon(equipment = eq)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(eq.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }.replace("_", " "), color = Color.White)
                        }
                    }
                } 
            }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    viewModel: WorkoutViewModel, 
    currentThemeColor: Color, 
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var selectedTab by remember { mutableStateOf(0) }
    
    val username by viewModel.username.collectAsState()
    val profUri by viewModel.profilePictureUri.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    var editName by remember(username) { mutableStateOf(username) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.updateProfilePictureUri(it.toString()) }
    }
    
    val csvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val success = viewModel.importCsvHistory(context, it)
            android.widget.Toast.makeText(context, if (success) "Sikeres importálás" else "Sikertelen importálás", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
        exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            val coroutineScope = rememberCoroutineScope()
            Column(modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 16.dp, end = 16.dp)) {
                // Header
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.close_btn), color = currentThemeColor, modifier = Modifier.clickable { 
                        viewModel.updateUsername(editName)
                        coroutineScope.launch {
                            visible = false
                            kotlinx.coroutines.delay(300)
                            onDismiss() 
                        }
                    }, fontSize = 16.sp)
                    Text(stringResource(R.string.profile_settings_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(48.dp)) // To center title
                }
                
                // Content
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        // Profile Picture
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(SurfaceDark).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                                if (profUri.isNullOrEmpty()) {
                                    Icon(Icons.Default.Person, null, tint = TextGray, modifier = Modifier.size(60.dp)) 
                                } else {
                                    coil.compose.AsyncImage(model = profUri, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.profile_picture), color = TextGray, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        // Name
                        Text(stringResource(R.string.name_label), fontWeight = FontWeight.Bold, color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(
                            value = editName, 
                            onValueChange = { editName = it }, 
                            textStyle = TextStyle(color = Color.White),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = currentThemeColor, focusedContainerColor = SurfaceDark, unfocusedContainerColor = SurfaceDark)
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(stringResource(R.string.theme_label), fontWeight = FontWeight.Bold, color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceDark)) {
                            listOf("Piros", "Sárga", "Zöld", "Kék", "Lila").forEachIndexed { index, themeName ->
                                val themeDisplay = when(themeName) { "Piros" -> stringResource(R.string.theme_red); "Sárga" -> stringResource(R.string.theme_yellow); "Zöld" -> stringResource(R.string.theme_green); "Lila" -> stringResource(R.string.theme_purple); else -> stringResource(R.string.theme_blue) }
                                val color = when(themeName) { "Piros" -> Color(0xFFFF453A); "Sárga" -> Color(0xFFFFD60A); "Zöld" -> Color(0xFF32D74B); "Lila" -> Color(0xFFAF52DE); else -> Color(0xFF0A84FF) }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setTheme(themeName) }.padding(16.dp), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) { 
                                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(themeDisplay, fontSize = 16.sp, color = Color.White) 
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (themeName == "Piros" && currentThemeColor == Color(0xFFFF453A)) { Icon(Icons.Default.Check, null, tint = currentThemeColor) }
                                    if (themeName == "Sárga" && currentThemeColor == Color(0xFFFFD60A)) { Icon(Icons.Default.Check, null, tint = currentThemeColor) }
                                    if (themeName == "Zöld" && currentThemeColor == Color(0xFF32D74B)) { Icon(Icons.Default.Check, null, tint = currentThemeColor) }
                                    if (themeName == "Kék" && currentThemeColor == Color(0xFF0A84FF)) { Icon(Icons.Default.Check, null, tint = currentThemeColor) }
                                    if (themeName == "Lila" && currentThemeColor == Color(0xFFAF52DE)) { Icon(Icons.Default.Check, null, tint = currentThemeColor) }
                                } 
                                if (index < 4) Divider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            } 
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.language_label), fontWeight = FontWeight.Bold, color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceDark)) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.setLanguage("en")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    context.getSystemService(android.app.LocaleManager::class.java).applicationLocales = android.os.LocaleList(java.util.Locale("en"))
                                }
                            }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("English", fontSize = 16.sp, color = Color.White)
                                Spacer(modifier = Modifier.weight(1f))
                                if (currentLang == "en") Icon(Icons.Default.Check, null, tint = currentThemeColor)
                            }
                            Divider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            Row(modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.setLanguage("hu")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    context.getSystemService(android.app.LocaleManager::class.java).applicationLocales = android.os.LocaleList(java.util.Locale("hu"))
                                }
                            }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Magyar", fontSize = 16.sp, color = Color.White)
                                Spacer(modifier = Modifier.weight(1f))
                                if (currentLang == "hu") Icon(Icons.Default.Check, null, tint = currentThemeColor)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("AI (Gemini API Key)", fontWeight = FontWeight.Bold, color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        val geminiKey by viewModel.geminiApiKey.collectAsState()
                        var showApiEdit by remember { mutableStateOf(false) }
                        var tempKey by remember { mutableStateOf(geminiKey ?: "") }
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceDark)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { tempKey = geminiKey ?: ""; showApiEdit = !showApiEdit }.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VpnKey, null, tint = if (!geminiKey.isNullOrBlank()) Color(0xFF32D74B) else TextGray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (!geminiKey.isNullOrBlank()) "API Key: Set ✓" else "API Key: Not Set", fontSize = 15.sp, color = Color.White)
                            }
                            if (showApiEdit) {
                                Divider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                Column(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = tempKey,
                                        onValueChange = { tempKey = it },
                                        label = { Text("Gemini API Key") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = currentThemeColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedLabelColor = TextGray, focusedLabelColor = currentThemeColor)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { viewModel.saveGeminiApiKey(tempKey); showApiEdit = false }, colors = ButtonDefaults.buttonColors(containerColor = currentThemeColor), modifier = Modifier.fillMaxWidth()) {
                                        Text("Save Key", color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Data", fontWeight = FontWeight.Bold, color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
                        ) { uri: android.net.Uri? ->
                            uri?.let {
                                val success = viewModel.exportWorkoutsAsCsv(context, it)
                                android.widget.Toast.makeText(context, if (success) context.getString(R.string.export_success) else context.getString(R.string.export_failure), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceDark)) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { csvLauncher.launch("*/*") }.padding(16.dp)) {
                                Text("Import CSV", fontSize = 16.sp, color = Color.White)
                            }
                            Divider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            Row(modifier = Modifier.fillMaxWidth().clickable { exportLauncher.launch("workouts.csv") }.padding(16.dp)) {
                                Text(stringResource(R.string.export_workouts_csv), fontSize = 16.sp, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PlateCalculatorDialog(
    onDismiss: () -> Unit, 
    themeColor: Color
) {
    var wI by remember { mutableStateOf("") }
    var bW by remember { mutableStateOf("20") }
    val plates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val tW = wI.toDoubleOrNull() ?: 0.0
    val b = bW.toDoubleOrNull() ?: 20.0
    val pS = (tW - b) / 2.0
    val needed = mutableMapOf<Double, Int>()
    var rem = pS
    
    if (rem > 0) { 
        for (p in plates) { 
            val c = (rem / p).toInt()
            if (c > 0) { needed[p] = c; rem -= c * p } 
        } 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text(stringResource(R.string.plate_calc_title), color = Color.White) }, 
        text = { 
            Column { 
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                    OutlinedTextField(value = wI, onValueChange = { wI = it }, label = { Text(stringResource(R.string.target_weight_label)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = bW, onValueChange = { bW = it }, label = { Text(stringResource(R.string.barbell_weight_label)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) 
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (pS > 0) { 
                    needed.forEach { (p, c) -> 
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { 
                            Text(stringResource(R.string.plate_format, p.toString()), color = TextGray)
                            Text("$c " + stringResource(R.string.pcs_label), fontWeight = FontWeight.Bold, color = Color.White) 
                        } 
                    } 
                } 
            } 
        }, 
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text(stringResource(R.string.close_btn), color = Color.White) } }
    )
}

@Composable
fun ManageFoldersDialog(
    viewModel: WorkoutViewModel,
    themeColor: Color,
    onDismiss: () -> Unit
) {
    var isCreating by remember { mutableStateOf(false) }
    
    if (isCreating) {
        CreateFolderDialog(viewModel, themeColor, onDismiss = { isCreating = false })
    } else {
        val folders by viewModel.routineFolders.collectAsState()
        val activeId by viewModel.activeFolderId.collectAsState()
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.manage_folders_title), color = Color.White) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders) { folder ->
                        val isActive = folder.id == activeId
                        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setActiveFolder(if (isActive) null else folder.id) }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(folder.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.folder_routines_count, folder.templateIds.size), color = TextGray, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActive) {
                                    Icon(Icons.Default.CheckCircle, null, tint = themeColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                IconButton(onClick = { viewModel.deleteFolder(folder.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                                }
                            }
                        }
                    }
                    if (folders.isEmpty()) {
                        item { Text(stringResource(R.string.no_folders_yet), color = TextGray) }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { isCreating = true }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                    Text(stringResource(R.string.new_folder_btn), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_btn), color = themeColor) }
            }
        )
    }
}

@Composable
fun CreateFolderDialog(
    viewModel: WorkoutViewModel,
    themeColor: Color,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTemplates by remember { mutableStateOf(setOf<Int>()) }
    val templates by viewModel.savedTemplates.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_folder_title), color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text(stringResource(R.string.folder_name_label)) }, 
                    textStyle = TextStyle(color = Color.White),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(templates) { template ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            selectedTemplates = if (selectedTemplates.contains(template.id)) selectedTemplates - template.id else selectedTemplates + template.id
                        }) {
                            Checkbox(checked = selectedTemplates.contains(template.id), onCheckedChange = { chk ->
                                selectedTemplates = if (chk) selectedTemplates + template.id else selectedTemplates - template.id
                            }, colors = CheckboxDefaults.colors(checkedColor = themeColor))
                            Text(template.templateName, color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (name.isNotBlank()) {
                    viewModel.createFolder(name, selectedTemplates.toList())
                    onDismiss()
                }
                             }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                Text(stringResource(R.string.save_btn), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn), color = themeColor) }
        }
    )
}

@Composable
fun WorkoutWrapUpDialog(
    summary: com.ateszk0.ostromgep.model.WorkoutSummaryData,
    themeColor: Color,
    onDismiss: () -> Unit
) {
    val mins = summary.durationSeconds / 60
    val secs = summary.durationSeconds % 60

    // Fun volume comparisons
    val funFact = when {
        summary.totalVolumeKg > 10000 -> "That's heavier than an African elephant! 🐘"
        summary.totalVolumeKg > 5000 -> "You lifted the weight of a car! 🚗"
        summary.totalVolumeKg > 1000 -> "You lifted a grand piano's worth! 🎹"
        summary.totalVolumeKg > 500 -> "That's a motorbike worth of weight! 🏍️"
        else -> "Keep grinding — every kg counts! 💪"
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .padding(24.dp)
        ) {
            // Header
            Text("🏆 Workout Complete!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = androidx.compose.ui.Modifier.height(18.dp))
            Divider(color = Color.DarkGray, thickness = 0.5.dp)
            Spacer(modifier = androidx.compose.ui.Modifier.height(14.dp))

            // Stats grid
            androidx.compose.foundation.layout.Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WrapUpStat("⏱ Duration", if (mins > 0) "${mins}m ${secs}s" else "${secs}s", themeColor)
                WrapUpStat("🏋️ Volume", "${String.format("%.0f", summary.totalVolumeKg)} kg", themeColor)
            }
            Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
            androidx.compose.foundation.layout.Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WrapUpStat("📦 Sets", "${summary.totalSets}", themeColor)
                WrapUpStat("🔁 Reps", "${summary.totalReps}", themeColor)
            }

            // New PRs
            if (summary.newPersonalRecords.isNotEmpty()) {
                Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                Text("🎯 New Personal Records", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
                summary.newPersonalRecords.forEach { pr ->
                    Text("  • $pr", color = Color.White, fontSize = 13.sp)
                }
            }

            // Muscles
            if (summary.muscleGroups.isNotEmpty()) {
                Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                Text("💪 Muscles Trained", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
                Text(summary.muscleGroups.joinToString(" · "), color = TextGray, fontSize = 13.sp)
            }

            // Fun fact
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBackground)
                    .padding(12.dp)
            ) {
                Text(funFact, color = TextGray, fontSize = 13.sp)
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Let's go! 🚀", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WrapUpStat(label: String, value: String, themeColor: Color) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = androidx.compose.ui.Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(value, color = themeColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextGray, fontSize = 12.sp)
    }
}
