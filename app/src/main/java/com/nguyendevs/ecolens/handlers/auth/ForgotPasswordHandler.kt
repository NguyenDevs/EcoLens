package com.nguyendevs.ecolens.handlers.auth

import android.content.Context
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.auth.FirebaseAuth
import com.nguyendevs.ecolens.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Handler cho Forgot Password
 * Xử lý việc gửi email reset password
 */
class ForgotPasswordHandler(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    private val auth = FirebaseAuth.getInstance()

    /**
     * Gửi email reset password
     * @param email Email của người dùng
     * @param onLoadingChange Callback để bật/tắt loading
     * @param onSuccess Callback khi gửi email thành công
     */
    fun sendPasswordResetEmail(
        email: String,
        onLoadingChange: (Boolean) -> Unit,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.error_email_empty),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!isValidEmail(email)) {
            Toast.makeText(
                context,
                context.getString(R.string.error_email_invalid),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        onLoadingChange(true)

        lifecycleScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()

                onLoadingChange(false)
                Toast.makeText(
                    context,
                    context.getString(R.string.reset_email_sent),
                    Toast.LENGTH_LONG
                ).show()

                onSuccess()

            } catch (e: Exception) {
                onLoadingChange(false)
                handleError(e)
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun handleError(exception: Exception) {
        val errorMessage = when {
            exception.message?.contains("network", ignoreCase = true) == true -> {
                context.getString(R.string.error_network)
            }
            exception.message?.contains("too-many-requests", ignoreCase = true) == true -> {
                context.getString(R.string.error_too_many_requests)
            }
            else -> {
                context.getString(R.string.error_reset_password_failed)
            }
        }

        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }
}