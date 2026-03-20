package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import com.ateszk0.ostromgep.ui.components.ExerciseEditDialog
import androidx.compose.ui.res.stringResource
import com.ateszk0.ostromgep.R

@Composable
fun ExercisesScreen(viewModel: WorkoutViewModel, themeColor: Color, onBack: () -> Unit) {
    val library by viewModel.exerciseLibrary.collectAsState()
    var search by remember { mutableStateOf("") }
    var showNewExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<ExerciseDef?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { 
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text(stringResource(R.string.exercises_btn), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = { showNewExerciseDialog = true }) { Text(stringResource(R.string.create_btn), color = themeColor) } 
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = search, 
            onValueChange = { search = it }, 
            label = { Text(stringResource(R.string.search_exercise_label)) }, 
            modifier = Modifier.fillMaxWidth(), 
            leadingIcon = { Icon(Icons.Default.Search, null) }, 
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn { 
            val filtered = library.filter { it.name.contains(search, ignoreCase = true) }.sortedBy { it.name }
            items(filtered) { exDef -> 
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { exerciseToEdit = exDef }.padding(vertical = 12.dp), 
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.SpaceBetween
                ) { 
                    Row(verticalAlignment = Alignment.CenterVertically) { 
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { 
                            if (!exDef.imageUri.isNullOrEmpty()) {
                                coil.compose.AsyncImage(
                                    model = exDef.imageUri,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.FitnessCenter, null, tint = TextGray) 
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column { 
                            Text(exDef.name, color = Color.White, fontSize = 18.sp)
                            Text(stringResource(R.string.target_reps_format, exDef.minReps, exDef.maxReps), color = TextGray, fontSize = 12.sp) 
                        } 
                    }
                    Icon(Icons.Default.Edit, null, tint = TextGray) 
                } 
            } 
        }
    }
    
    if (showNewExerciseDialog) { 
        var n by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewExerciseDialog = false }, 
            title = { Text(stringResource(R.string.custom_exercise_title)) }, 
            text = { OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text(stringResource(R.string.exercise_name_label)) }) }, 
            confirmButton = { 
                Button(onClick = { viewModel.createCustomExercise(n); showNewExerciseDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { 
                    Text(stringResource(R.string.add_btn), color = Color.White) 
                } 
            }, 
            dismissButton = { 
                TextButton(onClick = { showNewExerciseDialog = false }) { 
                    Text(stringResource(R.string.cancel_btn), color = themeColor) 
                } 
            }
        ) 
    }
    
    exerciseToEdit?.let { ex -> 
        ExerciseEditDialog(
            ex, themeColor, 
            { exerciseToEdit = null }, 
            { name, min, max, imgUri, muscles -> viewModel.updateExerciseDetails(name, min, max, imgUri, muscles); exerciseToEdit = null },
            if (ex.isCustom) { { viewModel.deleteCustomExercise(ex.name); exerciseToEdit = null } } else null
        ) 
    }
}
