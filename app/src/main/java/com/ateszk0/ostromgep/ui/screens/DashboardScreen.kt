package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun DashboardProfile(viewModel: WorkoutViewModel, themeColor: Color, onNavigateToExercises: () -> Unit, onNavigateToCalendar: () -> Unit, onNavigateToStatistics: () -> Unit) {
    val history by viewModel.workoutHistory.collectAsState()
    val chartData = viewModel.getChartData()
    val username by viewModel.username.collectAsState()
    val profUri by viewModel.profilePictureUri.collectAsState()

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.updateProfilePictureUri(it.toString()) }
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { /* TODO settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(SurfaceDark).clickable { launcher.launch("image/*") }, 
                    contentAlignment = Alignment.Center
                ) { 
                    if (profUri.isNullOrEmpty()) {
                        Icon(Icons.Default.Person, null, tint = TextGray, modifier = Modifier.size(40.dp)) 
                    } else {
                        AsyncImage(
                            model = profUri,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column { 
                    BasicTextField(
                        value = username,
                        onValueChange = { viewModel.updateUsername(it) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White),
                        cursorBrush = SolidColor(themeColor)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) { 
                        Column { 
                            Text("Workouts", color = TextGray, fontSize = 12.sp)
                            Text("${history.size}", color = Color.White, fontWeight = FontWeight.Bold) 
                        }
                    } 
                } 
            }
            Spacer(modifier = Modifier.height(24.dp)) 
        }
        item { 
            Text("Utolsó edzések (Volumen)", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) { 
                chartData.forEach { heightRatio -> 
                    Box(modifier = Modifier.width(24.dp).fillMaxHeight(heightRatio).background(themeColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))) 
                } 
            }
            Spacer(modifier = Modifier.height(24.dp)) 
        }
        item { 
            Text("Dashboard", color = TextGray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                DashboardButton("Statistics", Icons.Default.BarChart, Modifier.weight(1f)) { onNavigateToStatistics() }
                DashboardButton("Exercises", Icons.Default.FitnessCenter, Modifier.weight(1f)) { onNavigateToExercises() } 
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                DashboardButton("Measures", Icons.Default.Accessibility, Modifier.weight(1f)) {}
                DashboardButton("Calendar", Icons.Default.CalendarMonth, Modifier.weight(1f)) { onNavigateToCalendar() } 
            }
            Spacer(modifier = Modifier.height(32.dp)) 
        }
        item { 
            Text("Workouts", color = TextGray)
            Spacer(modifier = Modifier.height(8.dp)) 
        }
        items(history.reversed().take(5)) { workout -> 
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) { 
                Column(modifier = Modifier.padding(16.dp)) { 
                    Text("Edzésnapló", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) { 
                        Column { 
                            Text("Time", color = TextGray, fontSize = 12.sp)
                            Text("${workout.durationSeconds/60}m", color = Color.White) 
                        }
                        Column { 
                            Text("Volume", color = TextGray, fontSize = 12.sp)
                            Text("${workout.totalVolume.toInt()} kg", color = Color.White) 
                        } 
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    workout.exercises.take(3).forEach { ex -> 
                        Text("${ex.sets.count { it.isCompleted }} sets ${ex.name}", color = TextGray, fontSize = 14.sp) 
                    } 
                } 
            } 
        }
    }
}

@Composable
fun DashboardButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(64.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SurfaceDark)) { 
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { 
            Icon(icon, null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold) 
        } 
    }
}
