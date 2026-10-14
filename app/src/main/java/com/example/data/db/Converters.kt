package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.SubTask
import com.example.data.model.TaskPriority
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromSubTaskList(subtasks: List<SubTask>?): String {
        if (subtasks.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (subtask in subtasks) {
            val obj = JSONObject()
            obj.put("id", subtask.id)
            obj.put("title", subtask.title)
            obj.put("isCompleted", subtask.isCompleted)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSubTaskList(data: String?): List<SubTask> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<SubTask>()
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SubTask(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }

    @TypeConverter
    fun fromPriority(priority: TaskPriority?): String {
        return priority?.name ?: TaskPriority.MEDIUM.name
    }

    @TypeConverter
    fun toPriority(value: String?): TaskPriority {
        return try {
            if (value != null) TaskPriority.valueOf(value) else TaskPriority.MEDIUM
        } catch (_: Exception) {
            TaskPriority.MEDIUM
        }
    }

    @TypeConverter
    fun fromTagsList(tags: List<String>?): String {
        if (tags.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (tag in tags) {
            array.put(tag)
        }
        return array.toString()
    }

    @TypeConverter
    fun toTagsList(data: String?): List<String> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val tag = array.optString(i, "").trim()
                if (tag.isNotEmpty()) {
                    list.add(tag)
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }
}
