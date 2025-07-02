package com.ritika.dowinnApp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.divider.MaterialDivider
import com.ritika.dowinnApp.adapter.WalkthroughAdapter
import com.ritika.dowinnApp.api.dataclasses.WalkthroughItem
import com.ritika.dowinnApp.databinding.FragmentSliderBinding

class SliderFragment : Fragment() {

    private var _binding: FragmentSliderBinding? = null
    private val binding get() = _binding!!
    private lateinit var dividerList: List<MaterialDivider>
    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize session manager
        sessionManager = UserSessionManager.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSliderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if user is signed in, if not redirect to login
        if (!sessionManager.isUserSignedIn()) {
            findNavController().navigate(R.id.action_sliderFragment_to_welcomeFragment)
            return
        }

        val walkthroughItems = listOf(
            WalkthroughItem(
                R.drawable.slider_1,
                "Smart Task Management",
                "Organize your day effortlessly with a beautifully simple interface. Create, edit, and check off tasks with ease."
            ),
            WalkthroughItem(
                R.drawable.slider_2,
                "Focus & Productivity",
                "Stay focused and boost productivity by breaking your work into manageable steps and setting clear goals."
            ),
            WalkthroughItem(
                R.drawable.slider_3,
                "Daily Habit Tracking",
                "Track habits like workouts or morning routines to build consistency and stay motivated every day."
            ),
            WalkthroughItem(
                R.drawable.slider_4,
                "Make Progress Fun",
                "Enjoy ticking off tasks, keep your streak alive, and get rewarded for staying consistent."
            ),
            WalkthroughItem(
                R.drawable.slider_5,
                "Visualize Your Productivity",
                "Track your progress with clear insights and charts—see how consistent habits and completed tasks add up over time."
            )
        )

        val adapter = WalkthroughAdapter(walkthroughItems)
        binding.viewPager.adapter = adapter

        // Setup dividers
        dividerList = listOf(
            binding.materialDivider1,
            binding.materialDivider2,
            binding.materialDivider3,
            binding.materialDivider4,
            binding.materialDivider5
        )
        updateDividers(0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDividers(position)
            }
        })

        // Skip button click
        binding.skip.setOnClickListener {
            completeOnboarding()
        }

        // Continue button click
        binding.continueButton.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < walkthroughItems.lastIndex) {
                binding.viewPager.currentItem = current + 1
            } else {
                completeOnboarding()
            }
        }
    }

    private fun updateDividers(activeIndex: Int) {
        for ((index, divider) in dividerList.withIndex()) {
            val color = if (index == activeIndex) {
                ContextCompat.getColor(requireContext(), R.color.divider_selected)
            } else {
                ContextCompat.getColor(requireContext(), R.color.dividerColor)
            }
            divider.setDividerColor(color)
        }
    }

    private fun completeOnboarding() {
        try {
            // Mark onboarding as completed for the current user
            sessionManager.markOnboardingCompleted()

            // Navigate to main app
            findNavController().navigate(R.id.action_sliderFragment_to_taskFragment)

        } catch (e: Exception) {
            // Handle error - maybe show a toast or try navigation anyway
            android.util.Log.e("SliderFragment", "Error completing onboarding: ${e.message}")
            findNavController().navigate(R.id.action_sliderFragment_to_taskFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}