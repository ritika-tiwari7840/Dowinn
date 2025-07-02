package com.ritika.dowinnApp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.ritika.dowinnApp.R
import com.ritika.dowinnApp.databinding.FragmentTaskBinding

class TaskFragment : Fragment() {
    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize session manager
        sessionManager = UserSessionManager.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up listeners
        binding.signout.setOnClickListener {
            signOutUser()
        }
    }

    private fun signOutUser() {
        try {
            // Clear user session (includes Google Sign-In sign out)
            sessionManager.clearUserSession()

            // Small delay to ensure sign out completes before navigation
            Handler(Looper.getMainLooper()).postDelayed({
                // Navigate back to login/welcome screen
                findNavController().navigate(R.id.action_taskFragment_to_welcomeFragment)

                // Optional: Show toast message
                Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
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