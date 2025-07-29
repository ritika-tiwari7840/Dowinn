package com.ritika.dowinnApp.adapter

import android.util.Log // Import Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ritika.dowinnApp.api.dataclasses.ListItem
import com.ritika.dowinnApp.databinding.ItemListBinding

class ListAdapter(
    private val items: MutableList<ListItem>,
    private val onDeleteIconClick: (Int) -> Unit, // Callback for when the delete icon is clicked
) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    private var onDeleteItemCallback: ((Int) -> Unit)? = null

    fun setOnDeleteItemCallback(listener: (Int) -> Unit) {
        onDeleteItemCallback = listener
    }

    fun deleteItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            onDeleteItemCallback?.invoke(position) // Invoke callback after item is removed
        }
    }

    inner class ListViewHolder(val binding: ItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.foregroundCard.setOnClickListener {
                if (binding.foregroundCard.translationX != 0f) {
                    // Ignore clicks while swiped open
                    binding.deleteIconBackground.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                // Handle normal item click if needed
            }

            // Set click listener for the delete icon
            binding.deleteIconBackground.setOnClickListener {
                Log.d("DeleteClick", "Delete icon clicked for position: $adapterPosition")
                onDeleteIconClick.invoke(adapterPosition)
            }

        }

        fun bind(item: ListItem) {
            binding.titleText.text = item.title
            binding.descriptionText.text = item.description
            // Ensure the foreground card is reset to its original position when bound
            // This is important for recycled views
//            binding.foregroundCard.translationX = 0f
            binding.foregroundCard.isClickable = true // Ensure it's clickable by default
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
