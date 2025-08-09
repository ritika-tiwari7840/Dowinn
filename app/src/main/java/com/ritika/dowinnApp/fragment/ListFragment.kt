package com.ritika.dowinnApp.fragment

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ritika.dowinnApp.R
import com.ritika.dowinnApp.adapter.ListAdapter
import com.ritika.dowinnApp.api.RetrofitClient
import com.ritika.dowinnApp.api.dataclasses.Task
import com.ritika.dowinnApp.databinding.FragmentListBinding
import com.ritika.dowinnApp.utils.SwipeToDeleteCallback
import com.ritika.dowinnApp.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException


class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var listAdapter: ListAdapter
    private val taskList = mutableListOf<Task>()
    private lateinit var swipeToDeleteCallback: SwipeToDeleteCallback
    private val taskViewModel: TaskViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
        setupRecyclerView()
        observeTasks()
        startShimmerLoading()
        loadTasksFromApi()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeTasks() {
        try {
            taskViewModel.tasks.observe(viewLifecycleOwner) { updatedTasks ->
                taskList.clear()
                taskList.addAll(updatedTasks)
                listAdapter.updateData(updatedTasks)
                checkEmptyState()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupNavigation() {
        binding.imageView2.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_cardFragment)
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                ?.addOnSuccessListener { result ->
                    Log.d("ID_TOKEN", "Token: ${result.token}")
                }
        }
    }

    private fun setupRecyclerView() {
        try {
            listAdapter = ListAdapter { position ->
                showDeleteConfirmationDialog(position)
            }

            listAdapter.setShimmerComponents(binding.shimmerLayout, binding.recyclerViewList, binding.emptyStateText)

            binding.recyclerViewList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = listAdapter
            }

            swipeToDeleteCallback = SwipeToDeleteCallback(requireContext()) { position ->
                if (listAdapter.isTaskItem(position)) {
                    showDeleteConfirmationDialog(position)
                } else {
                    listAdapter.notifyItemChanged(position)
                }
            }

            ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(binding.recyclerViewList)

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTasksFromApi() {
        startShimmerLoading()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getTasks()

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val tasks = apiResponse?.payload ?: emptyList()
                    taskList.clear()
                    taskList.addAll(tasks)
                    stopShimmerLoading()
                    listAdapter.updateData(tasks)
                    checkEmptyState()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = parseErrorMessage(errorBody)
                    Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ListFragment", "Error loading tasks", e)
                Toast.makeText(context, "Unexpected Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addTask(task: Task) {
        try {
            taskList.add(0, task)
            listAdapter.updateData(taskList)
            checkEmptyState()
            binding.recyclerViewList.scrollToPosition(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        val taskId = listAdapter.getTaskAt(position)?.id ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { dialog, _ ->
                taskViewModel.deleteTask(taskId.toInt())

                taskViewModel.deleteResult.observe(viewLifecycleOwner) { result ->
                    result.onSuccess { message ->
                        listAdapter.deleteItem(position)
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        Log.d("ListFragment", "showDeleteConfirmationDialog: $message")
                    }


                    result.onFailure { throwable ->
                        // Try to parse API error body if available
                        val friendlyMessage = when (throwable) {
                            is HttpException -> {
                                val errorBody = throwable.response()?.errorBody()?.string()
                                parseErrorMessage(errorBody)
                            }
                            else -> throwable.message ?: "Something went wrong. Please try again."
                        }

                        Toast.makeText(requireContext(), friendlyMessage, Toast.LENGTH_LONG).show()

                        // Reset swiped item so UI doesn't get stuck
                        val viewHolder = binding.recyclerViewList.findViewHolderForAdapterPosition(position)
                        viewHolder?.itemView
                            ?.findViewById<View>(R.id.foregroundCard)
                            ?.animate()
                            ?.translationX(0f)
                            ?.setDuration(200)
                            ?.start()

                        swipeToDeleteCallback.resetSwipedItem(binding.recyclerViewList)
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                val viewHolder = binding.recyclerViewList.findViewHolderForAdapterPosition(position)
                viewHolder?.itemView?.findViewById<View>(R.id.foregroundCard)?.animate()?.translationX(0f)?.setDuration(200)?.start()
                swipeToDeleteCallback.resetSwipedItem(binding.recyclerViewList)
                dialog.dismiss()
            }
            .show()
    }

    private fun checkEmptyState() {
        val isEmpty = listAdapter.itemCount == 0
        binding.recyclerViewList.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.imageView2.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.emptyStateText2.visibility = if (isEmpty) View.VISIBLE else View.GONE
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

    fun startShimmerLoading() {
        try {
            binding.shimmerLayout.visibility = View.VISIBLE
            binding.shimmerLayout.startShimmer()

            binding.recyclerViewList.visibility = View.GONE
            binding.emptyStateText.visibility = View.GONE
            binding.emptyStateText2.visibility = View.GONE
            binding.imageView2.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun stopShimmerLoading() {
        try {
            binding.shimmerLayout.visibility = View.GONE
            binding.shimmerLayout.stopShimmer()

            binding.recyclerViewList.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
