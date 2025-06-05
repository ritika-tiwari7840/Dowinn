package com.ritika.dowinn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ritika.dowinn.R
import com.ritika.dowinn.api.dataclasses.WalkthroughItem

class WalkthroughAdapter(
    private val items: List<WalkthroughItem>,
) : RecyclerView.Adapter<WalkthroughAdapter.WalkthroughViewHolder>() {

    inner class WalkthroughViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.findViewById<ImageView>(R.id.imageView)
        private val titleView = itemView.findViewById<TextView>(R.id.titleView)
        private val descriptionView = itemView.findViewById<TextView>(R.id.descriptionView)

        fun bind(item: WalkthroughItem) {
            imageView.setImageResource(item.imageResId)
            titleView.text = item.title
            descriptionView.text = item.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkthroughViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_walkthrough, parent, false)
        return WalkthroughViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalkthroughViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
