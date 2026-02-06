package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemQuickExploreBinding
import com.nguyendevs.ecolens.models.ExploreItem
import java.util.Locale

class ExploreAdapter(private val onItemClick: (ExploreItem) -> Unit) :
    ListAdapter<ExploreItem, ExploreAdapter.ExploreViewHolder>(ExploreDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val binding =
            ItemQuickExploreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExploreViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val layoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        val context = holder.itemView.context

        if (position > 0) {
            val marginMd = context.resources.getDimensionPixelSize(R.dimen.spacing_md)
            layoutParams.marginStart = marginMd
        } else {
            layoutParams.marginStart = 0
        }
        holder.itemView.layoutParams = layoutParams

        holder.bind(getItem(position))
    }

    class ExploreViewHolder(
        private val binding: ItemQuickExploreBinding,
        private val onItemClick: (ExploreItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExploreItem) {
            val context = binding.root.context
            val isPlaceholder = item.id.startsWith("placeholder_")

            if (isPlaceholder) {
                // Loading state - bật shimmer cho tất cả phần
                startAllShimmers()

                // Ẩn nội dung thật
                binding.imgExplore.visibility = View.INVISIBLE
                binding.tvExploreName.visibility = View.INVISIBLE
                binding.tvExploreDesc.visibility = View.INVISIBLE

                // Optional: làm mờ card để trông "loading" hơn
                binding.root.alpha = 0.7f

                // Không set text, không set click
            } else {
                // Loaded state - tắt shimmer
                stopAllShimmers()

                // Hiện nội dung thật
                binding.imgExplore.visibility = View.VISIBLE
                binding.tvExploreName.visibility = View.VISIBLE
                binding.tvExploreDesc.visibility = View.VISIBLE

                // Fade in animation mượt mà
                binding.root.alpha = 0f
                binding.root.animate().alpha(1f).setDuration(400).start()

                // Localization cho name
                val currentLanguage = Locale.getDefault().language
                val displayName = when (currentLanguage) {
                    "vi" -> item.name
                    "en" -> if (item.name_en.isNotEmpty()) item.name_en else item.name
                    "ja" -> if (item.name_ja.isNotEmpty()) item.name_ja else item.name
                    "zh" -> if (item.name_zh.isNotEmpty()) item.name_zh else item.name
                    else -> item.name
                }

                binding.tvExploreName.text = displayName
                binding.tvExploreDesc.text = item.desc

                // Load image thật với crossfade
                Glide.with(context)
                    .load(item.image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.bg_skeleton_rounded)
                    .error(R.drawable.bg_skeleton_rounded)
                    .centerCrop()
                    .into(binding.imgExplore)

                // Click listener chỉ cho data thật
                binding.root.setOnClickListener { onItemClick(item) }
            }
        }

        private fun startAllShimmers() {
            binding.shimmerViewContainer?.let {
                it.visibility = View.VISIBLE
                it.startShimmer()
            }
            binding.shimmerName?.let {
                it.visibility = View.VISIBLE
                it.startShimmer()
            }
            binding.shimmerDesc?.let {
                it.visibility = View.VISIBLE
                it.startShimmer()
            }
        }

        private fun stopAllShimmers() {
            binding.shimmerViewContainer?.let {
                it.stopShimmer()
                it.visibility = View.GONE
            }
            binding.shimmerName?.let {
                it.stopShimmer()
                it.visibility = View.GONE
            }
            binding.shimmerDesc?.let {
                it.stopShimmer()
                it.visibility = View.GONE
            }
        }
    }

    class ExploreDiffCallback : DiffUtil.ItemCallback<ExploreItem>() {
        override fun areItemsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
            return oldItem == newItem
        }
    }
}