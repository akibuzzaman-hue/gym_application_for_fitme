package com.ateszk0.ostromgep.model

data class RoutineFolder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val templateIds: List<Int>
)
