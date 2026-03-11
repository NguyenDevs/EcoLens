package com.nguyendevs.ecolens.managers.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R

/**
 * Manager quản lý runtime permissions
 * Xử lý Camera, Storage và Location permissions với version-specific logic
 */
class PermissionManager(
        private val context: Context,
        private val permissionLauncher: ActivityResultLauncher<Array<String>>
) {

    /**
     * Required permissions dựa trên Android version
     * - API 33+: CAMERA, READ_MEDIA_IMAGES, ACCESS_FINE_LOCATION
     * - API < 33: CAMERA, READ_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION
     */
    private val requiredPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
            }

    // ==================== PERMISSION CHECK ====================

    /**
     * Kiểm tra xem tất cả required permissions đã được cấp chưa
     * @return true nếu tất cả permissions đã được cấp
     */
    fun hasPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Kiểm tra riêng camera permission
     * @return true nếu camera permission đã được cấp
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Kiểm tra riêng location permission
     * @return true nếu location permission đã được cấp
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    // ==================== PERMISSION REQUEST ====================

    /** Request tất cả required permissions */
    fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    // ==================== DIALOGS ====================

    /** Hiển thị dialog thông báo khi permissions bị từ chối */
    fun showPermissionDeniedDialog() {
        com.nguyendevs.ecolens.utils.CustomDialogUtils.showConfirmationDialog(
                context = context,
                title = context.getString(R.string.permission_title),
                message = context.getString(R.string.permission_message),
                confirmText = context.getString(R.string.ok),
                cancelText = null,
                onConfirm = {}
        )
    }
}
