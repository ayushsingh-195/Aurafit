package com.example.myapplication

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessMealDao {
    @Query("SELECT * FROM mess_meals")
    fun getAllMeals(): Flow<List<MessMeal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MessMeal)

    @Query("SELECT * FROM mess_meals WHERE day = :day")
    suspend fun getMealByDay(day: String): MessMeal?
}
