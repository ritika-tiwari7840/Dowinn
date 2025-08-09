package com.ritika.dowinnApp.viewmodel

import androidx.lifecycle.LiveData
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
    private val _deleteResult = MutableLiveData<Result<String>>()
    val deleteResult: LiveData<Result<String>> = _deleteResult

    fun loadTasks() {
        viewModelScope.launch {
            val result = repository.getTasks()
            tasks.postValue(result ?: emptyList())
        }
    }

    fun createNewTask(
        title: String,
        description: String,
        completed: String,
        category: String,
        priority: String,
        due_date: String,
        repeat: String,
        attachment: File? = null,
    ) {
        viewModelScope.launch {
            repository.createTask(
                title, description, completed, category, priority, due_date, repeat, attachment
            )
            loadTasks() // updates LiveData, ListFragment will auto-update
        }
    }


    fun deleteTask(id: Int) {
        viewModelScope.launch {
            val result = repository.deleteTask(id)
            _deleteResult.postValue(result)
        }
    }
}

