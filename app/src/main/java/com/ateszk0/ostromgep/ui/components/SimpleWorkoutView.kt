package com.ateszk0.ostromgep.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.model.ExerciseSessionData
import com.ateszk0.ostromgep.model.WorkoutSetData
import com.ateszk0.ostromgep.ui.theme.*
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import androidx.compose.ui.res.stringResource
import com.ateszk0.ostromgep.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SimpleWorkoutView(
    viewModel: WorkoutViewModel,
    themeColor: Color,
    onRpeClick: (ExerciseSessionData, WorkoutSetData) -> Unit
) {
    val exercises by viewModel.activeExercises.collectAsState()
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val library by viewModel.exerciseLibrary.collectAsState()
    val haptic = LocalHapticFeedback.current

    var manualIndex by remember { mutableStateOf<Int?>(null) }
    
    // Auto find index: The first uncompleted exercise. If all completed, use the last one in the list.
    val targetIndex = manualIndex ?: exercises.indexOfFirst { ex ->
        ex.sets.any { !it.isCompleted }
    }.takeIf { it >= 0 } ?: (exercises.size - 1).coerceAtLeast(0)

    val exercise = exercises.getOrNull(targetIndex) ?: return

    val currentSet = exercise.sets.firstOrNull { !it.isCompleted } ?: exercise.sets.last()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Centered Image / Timer with Arrows
        Row(
            modifier = Modifier.fillMaxWidth().weight(1.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { manualIndex = (targetIndex - 1).coerceAtLeast(0) },
                enabled = targetIndex > 0
            ) {
                Icon(Icons.Default.ChevronLeft, null, tint = if (targetIndex > 0) Color.White else TextGray, modifier = Modifier.size(36.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = restTimerSeconds > 0,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "TimerImageTransition"
                ) { showTimer ->
                    if (showTimer) {
                        // Timer View
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(32.dp))
                                .background(themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "%02d:%02d".format(restTimerSeconds / 60, restTimerSeconds % 60),
                                color = Color.White,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TimerAdjustButton("-15") { viewModel.adjustRestTimer(-15) }
                                TimerAdjustButton("+15") { viewModel.adjustRestTimer(15) }
                            }
                        }
                    } else {
                        // Image View
                        val imgUri = library.find { it.name == exercise.name }?.imageUri
                        if (!imgUri.isNullOrEmpty()) {
                            coil.compose.AsyncImage(
                                model = imgUri,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(32.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(SurfaceDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FitnessCenter, null, tint = TextGray, modifier = Modifier.size(80.dp))
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = { manualIndex = (targetIndex + 1).coerceAtMost(exercises.size - 1) },
                enabled = targetIndex < exercises.size - 1
            ) {
                Icon(Icons.Default.ChevronRight, null, tint = if (targetIndex < exercises.size - 1) Color.White else TextGray, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))
        
        Text(
            text = exercise.name,
            color = themeColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.weight(0.2f))

        // Headers
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { 
            Text(stringResource(R.string.set_label_short), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(0.15f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.previous_label_short), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(0.25f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.kg_label_short), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.reps_label_short), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.rpe_label_short), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Current Set Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentSet.setLabel, 
                color = if (currentSet.isWarmup) WarmupYellow else Color.White, 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.weight(0.15f), 
                textAlign = TextAlign.Center
            )
            Text(
                text = currentSet.previousText, 
                color = TextGray, 
                fontSize = 14.sp, 
                modifier = Modifier.weight(0.25f), 
                textAlign = TextAlign.Center, 
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Box(modifier = Modifier.weight(0.2f).padding(horizontal = 4.dp)) { 
                CustomTextField(currentSet.kg, { viewModel.updateSet(exercise.id, currentSet.copy(kg = it)) }, currentSet.isCompleted, themeColor) 
            }
            Box(modifier = Modifier.weight(0.2f).padding(horizontal = 4.dp)) { 
                CustomTextField(currentSet.reps, { viewModel.updateSet(exercise.id, currentSet.copy(reps = it)) }, currentSet.isCompleted, themeColor) 
            }
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .padding(horizontal = 4.dp)
                    .height(48.dp)
                    .background(if (currentSet.isCompleted) Color.Transparent else SurfaceDark, RoundedCornerShape(8.dp))
                    .border(1.dp, if (currentSet.isCompleted) Color.Transparent else Color.DarkGray, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRpeClick(exercise, currentSet) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentSet.rpe.isBlank()) "-" else currentSet.rpe,
                    color = if (currentSet.isCompleted) TextGray else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Big Checkmark Button
        val isTimerRunning = restTimerSeconds > 0
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isTimerRunning) CompletedGreen else SurfaceDark)
                .clickable(enabled = !isTimerRunning) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleSetComplete(exercise.id, currentSet)
                    manualIndex = null // Allow auto advance
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, null, tint = if (isTimerRunning) Color.White else CompletedGreen, modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.weight(0.2f))
    }
}
