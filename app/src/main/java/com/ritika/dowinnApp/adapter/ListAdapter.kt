package com.ritika.dowinnApp.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.ritika.dowinnApp.R
import com.ritika.dowinnApp.api.dataclasses.Task
import com.ritika.dowinnApp.databinding.ItemListBinding
import com.ritika.dowinnApp.utils.DisplayItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class ListAdapter(
    private val onDeleteIconClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var shimmerLayout: ShimmerFrameLayout? = null
    private var recyclerView: RecyclerView? = null
    private var emptyTextView: TextView? = null
    private val items = mutableListOf<DisplayItem>()
    private var onDeleteItemCallback: ((Int) -> Unit)? = null
    private var onEmptyStateCallback: (() -> Unit)? = null
    private  var navController: androidx.navigation.NavController? = null

    fun setOnDeleteItemCallback(listener: (Int) -> Unit) {
        onDeleteItemCallback = listener
    }

    fun setOnEmptyStateCallback(listener: () -> Unit) {
        onEmptyStateCallback = listener
    }
    fun setShimmerComponents(
        shimmerLayout: ShimmerFrameLayout,
        recyclerView: RecyclerView,
        emptyTextView: TextView
    ) {
        this.shimmerLayout = shimmerLayout
        this.recyclerView = recyclerView
        this.emptyTextView = emptyTextView
    }

    fun deleteItem(position: Int) {
        try {
            if (position in items.indices && items[position] is DisplayItem.TaskItem) {
                val deletedItem = items[position]
                items.removeAt(position)
                notifyItemRemoved(position)

                // Check if we need to remove the header for this section
                checkAndRemoveEmptyHeader(position)

                // Check if RecyclerView is empty and call empty state
                checkEmptyState()

                onDeleteItemCallback?.invoke(position)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndRemoveEmptyHeader(deletedPosition: Int) {
        try {
            // Find the header for the deleted item by looking backwards
            var headerPosition = -1
            for (i in (deletedPosition - 1) downTo 0) {
                if (items[i] is DisplayItem.DateHeader) {
                    headerPosition = i
                    break
                }
            }

            if (headerPosition != -1) {
                // Check if there are any task items after this header
                val hasTasksAfterHeader = hasTaskItemsAfterHeader(headerPosition)

                if (!hasTasksAfterHeader) {
                    // Remove the header as it has no associated tasks
                    items.removeAt(headerPosition)
                    notifyItemRemoved(headerPosition)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hasTaskItemsAfterHeader(headerPosition: Int): Boolean {
        try {
            // Look for task items after this header until we find another header or reach the end
            for (i in (headerPosition + 1) until items.size) {
                when (items[i]) {
                    is DisplayItem.TaskItem -> return true
                    is DisplayItem.DateHeader -> return false // Found next header, no tasks in this section
                }
            }
            return false // Reached end of list, no tasks found
        } catch (e: Exception) {
            return false
        }
    }

    private fun checkEmptyState() {
        try {
            // Check if there are any task items left
            val hasAnyTasks = items.any { it is DisplayItem.TaskItem }
            if (!hasAnyTasks) {
                // Clear all headers as well since there are no tasks
                items.clear()
                notifyDataSetChanged()
                onEmptyStateCallback?.invoke()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateData(newTasks: List<Task>) {
        try {
            val grouped = groupTasksByDate(newTasks)
            items.clear()
            items.addAll(grouped)
            notifyDataSetChanged()

            // Check empty state after updating data
            checkEmptyState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTaskAt(position: Int): Task? {
        return try {
            (items.getOrNull(position) as? DisplayItem.TaskItem)?.task
        } catch (e: Exception) {
            null
        }
    }

    fun isTaskItem(position: Int): Boolean {
        return try {
            items.getOrNull(position) is DisplayItem.TaskItem
        } catch (e: Exception) {
            false
        }
    }

    fun isEmpty(): Boolean {
        return items.isEmpty() || items.none { it is DisplayItem.TaskItem }
    }

    override fun getItemViewType(position: Int): Int {
        return try {
            when (items[position]) {
                is DisplayItem.DateHeader -> 0
                is DisplayItem.TaskItem -> 1
                else -> -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return try {
            if (viewType == 0) {
                val textView = TextView(parent.context).apply {
                    setPadding(32, 24, 16, 18)
                    setTextAppearance(R.style.TextAppearance_Dowinn_Toolbar_Subtitle)
                    textSize = 18f
                    setTextColor(parent.context.getColor(R.color.white))
                }
                HeaderViewHolder(textView)
            } else {
                val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TaskViewHolder(binding)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to create ViewHolder", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            when (val item = items[position]) {
                is DisplayItem.DateHeader -> (holder as HeaderViewHolder).bind(item.label)
                is DisplayItem.TaskItem -> (holder as TaskViewHolder).bind(item.task)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(private val view: TextView) : RecyclerView.ViewHolder(view) {
        fun bind(label: String) {
            view.text = label
        }
    }

    inner class TaskViewHolder(private val binding: ItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.foregroundCard.setOnClickListener {
                navController = binding.root.findNavController()
                try {
                    if (navController != null) {
                        val task = getTaskAt(adapterPosition)
                        if (task != null) {
                            // Navigate to the task details screen
                             navController?.navigate(R.id.action_listFragment_to_detailsFragment, bundleOf("task" to task))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(task: Task) {

            try {
                binding.titleText.text = task.title ?: "No Title"
                binding.descriptionText.text = task.description ?: "No Description"
                binding.dueDateText.text = task.due_date

                binding.foregroundCard.isClickable = true
                binding.deleteIconBackground.setOnClickListener {
                    try {
                        onDeleteIconClick.invoke(adapterPosition)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDate(dateString: String?): String {
        return try {
            if (dateString.isNullOrBlank()) return "No Date"
            val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
            val date = LocalDate.parse(dateString, inputFormat)
            val outputFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
            date.format(outputFormat)
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun groupTasksByDate(tasks: List<Task>): List<DisplayItem> {
        val groupedTasks = mutableListOf<DisplayItem>()

        try {
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val endOfWeek = today.with(DayOfWeek.SUNDAY)

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

            val sortedTasks = tasks.sortedBy {
                try {
                    LocalDate.parse(it.due_date, formatter)
                } catch (e: Exception) {
                    today.plusYears(100)
                }
            }

            var currentHeader: String? = null

            for (task in sortedTasks) {
                try {
                    val taskDate = LocalDate.parse(task.due_date, formatter)
                    val headerLabel = when {
                        taskDate == today -> "Today"
                        taskDate == tomorrow -> "Tomorrow"
                        taskDate <= endOfWeek -> "This Week"
                        taskDate.isBefore(today) -> "Overdue"
                        else -> "Upcoming"
                    }

                    if (headerLabel != currentHeader) {
                        groupedTasks.add(DisplayItem.DateHeader(headerLabel))
                        currentHeader = headerLabel
                    }

                    groupedTasks.add(DisplayItem.TaskItem(task))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return groupedTasks
    }
}