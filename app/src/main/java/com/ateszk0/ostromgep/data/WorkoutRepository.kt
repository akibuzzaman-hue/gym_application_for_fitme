package com.ateszk0.ostromgep.data

import android.content.Context
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.model.ExerciseSessionData
import com.ateszk0.ostromgep.model.WorkoutHistoryEntry
import com.ateszk0.ostromgep.model.WorkoutTemplate
import com.ateszk0.ostromgep.model.RoutineFolder
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.ateszk0.ostromgep.model.MuscleGroup
import com.ateszk0.ostromgep.model.Equipment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.InputStreamReader
import java.lang.reflect.Type

class WorkoutRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("ostromgep_prefs", Context.MODE_PRIVATE)
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secret_gemini_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private val gson = GsonBuilder()
        .registerTypeAdapter(MuscleGroup::class.java, object : JsonDeserializer<MuscleGroup> {
            override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): MuscleGroup {
                return try { MuscleGroup.valueOf(json.asString) } catch (e: Exception) { MuscleGroup.OTHER }
            }
        })
        .registerTypeAdapter(Equipment::class.java, object : JsonDeserializer<Equipment> {
            override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): Equipment {
                return try { Equipment.valueOf(json.asString) } catch (e: Exception) { Equipment.OTHER }
            }
        })
        .create()

    fun getTheme(): String {
        return prefs.getString("theme", "Piros") ?: "Piros"
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
    }

    fun getLanguage(): String = prefs.getString("app_language", "en") ?: "en"
    fun saveLanguage(lang: String) {
        prefs.edit().putString("app_language", lang).apply()
    }

    fun getExerciseLibrary(): List<ExerciseDef> {
        val libJsonV2 = prefs.getString("library_v2", null)
        if (libJsonV2 != null) {
            val raw: List<ExerciseDef> = gson.fromJson(libJsonV2, object : TypeToken<List<ExerciseDef>>() {}.type)
            val defaults = loadDefaultExercises()
            val defaultNames = defaults.map { it.name }.toSet()
            var changed = false
            val normalized = raw.map { 
                val norm = it.normalize()
                val def = defaults.find { d -> d.name == norm.name }
                if (norm.name !in defaultNames && !norm.isCustom) {
                    changed = true
                    norm.copy(isCustom = true) 
                } else if (def != null && (norm.equipment == com.ateszk0.ostromgep.model.Equipment.NONE || norm.equipment == null)) {
                    changed = true
                    norm.copy(equipment = def.equipment, muscleGroups = def.muscleGroups)
                } else norm
            }
            // Migration: merge in any new default exercises not yet in the user's library
            val existingNames = normalized.map { it.name }.toSet()
            val newDefaults = defaults.filter { it.name !in existingNames }
            if (newDefaults.isNotEmpty() || changed) {
                val merged = normalized + newDefaults
                saveExerciseLibrary(merged)
                return merged
            }
            return normalized
        }
        
        // Fallback or old data
        val oldLibJson = prefs.getString("library", null)
        if (oldLibJson != null) {
            val oldList: List<String> = gson.fromJson(oldLibJson, object : TypeToken<List<String>>() {}.type)
            val mapped = oldList.map { ExerciseDef(it, isCustom = true) }
            saveExerciseLibrary(mapped)
            return mapped
        }

        val defaultList = loadDefaultExercises()
        saveExerciseLibrary(defaultList)
        return defaultList
    }

    private fun loadDefaultExercises(): List<ExerciseDef> {
        return try {
            val inputStream = context.assets.open("default_exercises.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<ExerciseDef>>() {}.type
            val list: List<ExerciseDef> = gson.fromJson(reader, type)
            reader.close()
            list
        } catch (e: Exception) {
            listOf(ExerciseDef("Fekvenyomás"), ExerciseDef("Guggolás"), ExerciseDef("Felhúzás"))
        }
    }

    fun saveExerciseLibrary(library: List<ExerciseDef>) {
        prefs.edit().putString("library_v2", gson.toJson(library)).apply()
    }

    fun getSavedTemplates(): List<WorkoutTemplate> {
        val json = prefs.getString("templates", null) ?: return emptyList()
        val raw: List<WorkoutTemplate> = gson.fromJson(json, object : TypeToken<List<WorkoutTemplate>>() {}.type)
        return raw.map { it.normalize() }
    }

    fun saveSavedTemplates(templates: List<WorkoutTemplate>) {
        prefs.edit().putString("templates", gson.toJson(templates)).apply()
    }

    fun getRoutineFolders(): List<RoutineFolder> {
        val json = prefs.getString("routine_folders", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<RoutineFolder>>() {}.type)
    }

    fun saveRoutineFolders(folders: List<RoutineFolder>) {
        prefs.edit().putString("routine_folders", gson.toJson(folders)).apply()
    }

    fun getActiveFolderId(): String? {
        return prefs.getString("active_folder_id", null)
    }

    fun saveActiveFolderId(id: String?) {
        if (id == null) {
            prefs.edit().remove("active_folder_id").apply()
        } else {
            prefs.edit().putString("active_folder_id", id).apply()
        }
    }

    fun getWorkoutHistory(): List<WorkoutHistoryEntry> {
        val json = prefs.getString("history", null) ?: return emptyList()
        val raw: List<WorkoutHistoryEntry> = gson.fromJson(json, object : TypeToken<List<WorkoutHistoryEntry>>() {}.type)
        return raw.map { it.normalize() }
    }

    fun saveWorkoutHistory(history: List<WorkoutHistoryEntry>) {
        prefs.edit().putString("history", gson.toJson(history)).apply()
    }

    fun getBodyWeightHistory(): List<com.ateszk0.ostromgep.model.BodyWeightEntry> {
        val json = prefs.getString("bodyweight", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<com.ateszk0.ostromgep.model.BodyWeightEntry>>() {}.type)
    }

    fun saveBodyWeightHistory(history: List<com.ateszk0.ostromgep.model.BodyWeightEntry>) {
        prefs.edit().putString("bodyweight", gson.toJson(history)).apply()
    }

    fun getUsername(): String {
        return prefs.getString("username", "User") ?: "User"
    }

    fun saveUsername(name: String) {
        prefs.edit().putString("username", name).apply()
    }

    fun getProfilePictureUri(): String? {
        return prefs.getString("profile_pic_uri", null)
    }

    fun saveProfilePictureUri(uri: String) {
        prefs.edit().putString("profile_pic_uri", uri).apply()
    }

    fun getGeminiApiKey(): String? {
        return try {
            encryptedPrefs.getString("gemini_api_key", null)
        } catch (e: Exception) { null }
    }

    fun saveGeminiApiKey(key: String) {
        try {
            encryptedPrefs.edit().putString("gemini_api_key", key).apply()
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Active workout state persistence
    data class ActiveWorkoutState(
        val exercises: List<ExerciseSessionData>,
        val templateId: Int?,
        val totalSeconds: Int,
        val restTimerSeconds: Int
    )

    fun saveActiveWorkoutState(state: ActiveWorkoutState) {
        prefs.edit()
            .putString("active_workout_exercises", gson.toJson(state.exercises))
            .putString("active_workout_template_id", state.templateId?.toString())
            .putInt("active_workout_total_seconds", state.totalSeconds)
            .putInt("active_workout_rest_seconds", state.restTimerSeconds)
            .apply()
    }

    fun getActiveWorkoutState(): ActiveWorkoutState? {
        val json = prefs.getString("active_workout_exercises", null) ?: return null
        return try {
            val exercises: List<ExerciseSessionData> = gson.fromJson(json, object : TypeToken<List<ExerciseSessionData>>() {}.type)
            if (exercises.isEmpty()) return null
            val templateIdStr = prefs.getString("active_workout_template_id", null)
            val templateId = templateIdStr?.toIntOrNull()
            val totalSeconds = prefs.getInt("active_workout_total_seconds", 0)
            val restTimerSeconds = prefs.getInt("active_workout_rest_seconds", 0)
            ActiveWorkoutState(exercises.map { it.normalize() }, templateId, totalSeconds, restTimerSeconds)
        } catch (e: Exception) { null }
    }

    fun clearActiveWorkoutState() {
        prefs.edit()
            .remove("active_workout_exercises")
            .remove("active_workout_template_id")
            .remove("active_workout_total_seconds")
            .remove("active_workout_rest_seconds")
            .apply()
    }
}
