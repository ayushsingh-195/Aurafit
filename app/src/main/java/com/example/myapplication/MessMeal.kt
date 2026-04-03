package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mess_meals")
data class MessMeal(
    @PrimaryKey val day: String,
    val mealName: String = "",
    val isHighProtein: Boolean = true
)
