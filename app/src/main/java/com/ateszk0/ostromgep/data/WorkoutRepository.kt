package com.ateszk0.ostromgep.data

import android.content.Context
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.model.WorkoutHistoryEntry
import com.ateszk0.ostromgep.model.WorkoutTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WorkoutRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ostromgep_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getTheme(): String {
        return prefs.getString("theme", "Kék") ?: "Kék"
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
    }

    fun getExerciseLibrary(): List<ExerciseDef> {
        val libJsonV2 = prefs.getString("library_v2", null)
        if (libJsonV2 != null) {
            return gson.fromJson(libJsonV2, object : TypeToken<List<ExerciseDef>>() {}.type)
        }
        
        // Fallback or old data
        val oldLibJson = prefs.getString("library", null)
        if (oldLibJson != null) {
            val oldList: List<String> = gson.fromJson(oldLibJson, object : TypeToken<List<String>>() {}.type)
            val mapped = oldList.map { ExerciseDef(it) }
            saveExerciseLibrary(mapped)
            return mapped
        }

        val defaultList = listOf(ExerciseDef("Fekvenyomás"), ExerciseDef("Guggolás"), ExerciseDef("Felhúzás"))
        saveExerciseLibrary(defaultList)
        return defaultList
    }

    fun saveExerciseLibrary(library: List<ExerciseDef>) {
        prefs.edit().putString("library_v2", gson.toJson(library)).apply()
    }

    fun getSavedTemplates(): List<WorkoutTemplate> {
        val json = prefs.getString("templates", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<WorkoutTemplate>>() {}.type)
    }

    fun saveSavedTemplates(templates: List<WorkoutTemplate>) {
        prefs.edit().putString("templates", gson.toJson(templates)).apply()
    }

    fun getWorkoutHistory(): List<WorkoutHistoryEntry> {
        val json = prefs.getString("history", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<WorkoutHistoryEntry>>() {}.type)
    }

    fun saveWorkoutHistory(history: List<WorkoutHistoryEntry>) {
        prefs.edit().putString("history", gson.toJson(history)).apply()
    }
}
