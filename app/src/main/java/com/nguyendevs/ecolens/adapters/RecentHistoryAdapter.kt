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
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemRecentHistoryBinding
import com.nguyendevs.ecolens.models.history.HistoryEntry
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RecentHistoryAdapter(private val onItemClick: (HistoryEntry) -> Unit) :
    ListAdapter<HistoryEntry, RecentHistoryAdapter.RecentHistoryViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HistoryEntry>() {
            override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentHistoryViewHolder {
        val binding = ItemRecentHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentHistoryViewHolder(private val binding: ItemRecentHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context get() = binding.root.context

        fun bind(entry: HistoryEntry) {
            val isPlaceholder = entry.id < 0

            if (isPlaceholder) {
                startAllShimmers()
                binding.imgThumbnail.visibility = View.INVISIBLE
                binding.tvCommonName.visibility = View.INVISIBLE
                binding.tvScientificName.visibility = View.INVISIBLE
                binding.timeContainer.visibility = View.INVISIBLE
                binding.root.setOnClickListener(null)
                return
            }

            stopTextShimmers()
            showRealContent()

            binding.tvCommonName.text = entry.speciesInfo.commonName.ifEmpty {
                context.getString(R.string.unknown_common_name)
            }

            binding.tvScientificName.text = entry.speciesInfo.scientificName.ifEmpty {
                context.getString(R.string.unknown_scientific_name)
            }

            setupTime(entry.timestamp)
            binding.root.setOnClickListener { onItemClick(entry) }

            loadImageWithShimmer(entry)
        }

        private fun startAllShimmers() {
            listOf(
                binding.shimmerThumbnail,
                binding.shimmerCommonName,
                binding.shimmerScientificName
            ).forEach {
                it.visibility = View.VISIBLE
                it.startShimmer()
            }
        }

        private fun stopTextShimmers() {
            listOf(
                binding.shimmerCommonName,
                binding.shimmerScientificName
            ).forEach {
                it.stopShimmer()
                it.visibility = View.GONE
            }
        }

        private fun stopImageShimmer() {
            binding.shimmerThumbnail.apply {
                stopShimmer()
                visibility = View.GONE
            }
        }

        private fun showRealContent() {
            binding.imgThumbnail.visibility = View.VISIBLE
            binding.tvCommonName.visibility = View.VISIBLE
            binding.tvScientificName.visibility = View.VISIBLE
            binding.timeContainer.visibility = View.VISIBLE
        }

        private fun loadImageWithShimmer(entry: HistoryEntry) {
            binding.shimmerThumbnail.visibility = View.VISIBLE
            binding.shimmerThumbnail.startShimmer()

            val localPath = entry.localImagePath
            val remotePath = entry.imagePath

            val loadModel = when {
                localPath.isNotEmpty() && File(localPath).exists() -> File(localPath)
                remotePath.isNotEmpty() -> remotePath
                else -> null
            }

            if (loadModel != null) {
                Glide.with(context)
                    .load(loadModel)
                    .centerCrop()
                    .placeholder(R.drawable.bg_skeleton_transparent)
                    .error(R.drawable.bg_skeleton_rounded)
                    .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            stopImageShimmer()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            p1: Any,
                            target: Target<Drawable>,
                            p3: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            stopImageShimmer()
                            return false
                        }
                    })
                    .into(binding.imgThumbnail)
            } else {
                binding.imgThumbnail.setImageResource(R.mipmap.ic_launcher)
                stopImageShimmer()
            }
        }

        private fun setupTime(timestamp: Long) {
            val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
            val now = ZonedDateTime.now()

            val isToday = dateTime.toLocalDate() == now.toLocalDate()
            val isYesterday = dateTime.toLocalDate() == now.toLocalDate().minusDays(1)

            binding.tvDateLabel.text = when {
                isToday -> context.getString(R.string.today).uppercase()
                isYesterday -> context.getString(R.string.yesterday).uppercase()
                ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate()) < 7 ->
                    dateTime.format(DateTimeFormatter.ofPattern("EEEE"))
                else -> dateTime.format(DateTimeFormatter.ofPattern("dd/MM"))
            }

            binding.tvTime.text = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }
}