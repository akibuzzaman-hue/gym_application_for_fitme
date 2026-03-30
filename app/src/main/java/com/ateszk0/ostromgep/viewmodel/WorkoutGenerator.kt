package com.ateszk0.ostromgep.viewmodel

import android.content.Context
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.model.WorkoutTemplate
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InvalidApiKeyException : Exception("Invalid Gemini API Key")

class WorkoutGenerator(private val context: Context) {

    private fun getExerciseNames(): String {
        return try {
            val inputStream = context.assets.open("default_exercises.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<ExerciseDef>>() {}.type
            val exercises: List<ExerciseDef> = Gson().fromJson(reader, type)
            reader.close()
            exercises.joinToString("\n") { it.name }
        } catch (e: Exception) {
            "Bench Press\nSquat\nDeadlift" // Fallback
        }
    }

    suspend fun generateRoutines(apiKey: String, trainingDays: Int, customPrompt: String): List<WorkoutTemplate> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw InvalidApiKeyException()

        val splitRecommendation = when (trainingDays) {
            1, 2 -> "Full Body"
            3 -> "Full Body or PPL (Push-Pull-Legs)"
            4 -> "Upper / Lower"
            5 -> "PPL (Push-Pull-Legs) + Upper / Lower or 5-day bro split"
            6 -> "PPL (Push-Pull-Legs)"
            7 -> "Arnold Split or PPL"
            else -> "Full Body"
        }

        val availableExercises = getExerciseNames()

        val systemPrompt = """
            You are an expert fitness coach and personal trainer app component. 
            Your task is to generate customized workout routines for a user based on their preferences. 
            The goal is muscle hypertrophy, so stick to scientifically proven rep ranges (typically 8-12 reps per set, 3-4 sets per exercise) and appropriate rest times (e.g. 60-120 seconds).
            
            CRITICAL CONSTRAINTS:
            1. You MUST ONLY use the exercise names provided in the 'Available Exercises' list below. DO NOT invent new exercises. DO NOT use emojis in the exercise names.
            2. Output your response STRICTLY as a valid JSON array of `WorkoutTemplate` objects. Do not wrap the JSON in markdown blocks like ```json ... ```, just output the raw JSON array.
            3. For the `id` field of the `WorkoutTemplate`, you MUST use negative integers starting from -9001, -9002, etc. (To avoid colliding with regular user-created routines).
            4. `sets.id` should just be sequential positive integers (1, 2, 3...) for each exercise.
            5. `setLabel` should be the string form of the set number (e.g. "1", "2").
            6. `kg` can be "0" for the template.
            7. `rpe` should be empty ("").
            8. `previousText` should be "-".
            9. `isWarmup` and `isCompleted` should be `false`.
            
            Available Exercises:
            $availableExercises
        """.trimIndent()

        val userPrompt = """
            Please generate a complete workout plan for a user training $trainingDays days per week.
            Recommended split based on frequency: $splitRecommendation
            Please provide one routine (WorkoutTemplate) for each training day (so a total of $trainingDays templates).
            Each routine should be focused and have an appropriate `templateName` in English (e.g. "Upper Body", "Push Day").
            
            ADDITIONAL PREFERENCES / CUSTOM PROMPT FROM THE USER:
            "${if (customPrompt.isNotBlank()) customPrompt else "None."}"
            
            JSON Schema Reference:
            [
              {
                "id": -9001,
                "templateName": "Push Day",
                "exercises": [
                  {
                    "id": 1,
                    "name": "Bench Press (Barbell)",
                    "restTimerDuration": 120,
                    "sets": [
                      {
                        "id": 1,
                        "setLabel": "1",
                        "kg": "0",
                        "reps": "8",
                        "rpe": "",
                        "isWarmup": false,
                        "isCompleted": false,
                        "previousText": "-"
                      }
                    ]
                  }
                ]
              }
            ]
            
            Begin raw JSON output:
        """.trimIndent()

        val model = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            systemInstruction = content { text(systemPrompt) },
            generationConfig = generationConfig {
                temperature = 0.2f // Keep it somewhat deterministic to follow JSON format
                responseMimeType = "application/json"
            }
        )

        try {
            val response = model.generateContent(userPrompt)
            val jsonText = response.text ?: ""
            var cleanJson = jsonText.trim()
            val startIdx = cleanJson.indexOf('[')
            val endIdx = cleanJson.lastIndexOf(']')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                cleanJson = cleanJson.substring(startIdx, endIdx + 1)
            } else {
                cleanJson = cleanJson.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }
            
            val type = object : TypeToken<List<WorkoutTemplate>>() {}.type
            val generatedTemplates: List<WorkoutTemplate> = Gson().fromJson(cleanJson, type)
            generatedTemplates
        } catch (e: Exception) {
            // Check for API key errors (e.g., 400 Bad Request, API_KEY_INVALID)
            if (e.message?.contains("API_KEY_INVALID") == true || e.message?.contains("API key not valid") == true) {
                throw InvalidApiKeyException()
            }
            throw Exception("Failed to generate or parse response: ${e.message}", e)
        }
    }
}
