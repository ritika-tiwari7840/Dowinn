package com.ritika.dowinnApp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.ritika.dowinnApp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var sessionManager: UserSessionManager

    private var isSplashDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Material SplashScreen
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isSplashDone }

        super.onCreate(savedInstanceState)

        // Initialize Firebase & session
        FirebaseApp.initializeApp(this)
        sessionManager = UserSessionManager.getInstance(this)

        // Inflate view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get NavHostFragment and controller
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment
        navController = navHostFragment.navController

        // Dynamically change start destination BEFORE splash is dismissed
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)

        val actualStartDestination = when {
            FirebaseAuth.getInstance().currentUser == null -> R.id.welcomeFragment
            !sessionManager.isOnboardingCompleted() -> R.id.sliderFragment
            else -> R.id.taskFragment
        }

// Set the start destination using a new graph
        graph.setStartDestination(actualStartDestination)

// Set the new graph to NavController
        navController.graph = graph
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorOnPrimaryContainer)


        val navHost = findViewById<View>(R.id.nav_host_fragment)
        navHost.translationX = 1000f
        navHost.animate()
            .translationX(0f)
            .setDuration(400)
            .setStartDelay(100)
            .start()

        // Hide splash screen after navigation is ready
        isSplashDone = true
    }


    private fun handleUserNavigation() {
        when (sessionManager.getInitialDestination()) {
            NavigationDestination.LOGIN -> {
                // Already at welcomeFragment (start destination)
            }

            NavigationDestination.ONBOARDING -> {
                navController.navigate(R.id.action_global_sliderFragment)
            }

            NavigationDestination.MAIN_APP -> {
                if (!sessionManager.isOnboardingCompleted()) {
                    // Block navigation until onboarding is complete
                    navController.navigate(R.id.action_global_sliderFragment)
                } else {
                    navController.navigate(R.id.action_global_taskFragment)
                }
            }
        }
    }


    fun onOnboardingCompleted() {
        sessionManager.markOnboardingCompleted()
        // Navigate to next screen (profile setup or main app)
        handleUserNavigation()
    }
}