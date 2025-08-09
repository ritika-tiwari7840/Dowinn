package com.ritika.dowinnApp.repository

import android.util.Log
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
import com.ritika.dowinnApp.api.ApiService
import com.ritika.dowinnApp.api.RetrofitClient.apiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import org.json.JSONObject

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
        completed: String,
        category: String,
        priority: String,
        due_date: String,
        repeat: String,
        attachmentFile: File?,
    ): Response<ResponseBody> {

        val titlePart = title.toRequestBody("text/plain".toMediaType())
        val descriptionPart = description.toRequestBody("text/plain".toMediaType())
        val completedPart = completed.toRequestBody("text/plain".toMediaType())
        val categoryPart = category.toRequestBody("text/plain".toMediaType())
        val priorityPart = priority.toRequestBody("text/plain".toMediaType())
        val repeatPart = repeat.toRequestBody("text/plain".toMediaType())
        val dueDatePart = due_date.toRequestBody("text/plain".toMediaType())

        val attachmentPart = attachmentFile?.let {
            val requestFile = it.asRequestBody("multipart/form-data".toMediaType())
            MultipartBody.Part.createFormData("attachment", it.name, requestFile)
        }
        Log.d("AddTaskFragment", "createTask:  $due_date")
        return RetrofitClient.apiService.createTask(
            title = titlePart,
            description = descriptionPart,
            completed = completedPart,
            priority = priorityPart,
            category = categoryPart,
            due_date = dueDatePart,
            repeat = repeatPart,
            attachment = attachmentPart
        )
    }

    suspend fun deleteTask(id: Int): Result<String> {
        return try {
            val response = apiService.deleteTask(id)

            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: ""
                val message = try {
                    JSONObject(bodyString).optString("message", bodyString)
                } catch (e: Exception) {
                    bodyString
                }
                Result.success(message.ifBlank { "Deleted successfully" })
            } else {
                // Use your parseErrorMessage for failures
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private fun parseErrorMessage(errorBody: String?): String {
        return try {
            val jsonObject = JSONObject(errorBody ?: "")
            val keys = jsonObject.keys()
            buildString {
                while (keys.hasNext()) {
                    val key = keys.next()
                    val messages = jsonObject.getJSONArray(key)
                    for (i in 0 until messages.length()) {
                        append("${key.replaceFirstChar { it.uppercase() }}: ${messages[i]}\n")
                    }
                }
            }.trim()
        } catch (e: Exception) {
            errorBody ?: "Unknown error"
        }
    }

}
