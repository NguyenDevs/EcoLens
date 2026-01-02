package com.nguyendevs.ecolens.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nguyendevs.ecolens.R

class PermissionManager(
    private val context: Context,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>
) {

    // Xác định danh sách quyền một lần duy nhất để tái sử dụng
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.permission_title))
            .setMessage(context.getString(R.string.permission_message))
            .setPositiveButton(context.getString(R.string.ok), null)
            .show()
    }
}