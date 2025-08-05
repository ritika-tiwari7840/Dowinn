package com.ritika.dowinnApp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.ritika.dowinnApp.BuildConfig

class UserSessionManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val ONBOARDING_COMPLETED = "onboarding_completed"
        private const val TAG = "UserSessionManager"

        @Volatile
        private var INSTANCE: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun getCurrentUser(): FirebaseUser? = auth.currentUser

    private fun getUserSpecificPrefs(): SharedPreferences? {
        val currentUser = getCurrentUser()
        return currentUser?.let { user ->
            context.getSharedPreferences("user_${user.uid}", Context.MODE_PRIVATE)
        }
    }

    // Onboarding related methods
    fun isOnboardingCompleted(): Boolean {
        val completed = getUserSpecificPrefs()?.getBoolean(ONBOARDING_COMPLETED, false) ?: false
        Log.d(TAG, "Onboarding completed: $completed for user: ${getCurrentUserId()}")
        return completed
    }

    fun markOnboardingCompleted() {
        getUserSpecificPrefs()?.edit()
            ?.putBoolean(ONBOARDING_COMPLETED, true)
            ?.commit() // Use commit() instead of apply() for immediate persistence
        Log.d(TAG, "Onboarding marked as completed for user: ${getCurrentUserId()}")
    }

    private fun markOnboardingIncomplete() {
        getUserSpecificPrefs()?.edit()
            ?.putBoolean(ONBOARDING_COMPLETED, false)
            ?.commit()
        Log.d(TAG, "Onboarding marked as incomplete for user: ${getCurrentUserId()}")
    }

    // Authentication state checks
    fun isUserSignedIn(): Boolean {
        val signedIn = getCurrentUser() != null
        Log.d(TAG, "User signed in: $signedIn")
        return signedIn
    }

    fun getCurrentUserId(): String? = getCurrentUser()?.uid

    fun getCurrentUserEmail(): String? = getCurrentUser()?.email

    // Navigation decision helper
    fun getInitialDestination(): NavigationDestination {
        val destination = when {
            !isUserSignedIn() -> NavigationDestination.LOGIN
            !isOnboardingCompleted() -> NavigationDestination.ONBOARDING
            else -> NavigationDestination.MAIN_APP
        }
        Log.d(TAG, "Initial destination: $destination")
        return destination
    }

    // Clear user data on sign out - with onboarding reset
    fun clearUserSession() {
        val currentUserId = getCurrentUserId()
        Log.d(TAG, "Clearing session for user: $currentUserId")

        // Clear user-specific onboarding data before signing out
        clearCurrentUserOnboardingData()

        // Sign out from Firebase Auth
        auth.signOut()

        // Sign out from Google Sign-In to clear cached account
        val googleSignInClient = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.DEFAULT_WEB_CLIENT_ID)
                .requestEmail()
                .build()
        )

        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Google sign out successful")
            } else {
                Log.e(TAG, "Google sign out failed", task.exception)
            }
        }

        Log.d(TAG, "User session cleared successfully")
    }

    // Alternative method to revoke access completely (removes app from user's Google account)
    fun revokeGoogleAccess() {
        val currentUserId = getCurrentUserId()
        Log.d(TAG, "Revoking Google access for user: $currentUserId")

        // Clear user-specific onboarding data before revoking access
        clearCurrentUserOnboardingData()

        auth.signOut()

        val googleSignInClient = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.DEFAULT_WEB_CLIENT_ID)
                .requestEmail()
                .build()
        )

        googleSignInClient.revokeAccess().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Google access revoked successfully")
            } else {
                Log.e(TAG, "Failed to revoke Google access", task.exception)
            }
        }
    }

    // Clear current user's onboarding data specifically
    private fun clearCurrentUserOnboardingData() {
        getCurrentUser()?.let { user ->
            val userPrefs = context.getSharedPreferences("user_${user.uid}", Context.MODE_PRIVATE)
            userPrefs.edit()
                .putBoolean(ONBOARDING_COMPLETED, false)
                .commit()
            Log.d(TAG, "Onboarding data cleared for user: ${user.uid}")
        }
    }

    // Clear all user preferences (useful for troubleshooting)
    fun clearAllUserData() {
        getCurrentUser()?.let { user ->
            val userPrefs = context.getSharedPreferences("user_${user.uid}", Context.MODE_PRIVATE)
            userPrefs.edit().clear().commit()
            Log.d(TAG, "All user data cleared for user: ${user.uid}")
        }
    }

    // Method to manually reset onboarding for current user
    fun resetOnboardingForCurrentUser() {
        if (isUserSignedIn()) {
            markOnboardingIncomplete()
            Log.d(TAG, "Onboarding reset for current user")
        } else {
            Log.w(TAG, "Cannot reset onboarding - no user signed in")
        }
    }

    // Method to get user session info for debugging
    fun getSessionInfo(): String {
        return """
            User ID: ${getCurrentUserId() ?: "None"}
            Email: ${getCurrentUserEmail() ?: "None"}
            Signed In: ${isUserSignedIn()}
            Onboarding Completed: ${isOnboardingCompleted()}
            Initial Destination: ${getInitialDestination()}
        """.trimIndent()
    }
}

enum class NavigationDestination {
    LOGIN,
    ONBOARDING,
    MAIN_APP
}