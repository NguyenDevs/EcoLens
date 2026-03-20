package com.nguyendevs.ecolens.managers.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R

/** Quản lý yêu cầu quyền truy cập phần cứng và tập tin hệ thống. */
class PermissionManager(
        private val context: Context,
        private val permissionLauncher: ActivityResultLauncher<Array<String>>
) {

    /** Danh sách quyền bắt buộc theo phiên bản Android đang thông hành. */
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

    /** Đánh giá toàn bộ các quyền cơ bản đáp ứng đủ chưa. */
    fun hasPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /** Xác thực quyền tương tác với ống kính Camera. */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    /** Rà soát cấp thẩm quyền định vị hiện trường qua GPS. */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    /** Gửi thông điệp đòi cấp quyền gốc trong hệ sinh thái Android. */
    fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    /** Phát màn hình cảnh báo khi người dùng khước từ thẩm quyền. */
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
