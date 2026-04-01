package com.ateszk0.ostromgep.utils

import com.ateszk0.ostromgep.model.ExerciseDef
import java.util.Locale

/**
 * Calculates a relevance score for An exercise based on a search query.
 * Handles fuzzy matching (typos), word-order independence, partial matches, and metadata (muscles/equipment).
 * Higher score = more relevant. Returns 0 if no match.
 */
fun ExerciseDef.calculateSearchScore(query: String): Int {
    if (query.isBlank()) return 100 // Default to pass when empty

    val targetNameLower = this.name.lowercase(Locale.getDefault())
    val searchLower = query.lowercase(Locale.getDefault()).trim()
    val searchWords = searchLower.split(Regex("\\s+"))
    
    // 1. Exact Name / Full Substring (Highest score)
    if (targetNameLower.contains(searchLower)) {
        return 100
    }
    
    var score = 0
    val targetWords = targetNameLower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
    
    // Convert enums to lower strings for matching
    val metadataWords = mutableListOf<String>()
    metadataWords.add(this.equipment.name.lowercase(Locale.getDefault()).replace("_", " "))
    this.muscleGroups.forEach {
        metadataWords.add(it.name.lowercase(Locale.getDefault()).replace("_", " "))
    }
    
    // 2. Check each search word independently (Order Independent Match)
    var allWordsMatched = true
    var matchedWordCount = 0

    for (sWord in searchWords) {
        var wordMatched = false
        var bestWordScore = 0
        
        // Exact/Partial Word Match in Title
        if (targetWords.any { it == sWord }) {
            wordMatched = true
            bestWordScore = maxOf(bestWordScore, 20)
        } else if (targetWords.any { it.contains(sWord) || sWord.contains(it) }) {
            wordMatched = true
            bestWordScore = maxOf(bestWordScore, 10)
        } 
        
        // Fuzzy Matching (Levenshtein) in Title
        if (!wordMatched && sWord.length > 3) {
            for (tWord in targetWords) {
                if (levenshteinDistance(sWord, tWord) <= 1) { // 1 typo allowed
                    wordMatched = true
                    bestWordScore = maxOf(bestWordScore, 8)
                    break
                }
            }
        }
        
        // Exact/Partial Match in Metadata (Muscle/Equipment)
        if (!wordMatched) {
            if (metadataWords.any { it.contains(sWord) || sWord.contains(it) }) {
                wordMatched = true
                bestWordScore = maxOf(bestWordScore, 5) // Metadata matches are less prioritized than title
            }
        }
        
        if (wordMatched) {
            score += bestWordScore
            matchedWordCount++
        } else {
            allWordsMatched = false
        }
    }
    
    // 3. Final Scoring Adjustments
    if (allWordsMatched && searchWords.isNotEmpty()) {
        score += 50 // Huge bonus if ALL words from query exist in some form
    } else if (matchedWordCount == 0) {
        return 0
    }
    
    return score
}

/**
 * Basic Levenshtein distance algorithm to measure string similarity.
 */
private fun levenshteinDistance(s: String, t: String): Int {
    if (s == t) return 0
    if (s.isEmpty()) return t.length
    if (t.isEmpty()) return s.length

    val v0 = IntArray(t.length + 1)
    val v1 = IntArray(t.length + 1)

    for (i in 0..t.length) {
        v0[i] = i
    }

    for (i in 0 until s.length) {
        v1[0] = i + 1
        for (j in 0 until t.length) {
            val cost = if (s[i] == t[j]) 0 else 1
            v1[j + 1] = minOf(v1[j] + 1, v0[j + 1] + 1, v0[j] + cost)
        }
        for (j in 0..t.length) {
            v0[j] = v1[j]
        }
    }
    return v0[t.length]
}
