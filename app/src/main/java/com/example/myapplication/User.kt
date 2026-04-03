package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "User",
    val currentLevel: String = "Beginner",
    val streakCount: Int = 0,
    val lastWorkoutDate: Long = 0L
)
