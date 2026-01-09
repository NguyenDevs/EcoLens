package com.nguyendevs.ecolens.handlers.auth

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import kotlinx.coroutines.launch

/**
 * Handler cho Google Sign-In
 * Quản lý flow đăng nhập với Google
 */
class GoogleSignInHandler(
    private val activity: AppCompatActivity,
    private val userRepository: UserRepository,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    fun setup(
        rememberMe: Boolean,
        onLoadingChange: (Boolean) -> Unit,
        onSuccess: () -> Unit
    ) {
        // Setup Google Sign-In Options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)

        // Setup Activity Result Launcher
        googleSignInLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    account.idToken?.let {
                        firebaseAuthWithGoogle(it, rememberMe, onLoadingChange, onSuccess)
                    }
                } catch (e: ApiException) {
                    Toast.makeText(
                        activity,
                        "Google sign in failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(
        idToken: String,
        rememberMe: Boolean,
        onLoadingChange: (Boolean) -> Unit,
        onSuccess: () -> Unit
    ) {
        onLoadingChange(true)

        lifecycleScope.launch {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = userRepository.signInWithCredential(credential)

            if (user != null) {
                saveRememberMe(rememberMe)

                val userDetails = userRepository.getCurrentUserDetails()
                if (userDetails != null) {
                    applyUserTheme(userDetails.darkMode)
                }

                Toast.makeText(
                    activity,
                    activity.getString(R.string.login_success),
                    Toast.LENGTH_SHORT
                ).show()

                onSuccess()
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.login_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }

            onLoadingChange(false)
        }
    }

    private fun saveRememberMe(isRemember: Boolean) {
        val sharedPref = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("remember_me", isRemember).apply()
    }

    private fun applyUserTheme(isDarkMode: Boolean) {
        val sharedPref = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("dark_mode", isDarkMode).apply()
    }
}