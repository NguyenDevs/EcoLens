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

    // Kiểm tra quyền truy cập camera và bộ nhớ
    fun hasPermissions(): Boolean {
        val camera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        val storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
        return camera && storage
    }

    // Yêu cầu cấp quyền
    fun requestPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms)
    }

    // Hiển thị dialog thông báo quyền bị từ chối
    fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.permission_title))
            .setMessage(context.getString(R.string.permission_message))
            .setPositiveButton(context.getString(R.string.ok), null)
            .show()
    }
}