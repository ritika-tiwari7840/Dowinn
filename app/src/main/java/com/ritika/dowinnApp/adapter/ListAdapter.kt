package com.ritika.dowinnApp.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
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

    private val items = mutableListOf<DisplayItem>()
    private var onDeleteItemCallback: ((Int) -> Unit)? = null

    fun setOnDeleteItemCallback(listener: (Int) -> Unit) {
        onDeleteItemCallback = listener
    }

    fun deleteItem(position: Int) {
        if (position in items.indices && items[position] is DisplayItem.TaskItem) {
            items.removeAt(position)
            notifyItemRemoved(position)
            onDeleteItemCallback?.invoke(position)
        }
    }

    fun updateData(newTasks: List<Task>) {
        val grouped = groupTasksByDate(newTasks)
        items.clear()
        items.addAll(grouped)
        notifyDataSetChanged()
    }

    fun getTaskAt(position: Int): Task? {
        return (items.getOrNull(position) as? DisplayItem.TaskItem)?.task
    }

    fun isTaskItem(position: Int): Boolean {
        return items.getOrNull(position) is DisplayItem.TaskItem
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DisplayItem.DateHeader -> 0
            is DisplayItem.TaskItem -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val textView = TextView(parent.context).apply {
                setPadding(32, 24, 16, 8)
                textSize = 18f
            }
            HeaderViewHolder(textView)
        } else {
            val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            TaskViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DisplayItem.DateHeader -> (holder as HeaderViewHolder).bind(item.label)
            is DisplayItem.TaskItem -> (holder as TaskViewHolder).bind(item.task)
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

        fun bind(task: Task) {
            binding.titleText.text = task.title
            binding.descriptionText.text = task.description
//            binding.dueDateText.text = formatDate(task.due_date)

            binding.foregroundCard.isClickable = true
            binding.deleteIconBackground.setOnClickListener {
                onDeleteIconClick.invoke(adapterPosition)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDate(dateString: String?): String {
        return try {
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

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val endOfWeek = today.with(DayOfWeek.SUNDAY)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

        val sortedTasks = tasks.sortedBy {
            try {
                LocalDate.parse(it.due_date, formatter)
            } catch (e: Exception) {
                today.plusYears(100) // Push invalid dates to the end
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
                    else -> "Upcoming"
                }

                if (headerLabel != currentHeader) {
                    groupedTasks.add(DisplayItem.DateHeader(headerLabel))
                    currentHeader = headerLabel
                }

                groupedTasks.add(DisplayItem.TaskItem(task))
            } catch (e: Exception) {
                // Optional: handle tasks with invalid or null date
                e.printStackTrace()
            }
        }

        return groupedTasks
    }

}
