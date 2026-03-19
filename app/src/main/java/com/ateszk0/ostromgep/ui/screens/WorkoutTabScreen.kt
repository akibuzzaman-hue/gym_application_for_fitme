package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import com.ateszk0.ostromgep.ui.components.SettingsDialog

@Composable
fun WorkoutTab(viewModel: WorkoutViewModel, themeColor: Color, onStart: () -> Unit) {
    val templates by viewModel.savedTemplates.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
            Text("Workout", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            IconButton(onClick = { showSettingsDialog = true }) { 
                Icon(Icons.Default.Settings, null, tint = TextGray) 
            } 
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.startEmptyWorkout(); onStart() }, 
            modifier = Modifier.fillMaxWidth().height(64.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
        ) { 
            Text("QUICK START", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) 
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("My Templates", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
            items(templates) { template -> 
                var showMenu by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.startWorkoutFromTemplate(template); onStart() }, 
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) { 
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
    if (showSettingsDialog) { 
        SettingsDialog(viewModel, themeColor) { showSettingsDialog = false } 
    }
}
