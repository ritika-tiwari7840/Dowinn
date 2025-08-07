package com.ritika.dowinnApp.fragment

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
        setupRecyclerView()
        observeTasks()
        loadTasksFromApi()
    }

    private fun observeTasks() {
        taskViewModel.tasks.observe(viewLifecycleOwner) { updatedTasks ->
            taskList.clear()
            taskList.addAll(updatedTasks)
            listAdapter.updateData(updatedTasks)
            checkEmptyState()
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
        listAdapter = ListAdapter { position ->
            showDeleteConfirmationDialog(position)
        }

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
    }

    private fun loadTasksFromApi() {
        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Loading tasks...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getTasks()
                progressDialog.dismiss()

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val tasks = apiResponse?.payload ?: emptyList()
                    Log.d("ListFragment", "loadTasksFromApi: $tasks")
                    taskList.clear()
                    taskList.addAll(tasks)
                    listAdapter.updateData(tasks)
                    checkEmptyState()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = parseErrorMessage(errorBody)
                    Log.e("ListFragment", "Error loading tasks: $message")
                    Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e("ListFragment", "Error loading tasks", e)
                Toast.makeText(
                    context,
                    "Unexpected Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun addTask(task: Task) {
        taskList.add(0, task)
        listAdapter.updateData(taskList)
        checkEmptyState()
        binding.recyclerViewList.scrollToPosition(0)
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
                    }
                    result.onFailure { error ->
                        Log.e("ListFragment", "Error deleting task", error)
                        Toast.makeText(
                            requireContext(),
                            "Error: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        val viewHolder =
                            binding.recyclerViewList.findViewHolderForAdapterPosition(position)
                        viewHolder?.itemView?.findViewById<View>(R.id.foregroundCard)
                            ?.animate()?.translationX(0f)?.setDuration(200)?.start()
                        swipeToDeleteCallback.resetSwipedItem(binding.recyclerViewList)
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                val viewHolder = binding.recyclerViewList.findViewHolderForAdapterPosition(position)
                viewHolder?.itemView?.findViewById<View>(R.id.foregroundCard)
                    ?.animate()?.translationX(0f)?.setDuration(200)?.start()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
