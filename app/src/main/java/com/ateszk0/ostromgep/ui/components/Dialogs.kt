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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.ateszk0.ostromgep.model.OverloadPrompt
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.model.MuscleGroup
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*

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
        title = { Text("Progresszív Túlterhelés", fontWeight = FontWeight.Bold, color = themeColor) },
        text = {
            LazyColumn {
                item { Text("Javasolt változtatások a múltkori edzésed alapján:", color = TextGray, modifier = Modifier.padding(bottom = 16.dp)) }
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
                                    Text("🎯 Cél: +1 ismétlés (${prompt.newRepsStr} ism)", color = CompletedGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp)) 
                                } else {
                                    Text("🔥 Súlyemelés javasolt!", color = WarmupYellow, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp))
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
                                        label = { Text("Új Súly (kg)") }, 
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
        confirmButton = { Button(onClick = { onApply(statePrompts) }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Alkalmaz", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Mégse", color = themeColor) } }
    )
}

@Composable
fun ExerciseEditDialog(
    exercise: ExerciseDef, 
    themeColor: Color, 
    onDismiss: () -> Unit, 
    onSave: (String, Int, Int, String?, List<MuscleGroup>) -> Unit
) {
    var minI by remember { mutableStateOf(exercise.minReps.toString()) }
    var maxI by remember { mutableStateOf(exercise.maxReps.toString()) }
    var musc by remember { mutableStateOf(exercise.muscleGroups) }
    
    var imgUri by remember { mutableStateOf(exercise.imageUri) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { imgUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Edit: ${exercise.name}", color = Color.White) }, 
        text = { 
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) { 
                item {
                    Text("Rep Range", color = TextGray)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { 
                        OutlinedTextField(value = minI, onValueChange = { minI = it }, label = { Text("Min") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = maxI, onValueChange = { maxI = it }, label = { Text("Max") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) 
                    } 
                }
                item {
                    Text("Image", color = TextGray)
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
                                Text("No Img", color = TextGray, fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(if (imgUri.isNullOrEmpty()) "Pick Image" else "Change Image", color = Color.White)
                    }
                }
                item {
                    Text("Muscle Groups", color = TextGray)
                }
                items(MuscleGroup.values()) { mg ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                        musc = if (musc.contains(mg)) musc - mg else musc + mg 
                    }) {
                        Checkbox(checked = musc.contains(mg), onCheckedChange = { 
                            musc = if (it) musc + mg else musc - mg 
                        }, colors = CheckboxDefaults.colors(checkedColor = themeColor))
                        Text(mg.name, color = Color.White)
                    }
                }
            } 
        }, 
        confirmButton = { Button(onClick = { onSave(exercise.name, minI.toIntOrNull() ?: 8, maxI.toIntOrNull() ?: 12, imgUri, musc) }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Save", color = Color.White) } }, 
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = themeColor) } }
    )
}

@Composable
fun SettingsDialog(
    viewModel: WorkoutViewModel, 
    currentThemeColor: Color, 
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val username by viewModel.username.collectAsState()
    val profUri by viewModel.profilePictureUri.collectAsState()
    var editName by remember { mutableStateOf(username) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.updateProfilePictureUri(it.toString()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { 
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("Profil", color = if (selectedTab == 0) currentThemeColor else TextGray, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { selectedTab = 0 })
                Text("Téma", color = if (selectedTab == 1) currentThemeColor else TextGray, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.clickable { selectedTab = 1 })
            }
        }, 
        text = {
            if (selectedTab == 0) {
                Column {
                    Text("Profilkép:", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(SurfaceDark).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                        if (profUri.isNullOrEmpty()) {
                            Icon(Icons.Default.Person, null, tint = TextGray, modifier = Modifier.size(40.dp)) 
                        } else {
                            coil.compose.AsyncImage(model = profUri, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Neved:", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editName, 
                        onValueChange = { editName = it }, 
                        textStyle = TextStyle(color = Color.White),
                        singleLine = true
                    )
                }
            } else {
                Column { 
                    Text("Alkalmazás Témája:", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf("Kék", "Piros", "Sárga", "Zöld").forEach { themeName ->
                        val color = when(themeName) { "Piros" -> Color(0xFFFF453A); "Sárga" -> Color(0xFFFFD60A); "Zöld" -> Color(0xFF32D74B); else -> Color(0xFF0A84FF) }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setTheme(themeName) }.padding(vertical = 12.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) { 
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(themeName, fontSize = 16.sp, color = Color.White) 
                        } 
                    } 
                } 
            }
        },
        confirmButton = { 
            Button(onClick = { 
                if (selectedTab == 0) viewModel.updateUsername(editName)
                onDismiss() 
            }, colors = ButtonDefaults.buttonColors(containerColor = currentThemeColor)) { Text("Bezárás", color = Color.White) } 
        }
    )
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
        title = { Text("Tányér Kalkulátor", color = Color.White) }, 
        text = { 
            Column { 
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                    OutlinedTextField(value = wI, onValueChange = { wI = it }, label = { Text("Célsúly (kg)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = bW, onValueChange = { bW = it }, label = { Text("Rúd (kg)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) 
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (pS > 0) { 
                    needed.forEach { (p, c) -> 
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { 
                            Text("$p kg tárcsa:", color = TextGray)
                            Text("$c db", fontWeight = FontWeight.Bold, color = Color.White) 
                        } 
                    } 
                } 
            } 
        }, 
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Bezárás", color = Color.White) } }
    )
}
