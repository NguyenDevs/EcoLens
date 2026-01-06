package com.nguyendevs.ecolens.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemHistoryEntryModernBinding
import com.nguyendevs.ecolens.model.HistoryEntry
import io.noties.markwon.Markwon
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Adapter hiển thị danh sách lịch sử nhận diện loài
 * Tự động nhóm theo ngày với rounded corners và border liên tục
 * Sử dụng DiffUtil để tối ưu performance khi update list
 */
class HistoryAdapter(
    private var historyList: List<HistoryEntry>,
    private val markwon: Markwon,
    private val clickListener: (HistoryEntry) -> Unit,
    private val favoriteClickListener: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    // ==================== ADAPTER METHODS ====================

    /**
     * Cập nhật danh sách với DiffUtil để tối ưu performance
     * Tự động tính toán và chỉ update các item thay đổi
     */
    fun updateList(newList: List<HistoryEntry>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = historyList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return historyList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = historyList[oldItemPosition]
                val newItem = newList[newItemPosition]

                val isContentSame = oldItem.id == newItem.id &&
                        oldItem.timestamp == newItem.timestamp &&
                        oldItem.speciesInfo.commonName == newItem.speciesInfo.commonName &&
                        oldItem.speciesInfo.scientificName == newItem.speciesInfo.scientificName &&
                        oldItem.isFavorite == newItem.isFavorite &&
                        oldItem.imagePath == newItem.imagePath

                if (!isContentSame) return false

                val oldIsFirst = oldItemPosition == 0 ||
                        !isSameDay(oldItem.timestamp, historyList[oldItemPosition - 1].timestamp)
                val oldIsLast = oldItemPosition == historyList.size - 1 ||
                        !isSameDay(oldItem.timestamp, historyList[oldItemPosition + 1].timestamp)

                val newIsFirst = newItemPosition == 0 ||
                        !isSameDay(newItem.timestamp, newList[newItemPosition - 1].timestamp)
                val newIsLast = newItemPosition == newList.size - 1 ||
                        !isSameDay(newItem.timestamp, newList[newItemPosition + 1].timestamp)

                return oldIsFirst == newIsFirst && oldIsLast == newIsLast
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        historyList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryEntryModernBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = historyList[position]

        val isFirstItemOfDay = position == 0 ||
                !isSameDay(entry.timestamp, historyList[position - 1].timestamp)
        val isLastItemOfDay = position == historyList.size - 1 ||
                !isSameDay(entry.timestamp, historyList[position + 1].timestamp)

        holder.bind(entry, isFirstItemOfDay, isLastItemOfDay, clickListener)
    }

    override fun getItemCount(): Int = historyList.size

    /**
     * Kiểm tra hai timestamp có cùng ngày hay không
     */
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val date1 = Instant.ofEpochMilli(timestamp1).atZone(ZoneId.systemDefault()).toLocalDate()
        val date2 = Instant.ofEpochMilli(timestamp2).atZone(ZoneId.systemDefault()).toLocalDate()
        return date1 == date2
    }

    // ==================== VIEW HOLDER ====================

    inner class HistoryViewHolder(
        private val binding: ItemHistoryEntryModernBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val radius = itemView.resources.displayMetrics.density * 16
        private val strokeWidth = (1 * itemView.resources.displayMetrics.density).toInt()

        private val colorSurface = ContextCompat.getColor(itemView.context, R.color.surface)
        private val strokeColor = ContextCompat.getColor(itemView.context, R.color.border_normal)

        /**
         * Bind dữ liệu lịch sử vào view
         * Tự động xử lý rounded corners và border dựa vào vị trí trong ngày
         */
        fun bind(
            entry: HistoryEntry,
            isFirstItemOfDay: Boolean,
            isLastItemOfDay: Boolean,
            clickListener: (HistoryEntry) -> Unit
        ) {
            val currentDateTime = Instant.ofEpochMilli(entry.timestamp)
                .atZone(ZoneId.systemDefault())

            val commonText = entry.speciesInfo.commonName.ifEmpty {
                itemView.context.getString(R.string.unknown_common_name)
            }
            val scientificText = entry.speciesInfo.scientificName.ifEmpty {
                itemView.context.getString(R.string.unknown_scientific_name)
            }

            markwon.setMarkdown(binding.tvHistoryCommonName, commonText)
            markwon.setMarkdown(binding.tvHistoryScientificName, scientificText)
            binding.tvHistoryTime.text = timeFormatter.format(currentDateTime)

            loadImage(entry)
            setupDateHeader(isFirstItemOfDay, currentDateTime)
            setupCardAppearance(isFirstItemOfDay, isLastItemOfDay)

            binding.itemContainer.setOnClickListener { clickListener(entry) }
        }

        /**
         * Load ảnh từ local hoặc remote với Glide
         * Ưu tiên local path trước, fallback về remote path
         */
        private fun loadImage(entry: HistoryEntry) {
            val localPath = entry.localImagePath
            var loadModel: Any? = null

            if (!localPath.isNullOrEmpty()) {
                val file = File(localPath)
                if (file.exists()) {
                    loadModel = file
                }
            }

            if (loadModel == null && entry.imagePath.isNotEmpty()) {
                loadModel = if (entry.imagePath.startsWith("http")) {
                    entry.imagePath
                } else {
                    File(entry.imagePath)
                }
            }

            Glide.with(itemView)
                .load(loadModel)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .centerCrop()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(binding.ivHistoryImage)
        }

        /**
         * Setup date header nếu là item đầu tiên trong ngày
         */
        private fun setupDateHeader(isFirstItemOfDay: Boolean, dateTime: java.time.ZonedDateTime) {
            if (isFirstItemOfDay) {
                binding.tvDateHeader.text = dateFormatter.format(dateTime)
                binding.tvDateHeader.visibility = View.VISIBLE
            } else {
                binding.tvDateHeader.visibility = View.GONE
            }
        }

        /**
         * Setup rounded corners và margins để tạo border liên tục giữa các items
         * - Item đầu tiên: top corners bo tròn
         * - Item cuối cùng: bottom corners bo tròn
         * - Item giữa: không bo tròn, margin âm để border không bị gấp đôi
         */
        private fun setupCardAppearance(isFirstItemOfDay: Boolean, isLastItemOfDay: Boolean) {
            val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams
            layoutParams.topMargin = if (!isFirstItemOfDay) -strokeWidth else 0
            itemView.layoutParams = layoutParams

            val bgDrawable = GradientDrawable().apply {
                setColor(colorSurface)
                setStroke(strokeWidth, strokeColor)

                cornerRadii = when {
                    isFirstItemOfDay && isLastItemOfDay -> floatArrayOf(
                        radius, radius, radius, radius, radius, radius, radius, radius
                    )
                    isFirstItemOfDay -> floatArrayOf(
                        radius, radius, radius, radius, 0f, 0f, 0f, 0f
                    )
                    isLastItemOfDay -> floatArrayOf(
                        0f, 0f, 0f, 0f, radius, radius, radius, radius
                    )
                    else -> floatArrayOf(
                        0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f
                    )
                }
            }

            binding.itemContainer.background = bgDrawable
        }
    }
}