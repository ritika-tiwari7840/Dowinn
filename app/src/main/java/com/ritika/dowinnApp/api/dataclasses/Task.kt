package com.ritika.dowinnApp.api.dataclasses
data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val completed: Boolean,
    val created_at: String,
    val updated_at: String,
    val priority: String,
    val repeat: String,
    val category: String,
    val due_date: String?,
    val attachment: String?, // URL of uploaded file
    val user: Int // or a nested object if your API returns more info
)


