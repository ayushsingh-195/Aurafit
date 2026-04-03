package com.example.myapplication

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE level = :level")
    fun getExercisesByLevel(level: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun getExerciseById(id: Int): Flow<Exercise?>

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE level = :level AND isCompleted = 1")
    fun getCompletedCount(level: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM exercises WHERE level = :level")
    fun getTotalCount(level: String): Flow<Int>
}
