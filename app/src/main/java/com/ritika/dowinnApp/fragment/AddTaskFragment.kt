package com.ritika.dowinnApp.fragment

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.net.http.HttpException
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.ritika.dowinnApp.utils.KeyboardUtils
import com.ritika.dowinnApp.R
import com.ritika.dowinnApp.api.RetrofitClient.apiService
import com.ritika.dowinnApp.api.dataclasses.Task
import com.ritika.dowinnApp.databinding.FragmentAddTaskBinding
import com.ritika.dowinnApp.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import kotlin.math.log

class AddTaskFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskViewModel: TaskViewModel
    private val calendar: Calendar = Calendar.getInstance()
    private var selectedFile: File? = null

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedFile = uriToFile(uri)
                    selectedFile?.let {
                        binding.attachmentLayout.hint = it.name
                        Toast.makeText(requireContext(), "File selected: ${it.name}", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Toast.makeText(requireContext(), "Failed to select file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taskViewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]

        setupDropdown(binding.etPriority, listOf("High", "Medium", "Low"))
        setupDropdown(binding.collectionDropdown, listOf("Health", "Personal", "Work", "Study", "Finance", "Other"))

        expandBottomSheetOnShow()

        binding.etDateTime.setOnClickListener { showDateTimePicker() }
        binding.etAttachments.setOnClickListener { launchFilePicker() }
        binding.btnAddTask.setOnClickListener { validateAndCreateTask() }
    }

    private fun expandBottomSheetOnShow() {
        dialog?.setOnShowListener { dlg ->
            val bottomSheet = (dlg as? com.google.android.material.bottomsheet.BottomSheetDialog)
                ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let {
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(it).apply {
                    state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                    isDraggable = true
                }
            }
        }
    }

    private fun showDateTimePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        DatePickerDialog(requireContext(), { _, y, m, d ->
            TimePickerDialog(requireContext(), { _, h, min ->
                val formatted = "%02d/%02d/%04d %02d:%02d".format(d, m + 1, y, h, min)
                binding.etDateTime.setText(formatted)
            }, hour, minute, true).show()
        }, year, month, day).show()
    }

    private fun setupDropdown(view: AutoCompleteTextView, items: List<String>) {
        view.setAdapter(ArrayAdapter(requireContext(), R.layout.dropdown_item, items))
        view.setOnClickListener { view.showDropDown() }
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(Intent.createChooser(intent, "Select a file"))
    }

    private fun validateAndCreateTask() {
        val title = binding.etTaskName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val collection = binding.collectionDropdown.text.toString().lowercase()
        val dateTime = binding.etDateTime.text.toString()
        val priority = binding.etPriority.text.toString().lowercase()

        if (title.isEmpty() || description.isEmpty() || collection.isEmpty() || dateTime.isEmpty() || priority.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }else{

            createTask(title, description, collection, dateTime, priority, selectedFile)

        }
    }

    private fun createTask(
        title: String,
        description: String,
        collection: String,
        dateTime: String,
        priority: String,
        file: File?
    ) {
        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Creating task...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {

                val response = taskViewModel.repository.createTask(
                    title = title,
                    description = description,
                    category = collection,
                    priority = priority,
                    dueDate = dateTime,
                    attachmentFile = file
                )


                progressDialog.dismiss()

                if (response.isSuccessful) {
                    Toast.makeText(context, "Task created successfully", Toast.LENGTH_SHORT).show()
                    taskViewModel.loadTasks() // Refresh the task list
                    dismiss()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = parseErrorMessage(errorBody)
                    Log.e("AddTaskFragment", "createTask error: $message")
                    Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e("AddTaskFragment", "createTask error", e)
                Toast.makeText(context, "Unexpected Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
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

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileName(uri)
            val file = File(requireContext().cacheDir, fileName)
            file.outputStream().use { inputStream.copyTo(it) }
            file
        } catch (e: Exception) {
            Log.e("AddTaskFragment", "uriToFile error", e)
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "temp_file"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
