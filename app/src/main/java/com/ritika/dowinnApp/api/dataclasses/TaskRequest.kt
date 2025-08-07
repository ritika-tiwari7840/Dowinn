package com.ritika.dowinnApp.api.dataclasses

data class TaskRequest(
    val title: String,
    val description: String,
    val completed:String,
    val category: String,
    val priority: String,
    val due_date: String
)

