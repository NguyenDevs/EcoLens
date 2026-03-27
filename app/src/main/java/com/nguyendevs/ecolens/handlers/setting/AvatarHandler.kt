package com.nguyendevs.ecolens.handlers.setting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

/** Quản lý logic avatar: chọn ảnh, crop, upload lên Firebase Storage, hiển thị bằng Glide. */
class AvatarHandler(
    private val activity: AppCompatActivity,
    private val binding: ScreenSettingsBinding,
    private val onPickImage: () -> Unit
) {

    private val userRepository = UserRepository()
    private val storageRef = FirebaseStorage.getInstance().reference

    init {
        setupClickListener()
        loadCurrentAvatar()
    }

    /** Gắn click listener cho avatarCard để mở gallery. */
    private fun setupClickListener() {
        binding.avatarCard.setOnClickListener {
            onPickImage()
        }
    }

    /** Load avatar hiện tại từ DB và hiển thị bằng Glide. */
    fun loadCurrentAvatar() {
        activity.lifecycleScope.launch {
            val url = userRepository.getAvatarUrl()
            if (!url.isNullOrEmpty()) {
                Glide.with(activity)
                    .load(url)
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .into(binding.ivUserAvatar)
            } else {
                binding.ivUserAvatar.setImageResource(R.mipmap.ic_launcher_round)
            }
        }
    }

    /** Tạo Intent uCrop để crop ảnh thành hình vuông 512x512. */
    fun createCropIntent(sourceUri: Uri): Intent {
        val destinationUri = Uri.fromFile(
            File(activity.cacheDir, "avatar_cropped.png")
        )

        val primaryColor = ContextCompat.getColor(activity, R.color.primary)
        val backgroundColor = ContextCompat.getColor(activity, R.color.background)
        val surfaceColor = ContextCompat.getColor(activity, R.color.surface)

        val options = UCrop.Options().apply {
            setCompressionFormat(android.graphics.Bitmap.CompressFormat.PNG)
            setCompressionQuality(90)
            setMaxBitmapSize(1024)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(false)
            setToolbarColor(surfaceColor)
            setStatusBarColor(backgroundColor)
            setActiveControlsWidgetColor(primaryColor)
            setToolbarWidgetColor(ContextCompat.getColor(activity, R.color.text_primary))
        }

        return UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .withOptions(options)
            .getIntent(activity)
    }

    /** Xử lý kết quả crop từ uCrop, upload lên Firebase Storage. */
    fun handleCropResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val croppedUri = UCrop.getOutput(result.data!!) ?: return
            uploadAvatar(croppedUri)
        } else if (result.resultCode == UCrop.RESULT_ERROR && result.data != null) {
            val error = UCrop.getError(result.data!!)
            error?.printStackTrace()
            Toast.makeText(activity, "Crop thất bại: ${error?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Upload ảnh đã crop lên Firebase Storage rồi cập nhật DB. */
    private fun uploadAvatar(uri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val avatarRef = storageRef.child("users/$uid/avatars/avatar_picture.png")

        avatarRef.putFile(uri)
            .addOnSuccessListener {
                avatarRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    activity.lifecycleScope.launch {
                        userRepository.updateAvatarUrl(downloadUrl.toString())
                    }

                    Glide.with(activity)
                        .load(downloadUrl)
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(binding.ivUserAvatar)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(
                    activity,
                    "Upload avatar thất bại: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
