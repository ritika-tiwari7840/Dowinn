package com.ritika.dowinnApp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritika.dowinnApp.api.dataclasses.Task
import com.ritika.dowinnApp.repository.TaskRepository
import kotlinx.coroutines.launch
import java.io.File

class TaskViewModel : ViewModel() {
    val repository = TaskRepository()
    val tasks = MutableLiveData<List<Task>>()

    fun loadTasks() {
        viewModelScope.launch {
            val result = repository.getTasks()
            tasks.postValue(result ?: emptyList())
        }
    }

    fun createNewTask(
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: String,
        attachment: File? = null
    ) {
        viewModelScope.launch {
            repository.createTask(title, description, category, priority, dueDate, attachment)
            loadTasks() // updates LiveData, ListFragment will auto-update
        }
    }


}
