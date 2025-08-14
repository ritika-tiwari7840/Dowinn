package com.ritika.dowinnApp.api.dataclasses

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
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
    val attachment: String?,
    val user: Int
) : Parcelable

