package com.ritika.dowinnApp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ritika.dowinnApp.api.dataclasses.Task
import com.ritika.dowinnApp.databinding.ItemListBinding

class ListAdapter(
    private val items: MutableList<Task>,
    private val onDeleteIconClick: (Int) -> Unit
) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    private var onDeleteItemCallback: ((Int) -> Unit)? = null

    fun setOnDeleteItemCallback(listener: (Int) -> Unit) {
        onDeleteItemCallback = listener
    }

    fun deleteItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            onDeleteItemCallback?.invoke(position)
        }
    }
    fun updateData(newTasks: List<Task>) {
        items.clear()
        items.addAll(newTasks)
        notifyDataSetChanged()
    }


    inner class ListViewHolder(val binding: ItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.foregroundCard.setOnClickListener {
                if (binding.foregroundCard.translationX != 0f) {
                    binding.deleteIconBackground.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                // Optional: handle item click
            }

            binding.deleteIconBackground.setOnClickListener {
                Log.d("DeleteClick", "Delete icon clicked for position: $adapterPosition")
                onDeleteIconClick.invoke(adapterPosition)
            }
        }

        fun bind(task: Task) {
            binding.titleText.text = task.title
            binding.descriptionText.text = task.description
//            binding.priorityText.text = task.priority
//            binding.categoryText.text = task.category
//            binding.dueDateText.text = task.due_date ?: "No due date"
            binding.foregroundCard.isClickable = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
