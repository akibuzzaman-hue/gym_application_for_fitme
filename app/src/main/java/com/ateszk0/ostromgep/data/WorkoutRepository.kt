package com.ateszk0.ostromgep.data

import android.content.Context
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.model.WorkoutHistoryEntry
import com.ateszk0.ostromgep.model.WorkoutTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class WorkoutRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("ostromgep_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getTheme(): String {
        return prefs.getString("theme", "Kék") ?: "Kék"
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
            val defaults = loadDefaultExercises().map { it.name }.toSet()
            return raw.map { 
                val norm = it.normalize()
                if (norm.name !in defaults && !norm.isCustom) norm.copy(isCustom = true) else norm
            }
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
}
