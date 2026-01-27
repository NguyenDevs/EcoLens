package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemSpeciesHistoryBinding
import com.nguyendevs.ecolens.model.history.HistoryEntry
import io.noties.markwon.Markwon
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Adapter hiển thị danh sách lịch sử nhận diện loài Tự động nhóm theo ngày với rounded corners và
 * border liên tục Sử dụng DiffUtil để tối ưu performance khi update list
 */
class HistoryAdapter(
        private var historyList: MutableList<HistoryEntry>,
        private val markwon: Markwon,
        private val clickListener: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private var isLoading = false

    // ==================== ADAPTER METHODS ====================

    fun updateList(newList: List<HistoryEntry>) {
        // Dùng cho pull-to-refresh hoặc load lần đầu
        val diffCallback =
                object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int = historyList.size
                    override fun getNewListSize(): Int = newList.size

                    override fun areItemsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int
                    ): Boolean {
                        return historyList[oldItemPosition].id == newList[newItemPosition].id
                    }

                    override fun areContentsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int
                    ): Boolean {
                        val oldItem = historyList[oldItemPosition]
                        val newItem = newList[newItemPosition]

                        val isContentSame =
                                oldItem.id == newItem.id &&
                                        oldItem.timestamp == newItem.timestamp &&
                                        oldItem.speciesInfo.commonName ==
                                                newItem.speciesInfo.commonName &&
                                        oldItem.speciesInfo.scientificName ==
                                                newItem.speciesInfo.scientificName &&
                                        oldItem.isFavorite == newItem.isFavorite &&
                                        oldItem.imagePath == newItem.imagePath

                        if (!isContentSame) return false

                        val oldIsFirst =
                                oldItemPosition == 0 ||
                                        !isSameDay(
                                                oldItem.timestamp,
                                                historyList[oldItemPosition - 1].timestamp
                                        )
                        val oldIsLast =
                                oldItemPosition == historyList.size - 1 ||
                                        !isSameDay(
                                                oldItem.timestamp,
                                                historyList[oldItemPosition + 1].timestamp
                                        )
                        val newIsFirst =
                                newItemPosition == 0 ||
                                        !isSameDay(
                                                newItem.timestamp,
                                                newList[newItemPosition - 1].timestamp
                                        )
                        val newIsLast =
                                newItemPosition == newList.size - 1 ||
                                        !isSameDay(
                                                newItem.timestamp,
                                                newList[newItemPosition + 1].timestamp
                                        )

                        return oldIsFirst == newIsFirst && oldIsLast == newIsLast
                    }
                }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        historyList = newList.toMutableList()
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Phương thức dùng cho Lazy Loading: Thêm dữ liệu vào cuối danh sách
     */
    fun addItems(newItems: List<HistoryEntry>) {
        val startPosition = historyList.size
        historyList.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)

        // Cập nhật item cuối cùng của trang trước đó (để fix border/background nếu cùng ngày)
        if (startPosition > 0 && newItems.isNotEmpty()) {
            notifyItemChanged(startPosition - 1)
        }
    }

    fun setLoading(loading: Boolean) {
        if (isLoading == loading) return
        isLoading = loading
        if (isLoading) {
            notifyItemInserted(historyList.size)
        } else {
            notifyItemRemoved(historyList.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoading && position == historyList.size) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_LOADING) {
            // Tạo CircularProgressIndicator bọc trong FrameLayout để căn giữa
            val context = parent.context
            val frameLayout = FrameLayout(context)
            val layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            frameLayout.layoutParams = layoutParams
            frameLayout.setPadding(0, 32, 0, 32)

            val progressBar = CircularProgressIndicator(context)
            progressBar.isIndeterminate = true
            
            val typedArray = context.resources.obtainTypedArray(R.array.gemini_colors)
            val colors = IntArray(typedArray.length())
            for (i in 0 until typedArray.length()) {
                colors[i] = typedArray.getColor(i, 0)
            }
            typedArray.recycle()
            progressBar.setIndicatorColor(*colors)

            val progressParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            progressParams.gravity = Gravity.CENTER
            frameLayout.addView(progressBar, progressParams)

            return object : RecyclerView.ViewHolder(frameLayout) {}
        }

        val binding = ItemSpeciesHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HistoryViewHolder) {
            val entry = historyList[position]

            val isFirstItemOfDay =
                position == 0 || !isSameDay(entry.timestamp, historyList[position - 1].timestamp)
            
            // Logic kiểm tra item cuối cùng trong ngày
            val isLastItemOfDay = if (position == historyList.size - 1) {
                // Nếu là item cuối cùng của list hiện tại
                // Nếu đang loading thì tạm coi là cuối ngày, khi load thêm sẽ update lại sau
                true 
            } else {
                !isSameDay(entry.timestamp, historyList[position + 1].timestamp)
            }

            holder.bind(entry, isFirstItemOfDay, isLastItemOfDay, clickListener)
        }
    }

    override fun getItemCount(): Int = historyList.size + if (isLoading) 1 else 0

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val date1 = Instant.ofEpochMilli(timestamp1).atZone(ZoneId.systemDefault()).toLocalDate()
        val date2 = Instant.ofEpochMilli(timestamp2).atZone(ZoneId.systemDefault()).toLocalDate()
        return date1 == date2
    }

    // ==================== VIEW HOLDER ====================

    inner class HistoryViewHolder(private val binding: ItemSpeciesHistoryBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(
                entry: HistoryEntry,
                isFirstItemOfDay: Boolean,
                isLastItemOfDay: Boolean,
                clickListener: (HistoryEntry) -> Unit
        ) {
            val currentDateTime =
                    Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())

            val commonText =
                    entry.speciesInfo.commonName.ifEmpty {
                        itemView.context.getString(R.string.unknown_common_name)
                    }
            val scientificText =
                    entry.speciesInfo.scientificName.ifEmpty {
                        itemView.context.getString(R.string.unknown_scientific_name)
                    }

            markwon.setMarkdown(binding.tvHistoryCommonName, commonText)
            markwon.setMarkdown(binding.tvHistoryScientificName, scientificText)
            binding.tvHistoryTime.text = timeFormatter.format(currentDateTime)

            loadImage(entry)
            setupDateHeader(isFirstItemOfDay, currentDateTime)
            setupCardAppearance(isFirstItemOfDay, isLastItemOfDay)
            setupConfidenceBadge(entry)

            binding.itemContainer.setOnClickListener { clickListener(entry) }
        }

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
                loadModel =
                        if (entry.imagePath.startsWith("http")) {
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

        private fun setupDateHeader(isFirstItemOfDay: Boolean, dateTime: java.time.ZonedDateTime) {
            if (isFirstItemOfDay) {
                binding.tvDateHeader.text = dateFormatter.format(dateTime)
                binding.tvDateHeader.visibility = View.VISIBLE
            } else {
                binding.tvDateHeader.visibility = View.GONE
            }
        }

        private fun setupCardAppearance(isFirstItemOfDay: Boolean, isLastItemOfDay: Boolean) {
            binding.divider.visibility = if (!isFirstItemOfDay) View.VISIBLE else View.GONE
            val backgroundRes =
                    when {
                        isFirstItemOfDay && isLastItemOfDay -> R.drawable.bg_history_item_single
                        isFirstItemOfDay -> R.drawable.bg_history_item_top
                        isLastItemOfDay -> R.drawable.bg_history_item_bottom
                        else -> R.drawable.bg_history_item_middle
                    }

            binding.itemContainer.setBackgroundResource(backgroundRes)
        }

        private fun setupConfidenceBadge(entry: HistoryEntry) {
            val confidence = entry.speciesInfo.confidence
            val context = itemView.context

            val (colorRes, iconRes) =
                    when {
                        confidence >= 50 ->
                                Pair(R.color.confidence_high, R.drawable.ic_check_circle)
                        confidence >= 25 ->
                                Pair(R.color.confidence_medium, R.drawable.ic_check_warning_circle)
                        else -> Pair(R.color.confidence_low, R.drawable.ic_info)
                    }

            val color = ContextCompat.getColor(context, colorRes)
            binding.badgeSuccess.setCardBackgroundColor(color)
            binding.ivBadgeIcon.setImageResource(iconRes)
        }
    }
}
