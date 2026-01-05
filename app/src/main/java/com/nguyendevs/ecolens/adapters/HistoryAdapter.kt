package com.nguyendevs.ecolens.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemHistoryEntryModernBinding
import com.nguyendevs.ecolens.model.HistoryEntry
import io.noties.markwon.Markwon
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class HistoryAdapter(
    private var historyList: List<HistoryEntry>,
    private val markwon: Markwon,
    private val clickListener: (HistoryEntry) -> Unit,
    private val favoriteClickListener: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun updateList(newList: List<HistoryEntry>) {
        historyList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryEntryModernBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = historyList[position]

        val isFirstItemOfDay = position == 0 || !isSameDay(entry.timestamp, historyList[position - 1].timestamp)
        val isLastItemOfDay = position == historyList.size - 1 || !isSameDay(entry.timestamp, historyList[position + 1].timestamp)

        holder.bind(entry, isFirstItemOfDay, isLastItemOfDay, clickListener)
    }

    override fun getItemCount(): Int = historyList.size

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val date1 = Instant.ofEpochMilli(timestamp1).atZone(ZoneId.systemDefault()).toLocalDate()
        val date2 = Instant.ofEpochMilli(timestamp2).atZone(ZoneId.systemDefault()).toLocalDate()
        return date1 == date2
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryEntryModernBinding) : RecyclerView.ViewHolder(binding.root) {

        private val radius = itemView.resources.displayMetrics.density * 16
        private val strokeWidth = (1 * itemView.resources.displayMetrics.density).toInt()

        private val colorSurface = ContextCompat.getColor(itemView.context, R.color.surface)
        private val strokeColor = ContextCompat.getColor(itemView.context, R.color.border_normal)

        fun bind(
            entry: HistoryEntry,
            isFirstItemOfDay: Boolean,
            isLastItemOfDay: Boolean,
            clickListener: (HistoryEntry) -> Unit
        ) {
            val currentDateTime = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())

            val commonText = entry.speciesInfo.commonName.ifEmpty { itemView.context.getString(R.string.unknown_common_name) }
            val scientificText = entry.speciesInfo.scientificName.ifEmpty { itemView.context.getString(R.string.unknown_scientific_name) }

            markwon.setMarkdown(binding.tvHistoryCommonName, commonText)
            markwon.setMarkdown(binding.tvHistoryScientificName, scientificText)

            binding.tvHistoryTime.text = timeFormatter.format(currentDateTime)

            val localPath = entry.localImagePath
            var loadModel: Any? = null

            // Ưu tiên hiển thị ảnh từ local nếu tồn tại
            if (!localPath.isNullOrEmpty()) {
                val file = File(localPath)
                if (file.exists()) {
                    loadModel = file
                }
            }

            // Nếu không có ảnh local, kiểm tra xem imagePath có phải là đường dẫn file không (không phải http)
            // Nếu là http (Firebase), ta KHÔNG load ngay mà để HistoryManager tải ngầm và update DB sau.
            // Điều này đáp ứng yêu cầu: "hiển thị hết nội dung chữ... rồi mới bắt đầu tải ảnh dưới nền"
            if (loadModel == null && entry.imagePath.isNotEmpty() && !entry.imagePath.startsWith("http")) {
                loadModel = entry.imagePath
            }

            Glide.with(itemView)
                .load(loadModel) // Nếu null, Glide sẽ hiện placeholder
                .centerCrop()
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_broken_image)
                .into(binding.ivHistoryImage)

            if (isFirstItemOfDay) {
                binding.tvDateHeader.text = dateFormatter.format(currentDateTime)
                binding.tvDateHeader.visibility = View.VISIBLE
            } else {
                binding.tvDateHeader.visibility = View.GONE
            }

            val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams
            layoutParams.topMargin = if (!isFirstItemOfDay) -strokeWidth else 0
            itemView.layoutParams = layoutParams

            val bgDrawable = GradientDrawable().apply {
                setColor(colorSurface)
                setStroke(strokeWidth, strokeColor)

                cornerRadii = when {
                    isFirstItemOfDay && isLastItemOfDay -> floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
                    isFirstItemOfDay -> floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                    isLastItemOfDay -> floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius)
                    else -> floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                }
            }

            binding.itemContainer.background = bgDrawable

            binding.itemContainer.setOnClickListener { clickListener(entry) }
        }
    }
}