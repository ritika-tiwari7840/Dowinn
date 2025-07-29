package com.ritika.voy.util.swipe

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ritika.dowinnApp.R
import kotlin.math.max

class SwipeToDeleteCallback(
    private val context: Context,
    private val onDeleteIconClicked: (position: Int) -> Unit,
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    companion object {
        private const val TAG = "SwipeToDelete"
        private const val REVEAL_PERCENTAGE = 0.20f
    }

    var swipedPosition: Int = RecyclerView.NO_POSITION

    override fun isItemViewSwipeEnabled(): Boolean {
        // Disable swipe if one item is already swiped
        return swipedPosition == RecyclerView.NO_POSITION
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        // Prevent full swipe delete
        return 1.1f
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        swipedPosition = viewHolder.adapterPosition
        Log.d(TAG, "Item swiped at $swipedPosition")
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
        val itemView = viewHolder.itemView
        val foregroundView = itemView.findViewById<View>(R.id.foregroundCard)
        val backgroundView = itemView.findViewById<View>(R.id.deleteIconBackground)

        val maxSwipeDistance = -itemView.width * REVEAL_PERCENTAGE
        val limitedDx = max(maxSwipeDistance, dX)

        if (swipedPosition == RecyclerView.NO_POSITION && limitedDx < 0f) {
            foregroundView.translationX = limitedDx
            foregroundView.isClickable = false
            Log.d(TAG, "Swiping: dx=$limitedDx")
        } else if (swipedPosition != RecyclerView.NO_POSITION) {
            foregroundView.translationX = maxSwipeDistance
            foregroundView.isClickable = false
            Log.d(TAG, "Swipe locked")

            // ✅ Make background clickable and trigger delete
            backgroundView.setOnClickListener {
                Log.d(TAG, "Delete icon clicked at $swipedPosition")
                onDeleteIconClicked(swipedPosition)
                swipedPosition = RecyclerView.NO_POSITION
            }
            backgroundView.isClickable = true
        } else {
            foregroundView.translationX = 0f
            foregroundView.isClickable = true
            backgroundView.isClickable = false
        }
    }


    fun resetSwipedItem(recyclerView: RecyclerView) {
        if (swipedPosition != RecyclerView.NO_POSITION) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(swipedPosition)
            viewHolder?.itemView?.let { itemView ->
                val foregroundView = itemView.findViewById<View>(R.id.foregroundCard)

                foregroundView.animate()
                    .translationX(0f)
                    .setDuration(200)
                    .withEndAction {
                        foregroundView.isClickable = true
                    }
                    .start()
            }
            swipedPosition = RecyclerView.NO_POSITION
        }
    }
}
