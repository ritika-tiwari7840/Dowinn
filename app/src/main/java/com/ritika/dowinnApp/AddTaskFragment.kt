package com.ritika.dowinnApp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ritika.dowinnApp.databinding.FragmentAddTaskBinding
import java.util.Calendar

class AddTaskFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!
    lateinit var keyboardUtils: KeyboardUtils

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        val scrollView = binding.scrollView
        keyboardUtils = KeyboardUtils(scrollView.id)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdown(binding.etPriority, listOf("High", "Medium", "Low"))
        setupDropdown(binding.collectionDropdown, listOf("Health","Personal", "Work", "Study", "Finance", "Other"))

        dialog?.setOnShowListener { dialog ->
            val bottomSheet = (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)
                ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let {
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = true
            }
        }

        binding.etDateTime.setOnClickListener {
            // Open Date Picker
            val currentDate = Calendar.getInstance()

            val year = currentDate.get(Calendar.YEAR)
            val month = currentDate.get(Calendar.MONTH)
            val day = currentDate.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                // After selecting date, open Time Picker
                TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                    // Format date and time
                    val formattedDateTime = String.format(
                        "%02d/%02d/%04d %02d:%02d",
                        selectedDay, selectedMonth + 1, selectedYear, hourOfDay, minute
                    )
                    binding.etDateTime.setText(formattedDateTime)
                },
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    true
                ).show()
            }, year, month, day).show()
        }

    }


    private fun setupDropdown(view: AutoCompleteTextView, items: List<String>) {
        view.setAdapter(
            ArrayAdapter(requireContext(), R.layout.dropdown_item, items)
        )
        view.setOnClickListener { view.showDropDown() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
