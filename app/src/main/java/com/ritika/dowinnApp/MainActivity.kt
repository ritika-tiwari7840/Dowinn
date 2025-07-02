package com.ritika.dowinnApp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.FirebaseApp
import com.ritika.dowinnApp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: UserSessionManager

    private var isSplashDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isSplashDone }

        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize session manager
        sessionManager = UserSessionManager.getInstance(this)

        // Window styling
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorOnPrimaryContainer)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.colorOnPrimaryContainer)

        // Splash screen delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Inflate layout and set content view
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Apply window insets for padding
            ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // Mark splash screen done
            isSplashDone = true

            // Initialize NavController
            navController = Navigation.findNavController(this, R.id.nav_host_fragment)

            // Handle navigation based on user state
            handleUserNavigation()

        }, 1000)
    }

    private fun handleUserNavigation() {
        when (sessionManager.getInitialDestination()) {
            NavigationDestination.LOGIN -> {
                // Navigate to login/signup screen
                navController.navigate(R.id.welcomeFragment)
            }
            NavigationDestination.ONBOARDING -> {
                // User is signed in but hasn't completed onboarding
                navController.navigate(R.id.action_global_sliderFragment)
            }
            NavigationDestination.MAIN_APP -> {
                // User is fully set up, go to main app
                navController.navigate(R.id.action_global_taskFragment)
            }

        }
    }
    fun onOnboardingCompleted() {
        sessionManager.markOnboardingCompleted()
        // Navigate to next screen (profile setup or main app)
        handleUserNavigation()
    }


}