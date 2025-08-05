package com.ritika.dowinnApp.repository

import com.ritika.dowinnApp.api.RetrofitClient
import com.ritika.dowinnApp.api.dataclasses.Task
import com.ritika.dowinnApp.api.dataclasses.TaskRequest
import com.ritika.dowinnApp.api.dataclasses.ApiResponse

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

import retrofit2.Response
import java.io.File
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody

class TaskRepository {

    suspend fun getTasks(): List<Task>? {
        val response = RetrofitClient.apiService.getTasks()
        if (response.isSuccessful && response.body()?.status == "success") {
            return response.body()?.payload
        }
        return null
    }

    suspend fun createTask(
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: String,
        attachmentFile: File?
    ): Response<ResponseBody> {

        val titlePart = title.toRequestBody("text/plain".toMediaType())
        val descriptionPart = description.toRequestBody("text/plain".toMediaType())
        val categoryPart = category.toRequestBody("text/plain".toMediaType())
        val priorityPart = priority.toRequestBody("text/plain".toMediaType())
        val dueDatePart = dueDate.toRequestBody("text/plain".toMediaType())

        val attachmentPart = attachmentFile?.let {
            val requestFile = it.asRequestBody("multipart/form-data".toMediaType())
            MultipartBody.Part.createFormData("attachment", it.name, requestFile)
        }

        return RetrofitClient.apiService.createTask(
            title = titlePart,
            description = descriptionPart,
            priority = priorityPart,
            category = categoryPart,
            dueDate = dueDatePart,
            attachment = attachmentPart
        )
    }

}
