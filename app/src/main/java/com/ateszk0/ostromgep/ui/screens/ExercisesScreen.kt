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
        OutlinedTextField(
            value = search, 
            onValueChange = { search = it }, 
            label = { Text("Search exercise") }, 
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
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceDark), contentAlignment = Alignment.Center) { 
                            Icon(Icons.Default.FitnessCenter, null, tint = TextGray) 
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column { 
                            Text(exDef.name, color = Color.White, fontSize = 18.sp)
                            Text("Target: ${exDef.minReps}-${exDef.maxReps} reps", color = TextGray, fontSize = 12.sp) 
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
            title = { Text("Saját gyakorlat") }, 
            text = { OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Gyakorlat neve") }) }, 
            confirmButton = { 
                Button(onClick = { viewModel.createCustomExercise(n); showNewExerciseDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { 
                    Text("Hozzáadás", color = Color.White) 
                } 
            }, 
            dismissButton = { 
                TextButton(onClick = { showNewExerciseDialog = false }) { 
                    Text("Mégse", color = themeColor) 
                } 
            }
        ) 
    }
    
    exerciseToEdit?.let { ex -> 
        ExerciseEditDialog(
            ex, themeColor, 
            { exerciseToEdit = null }, 
            { name, min, max, imgUri, muscles -> viewModel.updateExerciseDetails(name, min, max, imgUri, muscles); exerciseToEdit = null }
        ) 
    }
}
