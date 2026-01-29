package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemRecentHistoryBinding
import com.nguyendevs.ecolens.models.history.HistoryEntry
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Adapter cho Recent History section trên Home Screen Hiển thị 5 lịch sử nhận diện gần nhất */
class RecentHistoryAdapter(private val onItemClick: (HistoryEntry) -> Unit) :
        ListAdapter<HistoryEntry, RecentHistoryAdapter.RecentHistoryViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback =
                object : DiffUtil.ItemCallback<HistoryEntry>() {
                    override fun areItemsTheSame(
                            oldItem: HistoryEntry,
                            newItem: HistoryEntry
                    ): Boolean {
                        return oldItem.id == newItem.id
                    }

                    override fun areContentsTheSame(
                            oldItem: HistoryEntry,
                            newItem: HistoryEntry
                    ): Boolean {
                        return oldItem == newItem
                    }
                }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentHistoryViewHolder {
        val binding =
                ItemRecentHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentHistoryViewHolder(private val binding: ItemRecentHistoryBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: HistoryEntry) {
            val context = binding.root.context

            binding.tvCommonName.text =
                    entry.speciesInfo.commonName.ifEmpty {
                        context.getString(R.string.unknown_common_name)
                    }

            binding.tvScientificName.text =
                    entry.speciesInfo.scientificName.ifEmpty {
                        context.getString(R.string.unknown_scientific_name)
                    }

            loadImage(entry)

            setupTime(entry.timestamp)

            binding.root.setOnClickListener { onItemClick(entry) }
        }

        private fun loadImage(entry: HistoryEntry) {
            val context = binding.root.context
            val localPath = entry.localImagePath
            val remotePath = entry.imagePath

            val loadModel =
                    when {
                        localPath.isNotEmpty() && File(localPath).exists() -> File(localPath)
                        remotePath.isNotEmpty() -> remotePath
                        else -> null
                    }

            if (loadModel != null) {
                Glide.with(context)
                        .load(loadModel)
                        .centerCrop()
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(binding.imgThumbnail)
            } else {
                binding.imgThumbnail.setImageResource(R.mipmap.ic_launcher)
            }
        }

        private fun setupTime(timestamp: Long) {
            val context = binding.root.context
            val dateTime =
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
            val now = ZonedDateTime.now()

            val isToday = dateTime.toLocalDate() == now.toLocalDate()
            val isYesterday = dateTime.toLocalDate() == now.toLocalDate().minusDays(1)

            binding.tvDateLabel.text =
                    when {
                        isToday -> context.getString(R.string.today).uppercase()
                        isYesterday -> context.getString(R.string.yesterday).uppercase()
                        ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate()) < 7 -> {
                            dateTime.format(DateTimeFormatter.ofPattern("EEEE"))
                        }
                        else -> dateTime.format(DateTimeFormatter.ofPattern("dd/MM"))
                    }

            binding.tvTime.text = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }
}
