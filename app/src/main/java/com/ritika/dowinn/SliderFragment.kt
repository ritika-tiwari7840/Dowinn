package com.ritika.dowinn

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.divider.MaterialDivider
import com.ritika.dowinn.adapter.WalkthroughAdapter
import com.ritika.dowinn.api.dataclasses.WalkthroughItem
import com.ritika.dowinn.databinding.ActivityMainBinding
import com.ritika.dowinn.databinding.FragmentSliderBinding

class SliderFragment : Fragment() {
    private var _binding: FragmentSliderBinding? = null
    private val binding get() = _binding!!
    private lateinit var dividerList: List<MaterialDivider>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentSliderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val walkthroughItems = listOf(
            WalkthroughItem(
                R.drawable.no_task,
                "Smart Task Management",
                "Organize your day effortlessly with a beautifully simple interface. Create, edit, and check off tasks with ease."
            ), WalkthroughItem(
                R.drawable.no_task,
                "Focus & Productivity",
                "Stay focused and boost productivity by breaking your work into manageable steps and setting clear goals."
            ), WalkthroughItem(
                R.drawable.no_task,
                "Daily Habit Tracking",
                "Track habits like workouts or morning routines to build consistency and stay motivated every day."
            ), WalkthroughItem(
                R.drawable.no_task,
                "Make Progress Fun",
                "Enjoy ticking off tasks, keep your streak alive, and get rewarded for staying consistent."
            ), WalkthroughItem(
                R.drawable.no_task,
                "Visualize Your Productivity",
                "Track your progress with clear insights and charts—see how consistent habits and completed tasks add up over time."
            )
        )

        val adapter = WalkthroughAdapter(walkthroughItems)
        binding.viewPager.adapter = adapter

        // Prepare the list of dividers
        dividerList = listOf(
            binding.materialDivider1,
            binding.materialDivider2,
            binding.materialDivider3,
            binding.materialDivider4,
            binding.materialDivider5
        )

        // Set initial active state
        updateDividers(0)

        // Listen for page changes
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDividers(position)
            }
        })

        // Set up click listeners for buttons
        binding.skip.setOnClickListener {
            // Navigate to Main/Login Activity or last screen
        }

        binding.continueButton.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < walkthroughItems.lastIndex) {
                binding.viewPager.currentItem = current + 1
            } else {
                // Last item - proceed to app
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
            divider.setDividerColor(color) // sets color for MaterialDivider
        }
    }
}
