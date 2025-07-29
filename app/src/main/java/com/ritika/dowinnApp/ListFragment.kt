package com.ritika.dowinnApp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ritika.dowinnApp.adapter.ListAdapter
import com.ritika.dowinnApp.api.dataclasses.ListItem
import com.ritika.dowinnApp.databinding.FragmentListBinding
import com.ritika.voy.util.swipe.SwipeToDeleteCallback

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var userSessionManager: UserSessionManager
    private lateinit var listAdapter: ListAdapter
    private val dataList = mutableListOf<ListItem>()
    private lateinit var swipeToDeleteCallback: SwipeToDeleteCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageView2.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_cardFragment)
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                ?.addOnSuccessListener { result ->
                    val idToken = result.token
                    Log.d("ID_TOKEN", "Token: $idToken")
                }
        }

        dataList.addAll(
            listOf(
                ListItem("Item 1", "Dascription 1"),
                ListItem("Item 2", "Description 2"),
                ListItem("Item 3", "Description 3"),
                ListItem("Item 4", "Description 4"),
                ListItem("Item 5", "Description 5")
            )
        )

        // Initialize ListAdapter with the new click listener for the delete icon
        listAdapter = ListAdapter(dataList) { position ->
            showDeleteConfirmationDialog(position)
        }

        binding.recyclerViewList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
        }
        swipeToDeleteCallback = SwipeToDeleteCallback(requireContext()) { position ->
            showDeleteConfirmationDialog(position)
        }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewList)


        // Add a scroll listener to reset any open swiped items when scrolling starts
        binding.recyclerViewList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    swipeToDeleteCallback.resetSwipedItem(recyclerView)
                }
            }
        })

        listAdapter.setOnDeleteItemCallback { position ->
            Log.d("ListFragment", "Item deleted at position: $position")
            checkEmptyState()
        }

        checkEmptyState()
    }

    /**
     * Shows a confirmation dialog before deleting an item. If confirmed,
     * deletes the item. If cancelled, resets the item's swipe position.
     */
    private fun showDeleteConfirmationDialog(position: Int) {
        Log.d("DeleteConfirm", "Showing delete confirmation for position: $position")
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Delete") { dialog, _ ->
                listAdapter.deleteItem(position)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                val viewHolder = binding.recyclerViewList.findViewHolderForAdapterPosition(position)
                viewHolder?.itemView?.findViewById<View>(R.id.foregroundCard)?.animate()
                    ?.translationX(0f)?.setDuration(200)?.start()
                swipeToDeleteCallback.resetSwipedItem(binding.recyclerViewList)


                dialog.dismiss()
            }
            .show()
    }

    private fun checkEmptyState() {
        if (dataList.isEmpty()) {
            binding.recyclerViewList.visibility = View.GONE
            binding.imageView2.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.VISIBLE
            binding.emptyStateText2.visibility = View.VISIBLE
        } else {
            binding.recyclerViewList.visibility = View.VISIBLE
            binding.imageView2.visibility = View.GONE
            binding.emptyStateText.visibility = View.GONE
            binding.emptyStateText2.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
