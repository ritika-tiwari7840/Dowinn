package com.ritika.dowinnApp.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ritika.dowinnApp.R
import com.ritika.dowinnApp.databinding.FragmentDetailsBinding
import com.ritika.dowinnApp.viewmodel.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DetailsFragment : Fragment(), EditFieldBottomSheet.OnFieldEditListener {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val taskViewModel: TaskViewModel by viewModels()

    // Task data - would typically come from arguments or database
    private var taskId = -1
    private var taskTitle = ""
    private var taskDescription = ""
    private var taskDate = ""
    private var taskTime = ""
    private var taskPriority = ""
    private var taskRepeat = ""
    private var taskAttachment = ""
    private var taskCollection = ""
    private var taskStatus = false

    // Coroutine scope for debouncing clicks
    private val statusUpdateScope = CoroutineScope(Dispatchers.Main.immediate)
    private var statusUpdateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DetailsFragment", "onCreate called")
        // Load data from arguments in onCreate
        arguments?.let { bundle ->
            loadTaskDataFromBundle(bundle)
        } ?: Log.d("Details", "No arguments bundle found")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // If coming back from another fragment, arguments might be null in onCreate, so re-check here.
        arguments?.let { bundle ->
            loadTaskDataFromBundle(bundle)
        }
        setupClickListeners()
        updateUI()
        setupStatusUpdateObserver()
    }

    private fun loadTaskDataFromBundle(bundle: Bundle) {
        taskId = bundle.getInt("taskId", -1)
        taskTitle = bundle.getString("taskTitle", "") ?: ""
        taskDescription = bundle.getString("taskDescription", "") ?: ""
        val fullDueDate = bundle.getString("taskDueDate", "") ?: ""
        if (fullDueDate.isNotEmpty() && fullDueDate != "null") {
            val parts = fullDueDate.split(" ")
            if (parts.size >= 2) {
                val datePart = parts[0]
                val timePart = parts[1]
                val dateComponents = datePart.split("-")
                if (dateComponents.size == 3) {
                    taskDate = "${dateComponents[2]}/${dateComponents[1]}/${dateComponents[0]}"
                }
                taskTime = timePart
            }
        } else {
            taskDate = ""
            taskTime = ""
        }
        taskPriority = bundle.getString("taskPriority", "") ?: ""
        taskRepeat = bundle.getString("taskRepeat", "") ?: ""
        taskCollection = bundle.getString("taskCategory", "") ?: ""
        taskStatus = bundle.getBoolean("taskCompleted", false)
        val attachmentUrl = bundle.getString("taskAttachment", "") ?: ""
        if (attachmentUrl.isNotEmpty() && attachmentUrl != "null") {
            taskAttachment = attachmentUrl.substringAfterLast("/")
        } else {
            taskAttachment = ""
        }

        Log.d("Details", "Loaded task details from Bundle - ID: $taskId, Title: $taskTitle, Status: $taskStatus")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        statusUpdateJob?.cancel()
    }

    private fun setupClickListeners() {
        binding.title.setOnClickListener {
            showEditBottomSheet(
                EditFieldBottomSheet.FIELD_TITLE, taskTitle
            )
        }
        binding.editIcon.setOnClickListener {
            showEditBottomSheet(
                EditFieldBottomSheet.FIELD_TITLE, taskTitle
            )
        }
        binding.description.setOnClickListener {
            showEditBottomSheet(
                EditFieldBottomSheet.FIELD_DESCRIPTION, taskDescription
            )
        }
        binding.Date.setOnClickListener {
            showEditBottomSheet(
                EditFieldBottomSheet.FIELD_DATETIME, "$taskDate $taskTime"
            )
        }
        binding.Time.setOnClickListener {
            showEditBottomSheet(
                EditFieldBottomSheet.FIELD_DATETIME, "$taskDate $taskTime"
            )
        }
        binding.priority.setOnClickListener {
            showEditBottomSheet(
                EditFieldBottomSheet.FIELD_PRIORITY, taskPriority
            )
        }
        binding.Repeat.setOnClickListener {
            showEditBottomSheet(
                EditFieldBottomSheet.FIELD_REPEAT, taskRepeat
            )
        }
        binding.attachment.setOnClickListener {
            showEditBottomSheet(
                EditFieldBottomSheet.FIELD_ATTACHMENT, taskAttachment
            )
        }
        binding.Collection.setOnClickListener {
            showEditBottomSheet(
                EditFieldBottomSheet.FIELD_COLLECTION, taskCollection
            )
        }

        binding.status.setOnClickListener {
            statusUpdateJob?.cancel()
            taskStatus = !taskStatus
            updateUI()
            Log.d("DetailsFragment", "Task status toggled to: $taskStatus")
            statusUpdateJob = statusUpdateScope.launch {
                delay(500)
                updateTaskStatusInBackend(taskId, taskStatus)
            }
        }
    }

    private fun showEditBottomSheet(fieldType: String, currentValue: String) {
        val bottomSheet = EditFieldBottomSheet.newInstance(
            fieldType = fieldType,
            currentValue = currentValue,
            taskTitle = taskTitle,
            taskId = taskId
        )
        bottomSheet.setOnFieldEditListener(this)
        bottomSheet.show(parentFragmentManager, "EditFieldBottomSheet")
    }

    private fun updateUI() {
        binding.title.text = if (taskTitle.isNotEmpty()) taskTitle else "No title"
        binding.description.text = if (taskDescription.isNotEmpty()) taskDescription else "No description"
        binding.Date.text = if (taskDate.isNotEmpty()) taskDate else "Add Date"
        binding.Time.text = if (taskTime.isNotEmpty()) taskTime else "Add Time"
        binding.priority.text = if (taskPriority.isNotEmpty()) taskPriority else "Edit Priority"
        binding.Repeat.text = if (taskRepeat.isNotEmpty()) taskRepeat else "Edit Repeatation"
        binding.attachment.text =
            if (taskAttachment.isNotEmpty()) taskAttachment else "Edit Attachment"
        binding.Collection.text =
            if (taskCollection.isNotEmpty()) taskCollection else "Edit Collection"

        val completedColor = ContextCompat.getColor(requireContext(), R.color.green)
        val pendingColor = ContextCompat.getColor(requireContext(), R.color.red)

        if (taskStatus) {
            binding.status.text = "Completed"
            binding.status.setTextColor(completedColor)
            binding.arrowImage.visibility = View.VISIBLE
        } else {
            binding.status.text = "Pending"
            binding.status.setTextColor(pendingColor)
            binding.arrowImage.visibility = View.GONE
        }
    }

    override fun onFieldUpdated(fieldType: String, newValue: String) {
        when (fieldType) {
            EditFieldBottomSheet.FIELD_TITLE -> taskTitle = newValue
            EditFieldBottomSheet.FIELD_DESCRIPTION -> taskDescription = newValue
            EditFieldBottomSheet.FIELD_DATETIME -> {
                if (newValue.isNotEmpty() && newValue.contains(" ")) {
                    val parts = newValue.split(" ")
                    if (parts.size >= 2) {
                        val datePart = parts[0]
                        val timePart = parts[1]
                        val dateComponents = datePart.split("-")
                        if (dateComponents.size == 3) taskDate =
                            "${dateComponents[2]}/${dateComponents[1]}/${dateComponents[0]}"
                        taskTime = timePart
                    }
                } else if (newValue.isEmpty()) {
                    taskDate = ""
                    taskTime = ""
                }
            }
            EditFieldBottomSheet.FIELD_PRIORITY -> taskPriority = newValue
            EditFieldBottomSheet.FIELD_REPEAT -> taskRepeat = newValue
            EditFieldBottomSheet.FIELD_ATTACHMENT -> taskAttachment = newValue
            EditFieldBottomSheet.FIELD_COLLECTION -> taskCollection = newValue
        }
        updateUI()
    }

    private fun updateTaskStatusInBackend(taskId: Int, isCompleted: Boolean) {
        if (taskId == -1) {
            Log.e("DetailsFragment", "Invalid task ID, cannot update status.")
            return
        }
        taskViewModel.updateTaskField(taskId, "completed", isCompleted.toString())
        Log.d("DetailsFragment", "API call initiated to update task $taskId status to $isCompleted")
    }

    private fun setupStatusUpdateObserver() {
        lifecycleScope.launch {
            taskViewModel.updateResult.collect { result ->
                result?.let {
                    if (it.isSuccess) {
                        val updatedTask = it.getOrNull()
                        updatedTask?.let { task ->
                            taskStatus = task.completed
                            Log.d(
                                "DetailsFragment",
                                "Task status updated successfully! UI reflecting new status: ${task.completed}"
                            )
                            Toast.makeText(
                                requireContext(),
                                "Status updated to ${if (taskStatus) "Completed" else "Pending"}",
                                Toast.LENGTH_SHORT
                            ).show()
                            updateUI()
                        }
                    } else {
                        val errorMessage =
                            it.exceptionOrNull()?.message ?: "Failed to update status."
                        Log.e("DetailsFragment", "Status update failed: $errorMessage")
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        taskStatus = !taskStatus
                        updateUI()
                    }
                }
            }
        }
    }

    override fun onUpdateSuccess() {
        Log.d("DetailsFragment", "API update successful")
        // Optional: refresh data or perform additional actions
    }

    override fun onUpdateError(error: String) {
        Log.e("DetailsFragment", "API update failed: $error")
        Toast.makeText(requireContext(), "Update failed: $error", Toast.LENGTH_LONG).show()
        // Optional: revert UI changes or show retry options
    }
}