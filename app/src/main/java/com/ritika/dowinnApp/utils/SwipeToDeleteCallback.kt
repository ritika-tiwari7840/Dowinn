package com.ritika.dowinnApp.utils

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ritika.dowinnApp.R
import kotlin.math.max
import android.os.Handler
import android.os.Looper

class SwipeToDeleteCallback(
    private val context: Context,
    private val onDeleteIconClicked: (position: Int) -> Unit,
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    companion object {
        private const val TAG = "SwipeToDelete"
        private const val REVEAL_PERCENTAGE = 0.20f
        private const val RESET_DELAY_MS = 1000L // 1 seconds
    }

    private val handler = Handler(Looper.getMainLooper())
    private var resetRunnable: Runnable? = null
    var swipedPosition: Int = RecyclerView.NO_POSITION

    override fun isItemViewSwipeEnabled(): Boolean = swipedPosition == RecyclerView.NO_POSITION

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 1.1f

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        swipedPosition = position
        onDeleteIconClicked(position)

        // Refresh item to clear swipe state
        (viewHolder.itemView.parent as? RecyclerView)?.adapter?.notifyItemChanged(position)
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
        with(viewHolder.itemView) {
            val foregroundView = findViewById<View>(R.id.foregroundCard)
            val backgroundView = findViewById<View>(R.id.deleteIconBackground)

            val maxSwipeDistance = -width * REVEAL_PERCENTAGE
            val limitedDx = max(maxSwipeDistance, dX)

            when {
                swipedPosition == RecyclerView.NO_POSITION && limitedDx < 0f -> {
                    foregroundView.translationX = limitedDx
                    foregroundView.isClickable = false
                }

                swipedPosition != RecyclerView.NO_POSITION -> {
                    foregroundView.translationX = maxSwipeDistance
                    foregroundView.isClickable = false

                    backgroundView.apply {
                        if (!isClickable) {
                            setThrottleClickListener {
                                Log.d(TAG, "Delete icon clicked at $swipedPosition")
                                onDeleteIconClicked(swipedPosition)
                                swipedPosition = RecyclerView.NO_POSITION
                                cancelAutoReset()
                            }
                        }
                        isClickable = true
                    }

                    // Start auto-reset timer
                    scheduleAutoReset(recyclerView)
                }

                else -> {
                    foregroundView.translationX = 0f
                    foregroundView.isClickable = true
                    backgroundView.isClickable = false
                }
            }
        }
    }

    private var lastClickTimeFab = 0L

    fun View.setThrottleClickListener(interval: Long = 1000L, onClick: (View) -> Unit) {
        setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTimeFab >= interval) {
                lastClickTimeFab = currentTime
                onClick(it)
            }
        }
    }

    fun resetSwipedItem(recyclerView: RecyclerView) {
        if (swipedPosition == RecyclerView.NO_POSITION) return

        recyclerView.findViewHolderForAdapterPosition(swipedPosition)?.itemView?.let { itemView ->
            val foregroundView = itemView.findViewById<View>(R.id.foregroundCard)

            foregroundView.animate().translationX(0f).setDuration(200).withEndAction {
                foregroundView.isClickable = true
            }.start()
        }
        swipedPosition = RecyclerView.NO_POSITION
        cancelAutoReset()
    }

    private fun scheduleAutoReset(recyclerView: RecyclerView) {
        cancelAutoReset()
        resetRunnable = Runnable {
            resetSwipedItem(recyclerView)
        }
        handler.postDelayed(resetRunnable!!, RESET_DELAY_MS)
    }

    private fun cancelAutoReset() {
        resetRunnable?.let { handler.removeCallbacks(it) }
        resetRunnable = null
    }
}
