package com.ritika.dowinnApp.api

import retrofit2.Response
import com.ritika.dowinnApp.api.dataclasses.ApiResponse
import com.ritika.dowinnApp.api.dataclasses.Task
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("tasks/")
    suspend fun getTasks(
        @Query("category") category: String? = null,
        @Query("start") startDate: String? = null,
        @Query("end") endDate: String? = null,
    ): retrofit2.Response<ApiResponse<List<Task>>>

    @GET("tasks/{id}/")
    suspend fun getTaskById(@Path("id") id: Int): retrofit2.Response<ApiResponse<Task>>

    @Multipart
    @POST("tasks/")
    suspend fun createTask(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("completed") completed: RequestBody,
        @Part("priority") priority: RequestBody,
        @Part("category") category: RequestBody,
        @Part("repeat") repeat: RequestBody,
        @Part("due_date") due_date: RequestBody,
        @Part attachment: MultipartBody.Part? = null,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @PATCH("tasks/{id}")
    suspend fun patchTask(
        @Path("id") taskId: Int,
        @FieldMap updates: Map<String, String>, // Use @FieldMap to send key-value pairs
    ): Response<ApiResponse<Task>>

    @Multipart
    @PATCH("tasks/{id}")
    suspend fun updateAttachment(
        @Path("id") taskId: Int,
        @Part attachment: MultipartBody.Part,
    ): Response<ApiResponse<Task>>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<ResponseBody>
}
