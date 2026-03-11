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
import com.nguyendevs.ecolens.databinding.ItemSpeciesImageBinding

class SpeciesImageAdapter :
    ListAdapter<String, SpeciesImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding =
            ItemSpeciesImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    class ImageViewHolder(private val binding: ItemSpeciesImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
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

            binding.ivSpeciesImage.visibility = android.view.View.INVISIBLE
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
                            binding.ivSpeciesImage.visibility = android.view.View.VISIBLE
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
                            binding.ivSpeciesImage.visibility = android.view.View.VISIBLE
                            
                            binding.ivSpeciesImage.setOnClickListener {
                                val context = binding.root.context
                                val dialog = android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                                dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                                
                                val imageView = android.widget.ImageView(context)
                                imageView.layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                imageView.setBackgroundColor(android.graphics.Color.BLACK)
                                imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                
                                com.bumptech.glide.Glide.with(context)
                                    .load(url)
                                    .into(imageView)
                                    
                                imageView.setOnClickListener {
                                    dialog.dismiss()
                                }
                                
                                dialog.setContentView(imageView)
                                dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                                dialog.show()
                            }
                            
                            return false
                        }
                    }
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivSpeciesImage)
        }
    }

    class ImageDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}