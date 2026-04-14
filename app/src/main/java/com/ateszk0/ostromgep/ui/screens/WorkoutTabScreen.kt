package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import androidx.compose.foundation.border
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import com.ateszk0.ostromgep.R

@Composable
fun WorkoutTab(viewModel: WorkoutViewModel, themeColor: Color, onStart: () -> Unit, onNavigateToRoutineEditor: () -> Unit, onNavigateToExplore: () -> Unit) {
    val templates by viewModel.savedTemplates.collectAsState()
    var showFoldersDialog by remember { mutableStateOf(false) }
    
    if (showFoldersDialog) {
        com.ateszk0.ostromgep.ui.components.ManageFoldersDialog(
            viewModel = viewModel,
            themeColor = themeColor,
            onDismiss = { showFoldersDialog = false }
        )
    }
    
    val activeExercises by viewModel.activeExercises.collectAsState()
    var showOverrideConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    var templateToDelete by remember { mutableStateOf<Int?>(null) }
    var templateToShare by remember { mutableStateOf<com.ateszk0.ostromgep.model.WorkoutTemplate?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scanLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        com.journeyapps.barcodescanner.ScanContract()
    ) { result ->
        if (result.contents != null) {
            try {
                val decodedTemplate = com.google.gson.Gson().fromJson(result.contents, com.ateszk0.ostromgep.model.WorkoutTemplate::class.java)
                if (decodedTemplate.templateName != null && decodedTemplate.exercises.isNotEmpty()) {
                    viewModel.saveNewTemplate(decodedTemplate.templateName + " (Imported)", decodedTemplate.exercises)
                    android.widget.Toast.makeText(context, "Routine Imported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Invalid QR content", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
               android.widget.Toast.makeText(context, "Failed to parse routine QR", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showOverrideConfirm != null) {
        AlertDialog(
            onDismissRequest = { showOverrideConfirm = null },
            title = { Text(stringResource(R.string.discard_dialog_title), color = Color.White) },
            text = { Text(stringResource(R.string.new_workout_confirm_text), color = TextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showOverrideConfirm?.invoke()
                        showOverrideConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.continue_btn), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideConfirm = null }) {
                    Text(stringResource(R.string.cancel_btn), color = themeColor)
                }
            }
        )
    }

    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text(stringResource(R.string.delete_btn), color = Color.White) },
            text = { Text(stringResource(R.string.delete_routine_confirm_text), color = TextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTemplate(templateToDelete!!)
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.delete_btn), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text(stringResource(R.string.cancel_btn), color = themeColor)
                }
            }
        )
    }

    if (templateToShare != null) {
        val window = (context as? android.app.Activity)?.window
        DisposableEffect(templateToShare) {
            // Boost brightness to max when dialog appears
            val originalBrightness = window?.attributes?.screenBrightness
            if (window != null) {
                val layoutParams = window.attributes
                layoutParams.screenBrightness = 1.0f
                window.attributes = layoutParams
            }
            onDispose {
                // Restore brightness when dialog disappears
                if (window != null && originalBrightness != null) {
                    val layoutParams = window.attributes
                    layoutParams.screenBrightness = originalBrightness
                    window.attributes = layoutParams
                }
            }
        }
        
        var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        LaunchedEffect(templateToShare) {
            try {
                val json = com.google.gson.Gson().toJson(templateToShare)
                val barcodeEncoder = com.journeyapps.barcodescanner.BarcodeEncoder()
                qrBitmap = barcodeEncoder.encodeBitmap(json, com.google.zxing.BarcodeFormat.QR_CODE, 600, 600)
            } catch (e: Exception) { e.printStackTrace() }
        }
        
        androidx.compose.ui.window.Dialog(onDismissRequest = { templateToShare = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Share Routine", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("Scan this code on another device to import.", color = Color.Gray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(bottom = 16.dp))
                    
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(250.dp)
                        )
                    } else {
                        Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = themeColor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { templateToShare = null }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                        Text("Done", color = Color.White)
                    }
                }
            }
        }
    }

    val handleStartWorkout: (() -> Unit) -> Unit = { startAction ->
        if (activeExercises.isNotEmpty()) {
            showOverrideConfirm = startAction
        } else {
            startAction()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.workout_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .clickable { handleStartWorkout { viewModel.startEmptyWorkout(); onStart() } }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.start_empty_workout), fontSize = 16.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.routines_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row {
                IconButton(onClick = { 
                    val options = com.journeyapps.barcodescanner.ScanOptions().apply {
                        setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
                        setPrompt("Scan a Routine QR Code")
                        setBeepEnabled(false)
                        setBarcodeImageEnabled(false)
                        setOrientationLocked(false)
                    }
                    scanLauncher.launch(options)
                }) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = TextGray)
                }
                IconButton(onClick = { showFoldersDialog = true }) {
                    Icon(Icons.Default.Folder, contentDescription = "Folders", tint = TextGray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onNavigateToRoutineEditor() },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
            ) {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.new_routine), color = Color.White, fontSize = 14.sp)
            }
            Button(
                onClick = { onNavigateToExplore() },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.explore_routines), color = Color.White, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = TextGray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.my_routines_format, templates.size), color = TextGray, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { 
            items(templates) { template -> 
                var showMenu by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) { 
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                            Text(
                                template.templateName, 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box { 
                                Icon(Icons.Default.MoreVert, null, tint = TextGray, modifier = Modifier.clickable { showMenu = true }.padding(4.dp))
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceDark)) {
                                    DropdownMenuItem(text = { Text("Share via QR", color = Color.White) }, onClick = { showMenu = false; templateToShare = template })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.delete_btn), color = Color.Red) }, onClick = { showMenu = false; templateToDelete = template.id }) 
                                } 
                            } 
                        }
                        
                        val exNames = template.exercises.joinToString(", ") { it.name }
                        Text(
                            text = exNames,
                            color = TextGray,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Button(
                            onClick = { handleStartWorkout { viewModel.startWorkoutFromTemplate(template); onStart() } },
                            modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.start_routine_btn), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } 
            } 
        }
    }
}
