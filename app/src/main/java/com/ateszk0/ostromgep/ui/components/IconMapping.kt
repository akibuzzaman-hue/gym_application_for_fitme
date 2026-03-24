package com.ateszk0.ostromgep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ateszk0.ostromgep.model.Equipment
import com.ateszk0.ostromgep.model.MuscleGroup

val Equipment.iconList: ImageVector
    get() = when (this) {
        Equipment.NONE -> Icons.Default.AccessibilityNew
        Equipment.BARBELL -> Icons.Default.FitnessCenter
        Equipment.DUMBBELL -> Icons.Default.FitnessCenter
        Equipment.KETTLEBELL -> Icons.Default.FitnessCenter
        Equipment.MACHINE -> Icons.Default.Build
        Equipment.PLATE -> Icons.Default.DonutLarge
        Equipment.RESISTANCE_BAND -> Icons.Default.LinearScale
        Equipment.SUSPENSION_BAND -> Icons.Default.Link
        Equipment.OTHER -> Icons.Default.MoreHoriz
    }

val MuscleGroup.iconList: ImageVector
    get() = when (this) {
        MuscleGroup.ABDOMINALS -> Icons.Default.ViewModule
        MuscleGroup.ABDUCTORS, MuscleGroup.ADDUCTORS -> Icons.Default.SwapHoriz
        MuscleGroup.BICEPS, MuscleGroup.TRICEPS -> Icons.Default.FitnessCenter
        MuscleGroup.CALVES, MuscleGroup.HAMSTRINGS, MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES -> Icons.Default.DirectionsRun
        MuscleGroup.CARDIO -> Icons.Default.FavoriteBorder
        MuscleGroup.CHEST -> Icons.Default.MonitorWeight
        MuscleGroup.FOREARMS -> Icons.Default.PanTool
        MuscleGroup.LATS, MuscleGroup.LOWER_BACK, MuscleGroup.UPPER_BACK -> Icons.Default.Height
        MuscleGroup.NECK -> Icons.Default.Face
        MuscleGroup.SHOULDERS -> Icons.Default.OpenWith
        MuscleGroup.TRAPS -> Icons.Default.ArrowDropUp
        MuscleGroup.OTHER -> Icons.Default.MoreHoriz
    }

@Composable
fun ThemedIcon(
    imageVector: ImageVector,
    backgroundTint: Color = Color.White,
    iconTint: Color = Color.Black
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundTint),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun EquipmentIcon(equipment: Equipment, backgroundTint: Color = Color.White) {
    ThemedIcon(imageVector = equipment.iconList, backgroundTint = backgroundTint)
}

@Composable
fun MuscleGroupIcon(muscleGroup: MuscleGroup, backgroundTint: Color = Color.White) {
    ThemedIcon(imageVector = muscleGroup.iconList, backgroundTint = backgroundTint)
}
