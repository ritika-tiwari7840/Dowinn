package com.ritika.dowinn

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Flag to track splash visibility
    private var isSplashDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show splash screen and control its visibility
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isSplashDone } // Keep splash only while false

        super.onCreate(savedInstanceState)

        // Optional: edge-to-edge system window setup
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorOnPrimaryContainer)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.colorOnPrimaryContainer)

        // Delay for splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            // Set content view after delay
            setContentView(R.layout.activity_main)

            // Now the view is available
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // Finally, release the splash screen
            isSplashDone = true
        }, 2000)
    }
}
