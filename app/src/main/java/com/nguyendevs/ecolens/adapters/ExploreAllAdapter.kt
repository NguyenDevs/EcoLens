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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemExploreListBinding
import com.nguyendevs.ecolens.models.ExploreItem
import java.util.Locale

/** Adapter hiển thị toàn bộ explore items dạng danh sách dọc. */
class ExploreAllAdapter(private val onItemClick: (ExploreItem) -> Unit) :
    ListAdapter<ExploreItem, ExploreAllAdapter.ExploreAllViewHolder>(ExploreAllDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreAllViewHolder {
        val binding =
            ItemExploreListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExploreAllViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ExploreAllViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /** ViewHolder cho một explore item dạng list. */
    class ExploreAllViewHolder(
        private val binding: ItemExploreListBinding,
        private val onItemClick: (ExploreItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExploreItem) {
            val isPlaceholder = item.id.startsWith("placeholder_")

            if (isPlaceholder) {
                startAllShimmers()
                binding.imgExploreThumb.visibility = View.INVISIBLE
                binding.tvExploreName.visibility = View.INVISIBLE
                binding.tvExploreDesc.visibility = View.INVISIBLE

                binding.root.alpha = 0.7f
                binding.itemContainer.setOnClickListener(null)
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

            binding.itemContainer.setOnClickListener { onItemClick(item) }

            loadThumbnail(item)
        }

        /** Bật shimmer cho tất cả các views. */
        private fun startAllShimmers() {
            listOf(
                binding.shimmerThumb,
                binding.shimmerName,
                binding.shimmerDesc
            ).forEach {
                it.visibility = View.VISIBLE
                it.startShimmer()
            }
        }

        /** Tắt shimmer và ẩn tất cả. */
        private fun stopAllShimmers() {
            listOf(
                binding.shimmerThumb,
                binding.shimmerName,
                binding.shimmerDesc
            ).forEach {
                it.stopShimmer()
                it.visibility = View.GONE
            }
        }

        /** Hiển thị nội dung thực sau placeholder. */
        private fun showRealContent() {
            binding.imgExploreThumb.visibility = View.VISIBLE
            binding.tvExploreName.visibility = View.VISIBLE
            binding.tvExploreDesc.visibility = View.VISIBLE
            binding.root.alpha = 1f
        }

        /** Tải ảnh thumbnail với shimmer khi đang load. */
        private fun loadThumbnail(item: ExploreItem) {
            binding.shimmerThumb.apply {
                visibility = View.VISIBLE
                startShimmer()
            }

            Glide.with(binding.root.context)
                .load(item.image)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(400))
                .placeholder(R.drawable.splash)
                .error(R.drawable.splash)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.shimmerThumb.apply {
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
                        binding.shimmerThumb.apply {
                            stopShimmer()
                            visibility = View.GONE
                        }
                        return false
                    }
                })
                .into(binding.imgExploreThumb)
        }
    }

    /** DiffCallback so sánh explore items theo ID. */
    class ExploreAllDiffCallback : DiffUtil.ItemCallback<ExploreItem>() {
        override fun areItemsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExploreItem, newItem: ExploreItem): Boolean {
            return oldItem == newItem
        }
    }
}
