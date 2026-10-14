package com.example.data.repository

import com.example.data.db.TaskDao
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?> = taskDao.getTaskByIdFlow(id)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun insertTasks(tasks: List<TaskEntity>) = taskDao.insertTasks(tasks)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun deleteById(id: Long) = taskDao.deleteById(id)

    suspend fun deleteAllTasks() = taskDao.deleteAllTasks()

    suspend fun getTaskCount(): Int = taskDao.getTaskCount()
}
