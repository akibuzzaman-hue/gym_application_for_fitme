package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.ateszk0.ostromgep.R
import com.ateszk0.ostromgep.model.Equipment
import com.ateszk0.ostromgep.model.WorkoutTemplate
import com.ateszk0.ostromgep.ui.theme.*
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import kotlinx.coroutines.launch

@Composable
fun ExploreRoutinesScreen(viewModel: WorkoutViewModel, themeColor: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var preAssembledRoutines by remember { mutableStateOf<List<WorkoutTemplate>>(emptyList()) }

    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val generatorStatus by viewModel.generatorStatus.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf("") }
    var trainingDays by remember { mutableFloatStateOf(3f) }
    var customPrompt by remember { mutableStateOf("") }
    var availableEquipments by remember { mutableStateOf(Equipment.values().toSet()) }
    var showEquipmentDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val generatorErrorMessage by viewModel.generatorErrorMessage.collectAsState()

    LaunchedEffect(generatorStatus) {
        when (generatorStatus) {
            WorkoutViewModel.GeneratorStatus.SUCCESS -> {
                snackbarHostState.showSnackbar("Routines added to your list!")
                viewModel.resetGeneratorStatus()
            }

            WorkoutViewModel.GeneratorStatus.ERROR_KEY -> {
                snackbarHostState.showSnackbar("Invalid API key. Please check your key.")
                viewModel.resetGeneratorStatus()
            }

            WorkoutViewModel.GeneratorStatus.ERROR_GENERIC -> {
                snackbarHostState.showSnackbar("Generation failed: ${generatorErrorMessage ?: "Unknown error"}")
                viewModel.resetGeneratorStatus()
            }

            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        try {
            val inputStream = context.assets.open("pre_assembled_routines.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<WorkoutTemplate>>() {}.type
            preAssembledRoutines = Gson().fromJson(reader, type)
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(DarkBackground).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "Explore Routines",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { padding ->
        if (showApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                title = { Text("Gemini API Key", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        placeholder = { Text("Enter your Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveGeminiApiKey(tempApiKey)
                            showApiKeyDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showApiKeyDialog = false }) {
                        Text(
                            "Cancel",
                            color = themeColor
                        )
                    }
                }
            )
        }

        if (showEquipmentDialog) {
            AlertDialog(
                onDismissRequest = { showEquipmentDialog = false },
                title = { Text("Select Equipment", color = Color.White) },
                text = {
                    LazyColumn {
                        items(Equipment.values().toList()) { eq ->
                            val isSelected = availableEquipments.contains(eq)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        availableEquipments =
                                            if (isSelected) availableEquipments - eq else availableEquipments + eq
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = themeColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    eq.name.lowercase().replace("_", " ")
                                        .replaceFirstChar { it.uppercase() }, color = Color.White
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEquipmentDialog = false }) {
                        Text(
                            "Done",
                            color = themeColor
                        )
                    }
                },
                containerColor = SurfaceDark
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkBackground,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = themeColor
                    )
                }
            ) {
                val titles = listOf("AI Generator", "Templates")
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = themeColor,
                        unselectedContentColor = TextGray
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTabIndex == 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, null, tint = themeColor)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Generate your own routine",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "Training days per week: ${trainingDays.toInt()}",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Slider(
                                    value = trainingDays,
                                    onValueChange = { trainingDays = it },
                                    valueRange = 1f..7f,
                                    steps = 5,
                                    colors = SliderDefaults.colors(
                                        thumbColor = themeColor,
                                        activeTrackColor = themeColor
                                    )
                                )

                                val recommendedSplit = when (trainingDays.toInt()) {
                                    1, 2 -> "Full Body"
                                    3 -> "Full Body or PPL"
                                    4 -> "Upper / Lower"
                                    5, 6 -> "PPL (Push-Pull-Legs)"
                                    7 -> "Arnold Split or PPL"
                                    else -> "Full Body"
                                }
                                Text(
                                    "Recommended split: $recommendedSplit",
                                    color = TextGray,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val totalEq = Equipment.values().size
                                OutlinedButton(
                                    onClick = { showEquipmentDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        TextGray
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (availableEquipments.size == totalEq) "Equipment: All Available"
                                        else "Equipment: ${availableEquipments.size} Selected",
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = customPrompt,
                                    onValueChange = { customPrompt = it },
                                    label = { Text("Additional preferences (optional)") },
                                    placeholder = { Text("e.g. Focus on chest...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeColor,
                                        unfocusedBorderColor = TextGray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        unfocusedLabelColor = TextGray,
                                        focusedLabelColor = themeColor
                                    ),
                                    maxLines = 3
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (generatorStatus == WorkoutViewModel.GeneratorStatus.LOADING) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = themeColor
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            if (geminiApiKey.isNullOrBlank()) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Please add a Gemini API key in Settings first"
                                                    )
                                                }
                                            } else {
                                                viewModel.generateWorkoutPlan(
                                                    trainingDays.toInt(),
                                                    customPrompt,
                                                    availableEquipments
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "Generate Workout Plan",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTabIndex == 1) {
                        items(preAssembledRoutines) { template ->
                            var expanded by remember { mutableStateOf(false) }

                                    Card(
                                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            val arrowAngle by animateFloatAsState(
                                                targetValue = if (expanded) 180f else 0f,
                                                label = "arrow_rotation"
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .clickable { expanded = !expanded },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    template.templateName,
                                                    color = Color.White,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.KeyboardArrowDown,
                                                    null,
                                                    tint = TextGray,
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .graphicsLayer(rotationZ = arrowAngle)
                                                )
                                            }

                                            AnimatedVisibility(
                                                visible = expanded,
                                                enter = expandVertically(),
                                                exit = shrinkVertically()
                                            ) {
                                                Column {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    template.exercises.forEach { ex ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(ex.name, color = Color.White, fontSize = 14.sp)
                                                            Text(
                                                                "${ex.sets.size} sets",
                                                                color = TextGray,
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Button(
                                                        onClick = {
                                                            viewModel.saveNewTemplate(
                                                                template.templateName,
                                                                template.exercises
                                                            )
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar("Routine added to your list!")
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, null, tint = Color.White)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            "Add to My Routines",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                        }
                    }
                }
            }
        }
    }
