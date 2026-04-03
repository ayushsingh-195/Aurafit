package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val level: String, // Beginner, Intermediate, Advanced
    val category: String, // Chest, Legs, Back, Core, Full Body
    val description: String, // Short summary
    val instructions: String, // Step by step documentation
    val isCompleted: Boolean = false
)
