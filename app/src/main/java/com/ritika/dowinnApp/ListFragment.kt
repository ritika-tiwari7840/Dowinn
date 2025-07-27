package com.ritika.dowinnApp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.ritika.dowinnApp.adapter.ListAdapter
import com.ritika.dowinnApp.api.dataclasses.ListItem
import com.ritika.dowinnApp.databinding.FragmentListBinding


class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var userSessionManager: UserSessionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentListBinding.inflate(inflater, container, false)



        return binding.root}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.imageView2.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_cardFragment)
            // Initialize NavController
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                ?.addOnSuccessListener { result ->
                    val idToken = result.token
                    Log.d("ID_TOKEN", "Token: $idToken")
                    // Use this token in your API request
                }
        }
        val recyclerView = binding.recyclerViewList
        recyclerView.layoutManager = LinearLayoutManager(requireContext()) // 👈 add thi
        val emptyViewImg = binding.imageView2
        val emptyViewMsg1 = binding.emptyStateText
        val emptyViewMsg2= binding.emptyStateText2

// Temp data (change as needed)
        val sampleData = listOf<ListItem>(
            // Uncomment to test with data
             ListItem("Item 1", "Description 1"),
             ListItem("Item 2", "Description 2"),
            ListItem("Item 1", "Description 1"),
            ListItem("Item 2", "Description 2"),
            ListItem("Item 1", "Description 1"),
            ListItem("Item 2", "Description 2"),
            ListItem("Item 1", "Description 1"),
            ListItem("Item 2", "Description 2")
        )

        val adapter = ListAdapter(sampleData)
        recyclerView.adapter = adapter

        if (sampleData.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyViewImg.visibility = View.VISIBLE
            emptyViewMsg1.visibility = View.VISIBLE
            emptyViewMsg2.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyViewImg.visibility = View.GONE
            emptyViewMsg1.visibility = View.GONE
            emptyViewMsg2.visibility = View.GONE

        }


    }


}