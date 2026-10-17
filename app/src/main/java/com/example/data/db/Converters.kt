package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.SubTask
import com.example.data.model.TaskPriority
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val subTaskListType = Types.newParameterizedType(List::class.java, SubTask::class.java)
    private val subTaskAdapter = moshi.adapter<List<SubTask>>(subTaskListType)

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    @TypeConverter
    fun fromSubTaskList(subtasks: List<SubTask>?): String {
        return subTaskAdapter.toJson(subtasks ?: emptyList())
    }

    @TypeConverter
    fun toSubTaskList(json: String?): List<SubTask> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            subTaskAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return stringListAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority?): String {
        return (priority ?: TaskPriority.MEDIUM).name
    }

    @TypeConverter
    fun toTaskPriority(value: String?): TaskPriority {
        return try {
            if (value.isNullOrBlank()) TaskPriority.MEDIUM else TaskPriority.valueOf(value)
        } catch (_: Exception) {
            TaskPriority.MEDIUM
        }
    }
}
