package com.ritika.dowinnApp.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.ritika.dowinnApp.R
import com.ritika.dowinnApp.utils.UserSessionManager
import com.ritika.dowinnApp.databinding.FragmentTaskBinding

class TaskFragment : Fragment() {
    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = UserSessionManager.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.navigationIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.svgrepo_iconcarrier)

        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.fab.setThrottleClickListener {
            AddTaskFragment().show(parentFragmentManager, "AddTaskFragment")
        }


        // Handle nav drawer menu click
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_tasks -> {
                    Toast.makeText(context, "Home clicked", Toast.LENGTH_SHORT).show()
                }

                R.id.nav_logout -> {
                    signOutUser()
                }
                // Add other cases if needed
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }


        val navView = view.findViewById<NavigationView>(R.id.navigationView)
        val headerView = navView.getHeaderView(0)

        val userName = headerView.findViewById<TextView>(R.id.user_name)
        val email = headerView.findViewById<TextView>(R.id.user_email)
        val profileImage = headerView.findViewById<ImageView>(R.id.profileImage)

        val user = FirebaseAuth.getInstance().currentUser

        userName.text = user?.displayName ?: "No name available"
        email.text = user?.email ?: "No email available"

        Glide.with(this).load(user?.photoUrl)  // Use Firebase user photo
            .placeholder(R.drawable.circle_background)
            .error(R.drawable.baseline_supervised_user_circle_24).circleCrop().into(profileImage)
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

    private fun signOutUser() {
        try {
            sessionManager.clearUserSession()
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().navigate(R.id.action_taskFragment_to_welcomeFragment)
                Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT)
                    .show()
            }, 500)
        } catch (e: Exception) {
            Log.e("TaskFragment", "Error during sign out: ${e.message}")
            Toast.makeText(requireContext(), "Sign out failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}