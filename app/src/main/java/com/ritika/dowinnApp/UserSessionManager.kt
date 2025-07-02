package com.ritika.dowinnApp

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.ritika.dowinnApp.R

class UserSessionManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val ONBOARDING_COMPLETED = "onboarding_completed"

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
        return getUserSpecificPrefs()?.getBoolean(ONBOARDING_COMPLETED, false) ?: false
    }

    fun markOnboardingCompleted() {
        getUserSpecificPrefs()?.edit()
            ?.putBoolean(ONBOARDING_COMPLETED, true)
            ?.apply()
    }

    // Authentication state checks
    fun isUserSignedIn(): Boolean = getCurrentUser() != null

    fun getCurrentUserId(): String? = getCurrentUser()?.uid

    fun getCurrentUserEmail(): String? = getCurrentUser()?.email

    // Navigation decision helper
    fun getInitialDestination(): NavigationDestination {
        return when {
            !isUserSignedIn() -> NavigationDestination.LOGIN
            !isOnboardingCompleted() -> NavigationDestination.ONBOARDING
            else -> NavigationDestination.MAIN_APP
        }
    }

    // Clear user data on sign out
    fun clearUserSession() {
        // Sign out from Firebase Auth
        auth.signOut()

        // Sign out from Google Sign-In to clear cached account
        val googleSignInClient = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id)) // Make sure you have this in strings.xml
                .requestEmail()
                .build()
        )

        googleSignInClient.signOut()
    }

    // Alternative method to revoke access completely (removes app from user's Google account)
    fun revokeGoogleAccess() {
        auth.signOut()

        val googleSignInClient = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        googleSignInClient.revokeAccess()
    }

    // Clear all user preferences (useful for troubleshooting)
    fun clearAllUserData() {
        getCurrentUser()?.let { user ->
            val userPrefs = context.getSharedPreferences("user_${user.uid}", Context.MODE_PRIVATE)
            userPrefs.edit().clear().apply()
        }
    }
}

enum class NavigationDestination {
    LOGIN,
    ONBOARDING,
    MAIN_APP
}