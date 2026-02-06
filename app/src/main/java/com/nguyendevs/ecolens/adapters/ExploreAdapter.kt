package com.nguyendevs.ecolens.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemQuickExploreBinding
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
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

            // Localization logic
            val currentLanguage = Locale.getDefault().language
            val displayName =
                    when (currentLanguage) {
                        "vi" -> item.name
                        "en" -> if (item.name_en.isNotEmpty()) item.name_en else item.name
                        "ja" -> if (item.name_ja.isNotEmpty()) item.name_ja else item.name
                        "zh" -> if (item.name_zh.isNotEmpty()) item.name_zh else item.name
                        else -> item.name
                    }

            binding.tvExploreName.text = displayName
            binding.tvExploreDesc.text = item.desc

            val shimmer = Shimmer.ColorHighlightBuilder()
                    .setBaseColor(Color.parseColor("#E0E0E0"))
                    .setHighlightColor(Color.parseColor("#F5F5F5"))
                    .setDuration(1800)
                    .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
                    .setAutoStart(true)
                    .build()

            val shimmerDrawable = ShimmerDrawable().apply {
                setShimmer(shimmer)
            }

            Glide.with(context)
                    .load(item.image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(shimmerDrawable)
                    .error(shimmerDrawable)
                    .centerCrop()
                    .into(binding.imgExplore)

            binding.root.setOnClickListener { onItemClick(item) }
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
