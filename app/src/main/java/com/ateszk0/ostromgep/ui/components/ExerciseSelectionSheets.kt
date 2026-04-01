package com.ateszk0.ostromgep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.zIndex
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.model.Equipment
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.model.MuscleGroup
import com.ateszk0.ostromgep.utils.calculateSearchScore
import com.ateszk0.ostromgep.ui.theme.DarkBackground
import com.ateszk0.ostromgep.ui.theme.SurfaceDark
import com.ateszk0.ostromgep.ui.theme.TextGray
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseContent(
    library: List<ExerciseDef>,
    recentExercises: List<ExerciseDef> = emptyList(),
    onExerciseSelected: (ExerciseDef) -> Unit,
    onClose: () -> Unit,
    onCreateCustom: () -> Unit = {},
    themeColor: Color
) {
    var search by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf<Equipment?>(null) }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }
    
    var showEquipments by remember { mutableStateOf(false) }
    var showMuscles by remember { mutableStateOf(false) }

    val filteredLibrary = library.filter { exercise ->
        (selectedEquipment == null || exercise.equipment == selectedEquipment) &&
        (selectedMuscle == null || exercise.muscleGroups.contains(selectedMuscle)) &&
        (search.isBlank() || exercise.calculateSearchScore(search) > 0)
    }.sortedWith(
        compareByDescending<ExerciseDef> { if (search.isBlank()) 0 else it.calculateSearchScore(search) }
            .thenBy { it.name }
    )

    Column(modifier = Modifier.fillMaxHeight(0.95f).background(Color.Black)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Cancel", color = themeColor, modifier = Modifier.clickable { onClose() }, fontSize = 16.sp)
            Text("Add Exercise", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Create", color = themeColor, modifier = Modifier.clickable { onCreateCustom() }, fontSize = 16.sp)
        }
        
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search exercise", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextGray) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = themeColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterButton(
                text = if (selectedEquipment == null) "All Equipment" else selectedEquipment!!.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }.replace("_", " "),
                modifier = Modifier.weight(1f),
                onClick = { showEquipments = true }
            )
            FilterButton(
                text = if (selectedMuscle == null) "All Muscles" else selectedMuscle!!.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }.replace("_", " "),
                modifier = Modifier.weight(1f),
                onClick = { showMuscles = true }
            )
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (search.isBlank() && selectedEquipment == null && selectedMuscle == null && recentExercises.isNotEmpty()) {
                item {
                    Text("Recent Exercises", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(recentExercises) { exercise ->
                    ExerciseListItem(exercise = exercise, onClick = { onExerciseSelected(exercise) })
                }
            }
            
            items(filteredLibrary) { exercise ->
                ExerciseListItem(exercise = exercise, onClick = { onExerciseSelected(exercise) })
            }
        }
    }

    if (showEquipments) {
        AlertDialog(
            onDismissRequest = { showEquipments = false },
            title = { Text("Equipment", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item {
                        SelectionItem(
                            text = "All Equipment",
                            isSelected = selectedEquipment == null,
                            themeColor = themeColor,
                            icon = { 
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Search, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                                }
                            },
                            onClick = { selectedEquipment = null; showEquipments = false }
                        )
                    }
                    items(Equipment.values().toList()) { eq ->
                        SelectionItem(
                            text = eq.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }.replace("_", " "),
                            isSelected = selectedEquipment == eq,
                            themeColor = themeColor,
                            icon = { EquipmentIcon(equipment = eq) },
                            onClick = { selectedEquipment = eq; showEquipments = false }
                        )
                    }
                }
            },
            confirmButton = {},
            containerColor = SurfaceDark
        )
    }

    if (showMuscles) {
        AlertDialog(
            onDismissRequest = { showMuscles = false },
            title = { Text("Muscle Group", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item {
                        SelectionItem(
                            text = "All Muscles",
                            isSelected = selectedMuscle == null,
                            themeColor = themeColor,
                            icon = { 
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Search, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                                }
                            },
                            onClick = { selectedMuscle = null; showMuscles = false }
                        )
                    }
                    items(MuscleGroup.values().toList()) { mg ->
                        SelectionItem(
                            text = mg.name.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }.replace("_", " "),
                            isSelected = selectedMuscle == mg,
                            themeColor = themeColor,
                            icon = { MuscleGroupIcon(muscleGroup = mg) },
                            onClick = { selectedMuscle = mg; showMuscles = false }
                        )
                    }
                }
            },
            confirmButton = {},
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun FilterButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ExerciseListItem(exercise: ExerciseDef, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!exercise.imageUri.isNullOrEmpty()) {
             Box(
                 modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White),
                 contentAlignment = Alignment.Center
             ) {
                 coil.compose.AsyncImage(
                    model = exercise.imageUri,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                 )
             }
        } else {
             MuscleGroupIcon(muscleGroup = exercise.muscleGroups.firstOrNull() ?: MuscleGroup.OTHER)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, color = Color.White, fontSize = 16.sp)
            val muscleText = exercise.muscleGroups.firstOrNull()?.name?.lowercase(Locale.ROOT)?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }?.replace("_", " ") ?: "Other"
            Text(muscleText, color = TextGray, fontSize = 14.sp)
        }
    }
}

@Composable
fun SelectionItem(text: String, isSelected: Boolean, themeColor: Color, icon: @Composable () -> Unit = {}, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, color = Color.White, fontSize = 16.sp)
        }
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = themeColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RpeSelectionSheet(
    themeColor: Color,
    currentRpe: String,
    onRpeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val rpeOptions = listOf(
        "10" to "Maximal effort. No more reps.",
        "9.5" to "No more reps, could do more weight.",
        "9" to "Could do 1 more rep.",
        "8.5" to "Could definitively do 1 more rep.",
        "8" to "Could do 2 more reps.",
        "7.5" to "Could definitively do 2 more reps.",
        "7" to "Could do 3 more reps.",
        "6.5" to "Could definitively do 3 more reps.",
        "6" to "Could do 4+ more reps."
    )

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
        exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxSize().zIndex(10f)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            visible = false
                            onDismiss() 
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeColor)
                        }
                        Text("Select RPE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                    Text("Rate of Perceived Exertion", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp, start = 8.dp))
                    
                    LazyColumn {
                    items(rpeOptions) { (rpe, desc) ->
                        val isSelected = currentRpe == rpe
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onRpeSelected(rpe)
                                }
                                .background(if (isSelected) themeColor.copy(alpha = 0.2f) else Color.Transparent)
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(if (isSelected) themeColor else SurfaceDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(rpe, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(desc, color = if (isSelected) Color.White else TextGray, fontSize = 16.sp)
                        }
                    }
                }
            }
            }
        }
    }
}
