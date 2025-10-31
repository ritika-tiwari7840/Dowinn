package com.ritika.dowinnApp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.ritika.dowinnApp.databinding.ActivityMainBinding
import com.ritika.dowinnApp.utils.NavigationDestination
import com.ritika.dowinnApp.utils.UserSessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var sessionManager: UserSessionManager


    // Flag to control splash screen visibility
    private var isNavGraphReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen once
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isNavGraphReady }

        super.onCreate(savedInstanceState)

        // Initialize Firebase and session manager
        FirebaseApp.initializeApp(this)
        sessionManager = UserSessionManager.getInstance(this)

        // Inflate layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply system window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up navigation host and controller
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment
        navController = navHostFragment.navController

        // Inflate navigation graph
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)

        // Determine dynamic start destination
        val actualStartDestination = when {
            FirebaseAuth.getInstance().currentUser == null -> R.id.welcomeFragment
            !sessionManager.isOnboardingCompleted() -> R.id.sliderFragment
            else -> R.id.taskFragment
        }

        // Set start destination and assign graph
        graph.setStartDestination(actualStartDestination)
        navController.graph = graph

        // ✅ Mark graph as ready to dismiss splash screen
        isNavGraphReady = true

        // Optional: Animate navHostFragment entrance
        val navHost = findViewById<View>(R.id.nav_host_fragment)
        navHost.translationX = 1000f
        navHost.animate()
            .translationX(0f)
            .setDuration(400)
            .setStartDelay(100)
            .start()

        // Customize status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorOnPrimaryContainer)
    }

    // Optional: Handle navigation after onboarding
    private fun handleUserNavigation() {
        when (sessionManager.getInitialDestination()) {
            NavigationDestination.LOGIN -> {
                // Already at welcomeFragment
            }
            NavigationDestination.ONBOARDING -> {
                navController.navigate(R.id.action_global_sliderFragment)
            }
            NavigationDestination.MAIN_APP -> {
                if (!sessionManager.isOnboardingCompleted()) {
                    navController.navigate(R.id.action_global_sliderFragment)
                } else {
                    navController.navigate(R.id.action_global_taskFragment)
                }
            }
        }
    }

    // Optional: Callback after onboarding is completed
    fun onOnboardingCompleted() {
        sessionManager.markOnboardingCompleted()
        handleUserNavigation()
    }
}
