package com.nguyendevs.ecolens.handlers.setting

import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.UserRepository
import com.nguyendevs.ecolens.databinding.ScreenSettingsBinding
import kotlinx.coroutines.launch

/** Quản lý logic avatar: chọn ảnh, upload lên Firebase Storage, hiển thị bằng Glide. */
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

    /** Xử lý ảnh đã chọn từ gallery: upload lên Storage rồi cập nhật DB. */
    fun handlePickedImage(uri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val avatarRef = storageRef.child("users/$uid/avatars/avatar_picture.png")

        avatarRef.putFile(uri)
            .addOnSuccessListener {
                avatarRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    activity.lifecycleScope.launch {
                        userRepository.updateAvatarUrl(downloadUrl.toString())
                    }

                    // Hiển thị ảnh mới ngay lập tức
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
