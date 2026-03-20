package com.nguyendevs.ecolens.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.nguyendevs.ecolens.R

/** Tiện ích hiển thị hộp thoại xác nhận tùy chỉnh trên toàn hệ thống. */
object CustomDialogUtils {

    /** Cấu hình và hiển thị hộp thoại xác nhận với các tham số truyền vào. */
    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        confirmText: String? = null,
        cancelText: String? = null,
        onConfirm: () -> Unit
    ) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_custom_confirmation, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val dialog = AlertDialog.Builder(context).setView(dialogView).setCancelable(true).create()

        tvTitle.text = title
        tvMessage.text = message

        if (confirmText != null) btnConfirm.text = confirmText
        if (cancelText != null) btnCancel.text = cancelText

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }
}