package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemSpeciesImageBinding
import com.nguyendevs.ecolens.utils.ImageUtils
import com.nguyendevs.ecolens.utils.ZoomableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import android.view.View
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window

/** Adapter hiển thị danh sách ảnh của loài, hỗ trợ xem toàn màn hình và lưu ảnh. */
class SpeciesImageAdapter :
    ListAdapter<String, SpeciesImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    /** Tạo ViewHolder cho item ảnh loài. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding =
            ItemSpeciesImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    /** Bind URL ảnh vào ViewHolder. */
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    /** ViewHolder cho một ảnh loài với shimmer và dialog xem toàn màn hình. */
    class ImageViewHolder(private val binding: ItemSpeciesImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /** Tải ảnh từ URL vào view, hiển thị shimmer trong khi chờ. */
        fun bind(url: String, isLastItem: Boolean) {
            val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            if (isLastItem) {
                params.marginEnd = 0
            } else {
                params.marginEnd = binding.root.context.resources.getDimensionPixelSize(
                    com.nguyendevs.ecolens.R.dimen.spacing_sm
                )
            }
            binding.root.layoutParams = params
            
            binding.root.alpha = 0f
            binding.root.animate().alpha(1f).setDuration(400).start()

            binding.ivSpeciesImage.visibility = android.view.View.VISIBLE
            binding.shimmerSpeciesImage.visibility = android.view.View.VISIBLE
            binding.shimmerSpeciesImage.startShimmer()

            Glide.with(binding.root.context)
                .load(url)
                .listener(
                    object : RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.shimmerSpeciesImage.stopShimmer()
                            binding.shimmerSpeciesImage.visibility = android.view.View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: Target<android.graphics.drawable.Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.shimmerSpeciesImage.stopShimmer()
                            binding.shimmerSpeciesImage.visibility = android.view.View.GONE
                            
                            binding.ivSpeciesImage.setOnClickListener {
                                val context = binding.root.context
                                val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                                
                                val dialogView = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_image_viewer, null)
                                
                                val ivFullscreenImage = dialogView.findViewById<ZoomableImageView>(R.id.ivFullscreenImage)
                                val btnCloseViewer = dialogView.findViewById<View>(R.id.btnCloseViewer)
                                val btnSaveImage = dialogView.findViewById<View>(R.id.btnSaveImage)
                                val progressBarImageSaving = dialogView.findViewById<View>(R.id.progressBarImageSaving)
                                
                                Glide.with(context)
                                    .load(url)
                                    .into(ivFullscreenImage)
                                    
                                btnCloseViewer.setOnClickListener {
                                    dialogView.animate().alpha(0f).setDuration(250).withEndAction {
                                        dialog.dismiss()
                                    }.start()
                                }
                                
                                btnSaveImage.setOnClickListener {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            withContext(Dispatchers.Main) {
                                                progressBarImageSaving.visibility = View.VISIBLE
                                                btnSaveImage.isEnabled = false
                                            }
                                            
                                            val file = Glide.with(context)
                                                .asFile()
                                                .load(url)
                                                .submit()
                                                .get()
                                                
                                            val savedUri = ImageUtils.saveImageToPublicStorage(context, file)
                                            
                                            withContext(Dispatchers.Main) {
                                                progressBarImageSaving.visibility = View.GONE
                                                btnSaveImage.isEnabled = true
                                                if (savedUri != null) {
                                                    Toast.makeText(context, "Đã lưu ảnh", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                progressBarImageSaving.visibility = View.GONE
                                                btnSaveImage.isEnabled = true
                                                Toast.makeText(context, "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                
                                dialogView.alpha = 0f
                                dialog.setContentView(dialogView)
                                dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                dialog.show()
                                
                                dialogView.animate().alpha(1f).setDuration(250).start()
                            }
                            
                            return false
                        }
                    }
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.bg_skeleton_transparent)
                .error(R.drawable.bg_skeleton_rounded)
                .into(binding.ivSpeciesImage)
        }
    }

    /** DiffCallback so sánh URL ảnh. */
    class ImageDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}