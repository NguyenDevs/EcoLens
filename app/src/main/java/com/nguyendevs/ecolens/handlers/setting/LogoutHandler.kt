package com.nguyendevs.ecolens.handlers.setting

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.activities.AuthActivity
import com.nguyendevs.ecolens.database.HistoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handler quản lý logout và delete account. Xử lý: confirm dialogs, clear local data, Firebase sign
 * out, navigation.
 */
class LogoutHandler(private val activity: AppCompatActivity) {

    fun showLogoutConfirmDialog() {
        AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.action_logout) { _, _ -> logout() }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
    }

    fun showDeleteAccountConfirmDialog() {
        AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_delete_account_title)
                .setMessage(R.string.dialog_delete_account_message)
                .setPositiveButton(R.string.action_delete) { _, _ -> deleteAccount() }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
    }

    private fun deleteAccount() {
        activity.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val db = HistoryDatabase.getDatabase(activity)
                    db.clearAllTables()
                }

                val user = FirebaseAuth.getInstance().currentUser
                user?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        logout()
                    } else {
                        Toast.makeText(
                                        activity,
                                        activity.getString(R.string.error_delete_account),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                                activity,
                                activity.getString(R.string.error_delete_account),
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }
    }

    private fun logout() {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = HistoryDatabase.getDatabase(activity)
                db.historyDao().deleteAll()
                db.chatDao().deleteMessagesBySession(-1)
                db.clearAllTables()
            }

            FirebaseAuth.getInstance().signOut()

            val gso =
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(activity.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
            val googleSignInClient = GoogleSignIn.getClient(activity, gso)
            googleSignInClient.signOut()

            val sharedPreferences =
                    activity.getSharedPreferences("EcoLensPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().remove("username").remove("last_nav_item").apply()

            val appSettings = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            appSettings.edit().putBoolean("dark_mode", false).apply()
            appSettings.edit().remove("remember_me").apply()

            withContext(Dispatchers.Main) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            val intent = Intent(activity, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }
    }
}
