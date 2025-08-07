package com.ritika.dowinnApp.utils

import com.ritika.dowinnApp.api.dataclasses.Task


sealed class DisplayItem {
    data class DateHeader(val label: String) : DisplayItem()
    data class TaskItem(val task: Task) : DisplayItem()
}