package com.ritika.dowinnApp.fragment

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ritika.dowinnApp.R
import com.ritika.dowinnApp.api.dataclasses.Task
import com.ritika.dowinnApp.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.text.SimpleDateFormat
import java.lang.Exception

class EditFieldBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "EditFieldBottomSheet"
        private const val ARG_FIELD_TYPE = "field_type"
        private const val ARG_CURRENT_VALUE = "current_value"
        private const val ARG_TASK_TITLE = "task_title"
        private const val ARG_TASK_ID = "task_id"

        // Field types
        const val FIELD_TITLE = "title"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_DATETIME = "datetime"
        const val FIELD_PRIORITY = "priority"
        const val FIELD_REPEAT = "repeat"
        const val FIELD_ATTACHMENT = "attachment"
        const val FIELD_COLLECTION = "collection"
        const val FIELD_STATUS = "status"

        fun newInstance(
            fieldType: String,
            currentValue: String = "",
            taskTitle: String = "",
            taskId: Int = -1,
        ): EditFieldBottomSheet {
            return EditFieldBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FIELD_TYPE, fieldType)
                    putString(ARG_CURRENT_VALUE, currentValue)
                    putString(ARG_TASK_TITLE, taskTitle)
                    putInt(ARG_TASK_ID, taskId)
                }
            }
        }
    }

    interface OnFieldEditListener {
        fun onFieldUpdated(fieldType: String, newValue: String)
        fun onUpdateSuccess()
        fun onUpdateError(error: String)
    }

    // ViewModel
    private val taskViewModel: TaskViewModel by viewModels()

    private var listener: OnFieldEditListener? = null
    private lateinit var fieldType: String
    private var currentValue: String = ""
    private var taskTitle: String = ""
    private var taskId: Int = -1

    // Views
    private lateinit var titleText: TextView
    private lateinit var contentContainer: LinearLayout
    private lateinit var addButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar

    private val calendar = Calendar.getInstance()

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val file = uriToFile(uri)
                file?.let {
                    selectedFile = it
                    attachmentText.text = it.name
                    fileSizeText.text =
                        "${formatFileSize(it.length())} • ${getFileExtension(it.name).uppercase()}"
                    Log.d(TAG, "File selected: ${it.name}, size: ${it.length()}")
                } ?: run {
                    Log.e(TAG, "Failed to select file from URI: $uri")
                    Toast.makeText(
                        requireContext(), "Failed to select file", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private var selectedFile: File? = null
    private lateinit var attachmentText: TextView
    private lateinit var fileSizeText: TextView

    fun setOnFieldEditListener(listener: OnFieldEditListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            arguments?.let {
                fieldType = it.getString(ARG_FIELD_TYPE, FIELD_TITLE)
                currentValue = it.getString(ARG_CURRENT_VALUE, "")
                taskTitle = it.getString(ARG_TASK_TITLE, "")
                taskId = it.getInt(ARG_TASK_ID, -1)
                Log.d(TAG, "onCreate - Field Type: $fieldType, Task ID: $taskId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(requireContext(), "Error initializing bottom sheet", Toast.LENGTH_SHORT)
                .show()
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_field_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupObservers()
        setupBottomSheet()
    }

    private fun initViews(view: View) {
        titleText = view.findViewById(R.id.bottomSheetTitle)
        contentContainer = view.findViewById(R.id.contentContainer)
        addButton = view.findViewById(R.id.addButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        progressBar = view.findViewById(R.id.progressbar)

        cancelButton.setOnClickListener { dismiss() }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            taskViewModel.updateResult.collect { result ->
                hideLoading()
                result?.let {
                    if (it.isSuccess) {
                        val updatedTask = it.getOrNull() as? Task
                        if (updatedTask != null) {
                            Log.d(TAG, "Task update successful for field: $fieldType")

                            // Extract the actual new value based on the field type
                            val newValue = when (fieldType) {
                                FIELD_TITLE -> updatedTask.title
                                FIELD_DESCRIPTION -> updatedTask.description
                                FIELD_DATETIME -> updatedTask.due_date // This should be the formatted date/time string from the backend
                                FIELD_PRIORITY -> updatedTask.priority
                                FIELD_REPEAT -> updatedTask.repeat
                                FIELD_ATTACHMENT -> {
                                    // Extract the filename from the URL, if it's not null
                                    updatedTask.attachment?.substringAfterLast("/") ?: ""
                                }

                                FIELD_COLLECTION -> updatedTask.category
                                else -> ""
                            }

                            // Call the listener with the real, extracted value
                            if (newValue != null) {
                                listener?.onFieldUpdated(fieldType, newValue)
                            }

                            Toast.makeText(
                                context, "Field updated successfully", Toast.LENGTH_SHORT
                            ).show()
                            listener?.onUpdateSuccess()
                            dismiss()
                        } else {
                            Log.e(TAG, "Update successful but no Task payload received.")
                            Toast.makeText(
                                context, "Update failed: No data returned.", Toast.LENGTH_LONG
                            ).show()
                            listener?.onUpdateError("Update successful but no data was returned.")
                        }
                    } else {
                        val errorMessage = it.exceptionOrNull()?.message ?: "Update failed"
                        val userFriendlyMessage = parseUserFriendlyErrorMessage(errorMessage)

                        Log.e(TAG, "Task update failed for field: $fieldType. Error: $errorMessage")
                        Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_LONG).show()
                        listener?.onUpdateError(errorMessage)
                    }
                }
            }
        }
    }

    private fun parseUserFriendlyErrorMessage(error: String): String {
        return try {
            val jsonObject = org.json.JSONObject(error)
            if (jsonObject.has("errors")) {
                val errors = jsonObject.getJSONObject("errors")
                val firstKey = errors.keys().next()
                val firstErrorArray = errors.getJSONArray(firstKey)
                if (firstErrorArray.length() > 0) {
                    val fieldName =
                        firstKey.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    return "$fieldName: ${firstErrorArray.getString(0)}"
                }
            }
            return jsonObject.optString(
                "detail", jsonObject.optString("message", "An unexpected error occurred.")
            )
        } catch (e: Exception) {
            return "An unexpected error occurred."
        }
    }

    private fun showLoading() {
        addButton.isEnabled = false
        addButton.text = "Updating..."
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        addButton.isEnabled = true
        addButton.text = "Update"
        progressBar.visibility = View.GONE
    }

    private fun updateTaskField(fieldName: String, fieldValue: Any) {
        if (taskId == -1) {
            val error = "Invalid task ID"
            Log.e(TAG, error)
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            listener?.onUpdateError(error)
            return
        }
        showLoading()
        Log.i(TAG, "Initiating update for task $taskId. Field: $fieldName, New Value: $fieldValue")
        // Pass the value as a String to ensure correct serialization
        taskViewModel.updateTaskField(taskId, fieldName, fieldValue.toString())
    }

    private fun setupBottomSheet() {
        Log.d(TAG, "Setting up bottom sheet for field type: $fieldType")
        when (fieldType) {
            FIELD_TITLE -> setupTitleEdit()
            FIELD_DESCRIPTION -> setupDescriptionEdit()
            FIELD_DATETIME -> setupDateTimeEdit()
            FIELD_PRIORITY -> setupPriorityEdit()
            FIELD_REPEAT -> setupRepeatEdit()
            FIELD_ATTACHMENT -> setupAttachmentEdit()
            FIELD_COLLECTION -> setupCollectionEdit()
        }
    }

    private fun setupTitleEdit() {
        titleText.text = "Edit Task Title"

        val editText = EditText(requireContext()).apply {
            hint = "Enter task title"
            setText(currentValue)
            setTextColor(resources.getColor(android.R.color.white))
            setHintTextColor(resources.getColor(R.color.dividerColor))
            setPadding(16, 16, 16, 16)
        }

        contentContainer.addView(editText)

        addButton.setThrottleClickListener {
            val newValue = editText.text.toString().trim()
            if (newValue.isNotEmpty()) {
                if (newValue != currentValue) {
                    updateTaskField("title", newValue)
                } else {
                    Log.d(TAG, "Title not changed. Dismissing.")
                    dismiss()
                }
            } else {
                Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDescriptionEdit() {
        titleText.text = "Edit Description"

        val editText = EditText(requireContext()).apply {
            hint = "Enter task description"
            setText(currentValue)
            setTextColor(resources.getColor(android.R.color.white))
            setHintTextColor(resources.getColor(R.color.dividerColor))
            setPadding(16, 16, 16, 16)
            minHeight = 120
            maxLines = 5
        }

        contentContainer.addView(editText)

        addButton.setThrottleClickListener {
            val newValue = editText.text.toString().trim()
            if (newValue != currentValue) {
                updateTaskField("description", newValue)
            } else {
                Log.d(TAG, "Description not changed. Dismissing.")
                dismiss()
            }
        }
    }

    private fun setupDateTimeEdit() {
        titleText.text = "Select Date & Time"

        val dateTimeButton = Button(requireContext()).apply {
            text = if (currentValue.isNotEmpty()) currentValue else "Select Date & Time"
            setBackgroundResource(R.drawable.button_background)
            setTextColor(resources.getColor(android.R.color.white))
            setPadding(24, 24, 24, 24)
        }

        contentContainer.addView(dateTimeButton)

        var selectedDateTime = currentValue

        dateTimeButton.setOnClickListener {
            Log.d(TAG, "Date/Time button clicked. Opening picker.")
            showDateTimePicker { formattedDateTime ->
                selectedDateTime = formattedDateTime
                dateTimeButton.text = selectedDateTime
            }
        }

        addButton.setThrottleClickListener {
            try {
                if (selectedDateTime.isNotEmpty()) {
                    if (selectedDateTime != currentValue) {
                        val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getDefault()

                        val outputFormat =
                            SimpleDateFormat("yyyy-MM-dd' 'HH:mm", Locale.getDefault())
                        outputFormat.timeZone = TimeZone.getTimeZone("UTC")

                        val parsedDate = inputFormat.parse(selectedDateTime)
                        val formattedDateTime = parsedDate?.let { outputFormat.format(it) }

                        if (formattedDateTime != null) {
                            updateTaskField("due_date", formattedDateTime)
                        } else {
                            Log.e(TAG, "Failed to parse selected date time.")
                            Toast.makeText(context, "Invalid date format", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Log.d(TAG, "Date/Time not changed. Dismissing.")
                        dismiss()
                    }
                } else {
                    Toast.makeText(context, "Please select date and time", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date time: ${e.message}", e)
                Toast.makeText(context, "Error with date/time format", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDateTimePicker(onDateTimeSelected: (String) -> Unit) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        DatePickerDialog(requireContext(), { _, y, m, d ->
            TimePickerDialog(requireContext(), { _, h, min ->
                val formatted = "%02d/%02d/%04d %02d:%02d".format(d, m + 1, y, h, min)
                onDateTimeSelected(formatted)
                Log.d(TAG, "Date/Time selected: $formatted")
            }, hour, minute, true).show()
        }, year, month, day).show()
    }

    private fun setupPriorityEdit() {
        titleText.text = "Select Priority"

        val radioGroup = RadioGroup(requireContext())
        val priorities = listOf("low", "medium", "high") // Changed to lowercase

        priorities.forEach { priority ->
            val radioButton = RadioButton(requireContext()).apply {
                text =
                    priority.replaceFirstChar { it.titlecase(Locale.getDefault()) } // Display text is capitalized
                setTextColor(resources.getColor(android.R.color.white))
                setPadding(16, 16, 16, 16)
                isChecked = priority.equals(currentValue, ignoreCase = true)
            }
            radioGroup.addView(radioButton)
        }

        contentContainer.addView(radioGroup)

        addButton.setThrottleClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedRadioButton = radioGroup.findViewById<RadioButton>(selectedId)
                val selectedPriority =
                    selectedRadioButton.text.toString().lowercase() // Send lowercase to API
                if (!selectedPriority.equals(currentValue, ignoreCase = true)) {
                    updateTaskField("priority", selectedPriority)
                } else {
                    Log.d(TAG, "Priority not changed. Dismissing.")
                    dismiss()
                }
            } else {
                Toast.makeText(context, "Please select a priority", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRepeatEdit() {
        titleText.text = "Select Repeat Option"

        val radioGroup = RadioGroup(requireContext())
        val repeatOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")

        repeatOptions.forEach { option ->
            val radioButton = RadioButton(requireContext()).apply {
                text = option
                setTextColor(resources.getColor(android.R.color.white))
                setPadding(16, 16, 16, 16)
                isChecked = option.equals(currentValue, ignoreCase = true)
            }
            radioGroup.addView(radioButton)
        }

        contentContainer.addView(radioGroup)

        addButton.setThrottleClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedRadioButton = radioGroup.findViewById<RadioButton>(selectedId)
                val selectedRepeat = selectedRadioButton.text.toString()
                if (!selectedRepeat.equals(currentValue, ignoreCase = true)) {
                    updateTaskField("repeat", selectedRepeat)
                } else {
                    Log.d(TAG, "Repeat option not changed. Dismissing.")
                    dismiss()
                }
            } else {
                Toast.makeText(context, "Please select a repeat option", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAttachmentEdit() {
        titleText.text = "Add Attachment"

        val attachmentButton = Button(requireContext()).apply {
            text = "Choose File"
            setBackgroundResource(R.drawable.button_background)
            setTextColor(resources.getColor(android.R.color.white))
            setPadding(24, 24, 24, 24)
        }

        attachmentText = TextView(requireContext()).apply {
            text = if (currentValue.isNotEmpty()) currentValue else "No file selected"
            setTextColor(resources.getColor(android.R.color.white))
            setPadding(16, 16, 16, 16)
            textSize = 16f
        }

        fileSizeText = TextView(requireContext()).apply {
            text = ""
            setTextColor(resources.getColor(R.color.dividerColor))
            setPadding(16, 8, 16, 16)
            textSize = 12f
        }

        contentContainer.addView(attachmentButton)
        contentContainer.addView(attachmentText)
        contentContainer.addView(fileSizeText)

        if (currentValue.isNotEmpty()) {
            attachmentText.text = currentValue
        }

        attachmentButton.setOnClickListener {
            launchFilePicker()
        }

        addButton.setThrottleClickListener {
            selectedFile?.let { file ->
                // The following logic is commented out because it's incorrect.
                // It sends a filename as a String in a PATCH request, which your API rejects.
                // You must first upload the file and then PATCH the URL.
                /* if (file.name != currentValue) {
                    val attachmentPart = selectedFile?.let {
                        val requestFile = it.asRequestBody("multipart/form-data".toMediaType())
                        MultipartBody.Part.createFormData("attachment", file.name, requestFile)
                    }
                    updateTaskField("attachment", file.name)
                } else {
                    Log.d(TAG, "Attachment not changed. Dismissing.")
                    dismiss()
                }
                */

                Toast.makeText(
                    context,
                    "Attachment update is not supported with this API call.",
                    Toast.LENGTH_LONG
                ).show()

            } ?: run {
                if (currentValue.isNotEmpty()) {
                    dismiss()
                } else {
                    Toast.makeText(context, "Please select a file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchFilePicker() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            filePickerLauncher.launch(Intent.createChooser(intent, "Select a file"))
            Log.d(TAG, "Launched file picker.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file picker: ${e.message}", e)
            Toast.makeText(context, "Failed to open file picker: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileName(uri)
            val file = File(requireContext().cacheDir, fileName)
            file.outputStream().use { inputStream.copyTo(it) }
            Log.d(TAG, "Converted URI to file: ${file.name}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "uriToFile error", e)
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "temp_file"
        try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex != -1) {
                    name = it.getString(nameIndex) ?: "temp_file"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name", e)
        }
        return name
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    private fun setupCollectionEdit() {
        titleText.text = "Select Collection"

        val radioGroup = RadioGroup(requireContext())
        val collections = listOf("Health", "Work", "Personal", "Study", "Finance", "Other")

        collections.forEach { collection ->
            val radioButton = RadioButton(requireContext()).apply {
                text = collection
                setTextColor(resources.getColor(android.R.color.white))
                setPadding(16, 16, 16, 16)
                isChecked = collection.equals(currentValue, ignoreCase = true)
            }
            radioGroup.addView(radioButton)
        }

        contentContainer.addView(radioGroup)

        addButton.setThrottleClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedRadioButton = radioGroup.findViewById<RadioButton>(selectedId)
                val selectedCollection = selectedRadioButton.text.toString()
                if (!selectedCollection.equals(currentValue, ignoreCase = true)) {
                    updateTaskField("category", selectedCollection)
                } else {
                    Log.d(TAG, "Collection not changed. Dismissing.")
                    dismiss()
                }
            } else {
                Toast.makeText(context, "Please select a collection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var lastClickTime = 0L

    private fun View.setThrottleClickListener(interval: Long = 1000L, onClick: (View) -> Unit) {
        setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= interval) {
                lastClickTime = currentTime
                onClick(it)
            }
        }
    }
}