package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RoutineTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineTemplateDao {
    @Query("SELECT * FROM routine_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<RoutineTemplateEntity>>

    @Query("SELECT * FROM routine_templates ORDER BY createdAt DESC")
    suspend fun getAllTemplatesSnapshot(): List<RoutineTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: RoutineTemplateEntity): Long

    @Query("DELETE FROM routine_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("DELETE FROM routine_templates")
    suspend fun deleteAllTemplates()
}
