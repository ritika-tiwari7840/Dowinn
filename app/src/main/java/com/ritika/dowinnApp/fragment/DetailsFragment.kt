package com.ritika.dowinnApp.fragment

import android.os.Bundle
import android.os.SystemClock
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
        arguments?.let { bundle ->
            loadTaskDataFromBundle(bundle)
        }
        setupClickListeners()
        updateUI()
        setupStatusUpdateObserver()
    }

    private fun loadTaskDataFromBundle(bundle: Bundle) {
        try {
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
            Log.d(
                "Details",
                "Loaded task details from Bundle - ID: $taskId, Title: $taskTitle, Status: $taskStatus"
            )
        } catch (e: Exception) {
            Log.e("DetailsFragment", "Failed to load task data from bundle: ${e.message}")
            // Optional: show a user-facing error message or navigate back
            Toast.makeText(requireContext(), "Failed to load task details.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        statusUpdateJob?.cancel()
    }

    private fun setupClickListeners() {
        // Applying debounce to all listeners that open the bottom sheet
        binding.title.setOnClickListener(debounce {
            showEditBottomSheet(EditFieldBottomSheet.FIELD_TITLE, taskTitle)
        })
        binding.editIcon.setOnClickListener(debounce {
            showEditBottomSheet(EditFieldBottomSheet.FIELD_TITLE, taskTitle)
        })
        binding.description.setOnClickListener(debounce {
            showEditBottomSheet(EditFieldBottomSheet.FIELD_DESCRIPTION, taskDescription)
        })
        binding.Date.setOnClickListener(debounce {
            showEditBottomSheet(EditFieldBottomSheet.FIELD_DATETIME, "$taskDate $taskTime")
        })
        binding.Time.setOnClickListener(debounce {
            showEditBottomSheet(EditFieldBottomSheet.FIELD_DATETIME, "$taskDate $taskTime")
        })
        binding.priority.setOnClickListener(debounce {
            showEditBottomSheet(EditFieldBottomSheet.FIELD_PRIORITY, taskPriority)
        })
        binding.Repeat.setOnClickListener(debounce {
            showEditBottomSheet(EditFieldBottomSheet.FIELD_REPEAT, taskRepeat)
        })
        binding.attachment.setOnClickListener(debounce {
            showEditBottomSheet(EditFieldBottomSheet.FIELD_ATTACHMENT, taskAttachment)
        })
        binding.Collection.setOnClickListener(debounce {
            showEditBottomSheet(EditFieldBottomSheet.FIELD_COLLECTION, taskCollection)
        })

        binding.status.setOnClickListener {
            statusUpdateJob?.cancel()
            taskStatus = !taskStatus
            if (_binding != null) {
                updateUI()
            }
            Log.d("DetailsFragment", "Task status toggled to: $taskStatus")
            statusUpdateJob = statusUpdateScope.launch {
                delay(500)
                updateTaskStatusInBackend(taskId, taskStatus)
            }
        }
    }

    fun debounce(delay: Long = 500L, block: (View) -> Unit): View.OnClickListener {
        var lastClickTime = 0L
        return View.OnClickListener {
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastClickTime > delay) {
                lastClickTime = currentTime
                block(it)
            }
        }
    }

    private fun showEditBottomSheet(fieldType: String, currentValue: String) {
        // Use try-catch to guard against potential issues with fragment manager
        try {
            if (isAdded) { // Check if fragment is attached to activity
                val bottomSheet = EditFieldBottomSheet.newInstance(
                    fieldType = fieldType,
                    currentValue = currentValue,
                    taskTitle = taskTitle,
                    taskId = taskId
                )
                bottomSheet.setOnFieldEditListener(this)
                bottomSheet.show(parentFragmentManager, "EditFieldBottomSheet")
            }
        } catch (e: Exception) {
            Log.e("DetailsFragment", "Failed to show bottom sheet: ${e.message}")
        }
    }

    private fun updateUI() {
        // Essential check: The view could be null if the fragment is in a torn-down state.
        if (_binding == null) return

        try {
            binding.title.text = if (taskTitle.isNotEmpty()) taskTitle else "No title"
            binding.description.text =
                if (taskDescription.isNotEmpty()) taskDescription else "No description"
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
        } catch (e: Exception) {
            Log.e("DetailsFragment", "Error updating UI: ${e.message}")
        }
    }

    override fun onFieldUpdated(fieldType: String, newValue: String) {
        // The logic inside this block is generally safe, but a try-catch is good practice
        // for defensive programming against unexpected data formats.
        try {
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
        } catch (e: Exception) {
            Log.e("DetailsFragment", "Error processing updated field: ${e.message}")
        }
    }

    private fun updateTaskStatusInBackend(taskId: Int, isCompleted: Boolean) {
        if (taskId == -1) {
            Log.e("DetailsFragment", "Invalid task ID, cannot update status.")
            return
        }
        // This call is in a coroutine, so it's a safe place for API calls.
        taskViewModel.updateTaskField(taskId, "completed", isCompleted.toString())
        Log.d("DetailsFragment", "API call initiated to update task $taskId status to $isCompleted")
    }

    private fun setupStatusUpdateObserver() {
        lifecycleScope.launch {
            taskViewModel.updateResult.collect { result ->
                if (_binding == null) {
                    Log.d("DetailsFragment", "View is null, skipping UI update.")
                    return@collect
                }
                try {
                    result?.let {
                        if (it.isSuccess) {
                            val updatedTask = it.getOrNull()
                            updatedTask?.let { task ->
                                taskStatus = task.completed
                                Log.d("DetailsFragment", "Task status updated successfully!")
                                Toast.makeText(
                                    requireContext(),
                                    "Status updated to ${if (taskStatus) "Completed" else "Pending"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                updateUI()
                            }
                        } else {
                            val errorMessage = it.exceptionOrNull()?.message ?: "Failed to update status."
                            Log.e("DetailsFragment", "Status update failed: $errorMessage")
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                            // Revert status on failure
                            taskStatus = !taskStatus
                            updateUI()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DetailsFragment", "Error during status observer handling: ${e.message}")
                }
            }
        }
    }

    override fun onUpdateSuccess() {
        Log.d("DetailsFragment", "API update successful")
    }

    override fun onUpdateError(error: String) {
        Log.e("DetailsFragment", "API update failed: $error")
        // Check if context is available before showing a toast
        if (context != null) {
            Toast.makeText(requireContext(), "Update failed: $error", Toast.LENGTH_LONG).show()
        }
    }
}