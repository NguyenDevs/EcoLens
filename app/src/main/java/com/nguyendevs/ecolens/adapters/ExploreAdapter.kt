package com.nguyendevs.ecolens.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemQuickExploreBinding
import com.nguyendevs.ecolens.models.ExploreItem
import java.util.Locale

/** Adapter hiển thị danh sách explore items, hỗ trợ placeholder shimmer. */
class ExploreAdapter(private val onItemClick: (ExploreItem) -> Unit) :
    ListAdapter<ExploreItem, ExploreAdapter.ExploreViewHolder>(ExploreDiffCallback()) {

    /** Tạo ViewHolder cho explore item. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val binding =
            ItemQuickExploreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExploreViewHolder(binding, onItemClick)
    }

    private var marginMd: Int = -1

    /** Bind item với margin phù hợp cho vị trí đầu tiên. */
    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val layoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        
        if (marginMd == -1) {
            marginMd = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_md)
        }

        if (position > 0) {
            layoutParams.marginStart = marginMd
        } else {
            layoutParams.marginStart = 0
        }
        holder.itemView.layoutParams = layoutParams

        holder.bind(getItem(position))
    }

    /** ViewHolder cho một explore item. */
    class ExploreViewHolder(
        private val binding: ItemQuickExploreBinding,
        private val onItemClick: (ExploreItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Hiển thị thông tin item hoặc trạng thái placeholder shimmer. */
        fun bind(item: ExploreItem) {
            val context = binding.root.context
            val isPlaceholder = item.id.startsWith("placeholder_")

            if (isPlaceholder) {
                startAllShimmers()

                binding.imgExplore.visibility = View.INVISIBLE
                binding.tvExploreName.visibility = View.INVISIBLE
                binding.tvExploreDesc.visibility = View.INVISIBLE

                binding.root.alpha = 0.7f

                binding.root.setOnClickListener(null)
                return
            }

            stopAllShimmers()
            showRealContent()

            binding.root.alpha = 0f
            binding.root.animate().alpha(1f).setDuration(400).start()

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

            binding.root.setOnClickListener { onItemClick(item) }

            loadImageWithShimmer(item)
        }

        /** Bật shimmer cho tất cả các views. */
        private fun startAllShimmers() {
            listOfNotNull(
                binding.shimmerViewContainer,
                binding.shimmerName,
                binding.shimmerDesc
            ).forEach {
                it.visibility = View.VISIBLE
                it.startShimmer()
            }
        }

        /** Tắt shimmer và ẩn tất cả. */
        private fun stopAllShimmers() {
            listOfNotNull(
                binding.shimmerViewContainer,
                binding.shimmerName,
                binding.shimmerDesc
            ).forEach {
                it.stopShimmer()
                it.visibility = View.GONE
            }
        }

        /** Hiển thị nội dung thực sau placeholder. */
        private fun showRealContent() {
            binding.imgExplore.visibility = View.VISIBLE
            binding.tvExploreName.visibility = View.VISIBLE
            binding.tvExploreDesc.visibility = View.VISIBLE
            binding.root.alpha = 1f
        }

        /** Tải ảnh từ URL với shimmer trong khi chờ. */
        private fun loadImageWithShimmer(item: ExploreItem) {
            binding.shimmerViewContainer.apply {
                visibility = View.VISIBLE
                startShimmer()
            }

            Glide.with(binding.root.context)
                .load(item.image)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.bg_skeleton_transparent)
                .error(R.drawable.bg_skeleton_rounded)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.shimmerViewContainer.apply {
                            stopShimmer()
                            visibility = View.GONE
                        }
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.shimmerViewContainer.apply {
                            stopShimmer()
                            visibility = View.GONE
                        }
                        return false
                    }
                })
                .into(binding.imgExplore)
        }
    }

    /** DiffCallback so sánh explore items theo ID. */
    class ExploreDiffCallback : DiffUtil.ItemCallback<ExploreItem>() {
        override fun areItemsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
            return oldItem == newItem
        }
    }
}