// FILE: nguyendevs/ecolens/EcoLens-312c2dae705bb34fd90d29e6d1b5003c678c945f/app/src/main/java/com/nguyendevs/ecolens/adapters/HistoryAdapter.kt

package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HistoryAdapter(
    private var historyList: List<HistoryEntry>,
    private val clickListener: (HistoryEntry) -> Unit,
    private val favoriteClickListener: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    fun updateList(newList: List<HistoryEntry>) {
        historyList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_entry, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = historyList[position]

        // Logic check ngày
        val isFirstItemOfDay = position == 0 || !isSameDay(entry.timestamp, historyList[position - 1].timestamp)
        val isLastItemOfDay = position == historyList.size - 1 || !isSameDay(entry.timestamp, historyList[position + 1].timestamp)

        holder.bind(entry, isFirstItemOfDay, isLastItemOfDay, clickListener)
    }

    override fun getItemCount(): Int = historyList.size

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        return dateFormatter.format(Date(timestamp1)) == dateFormatter.format(Date(timestamp2))
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivHistoryImage)
        private val tvCommonName: TextView = itemView.findViewById(R.id.tvHistoryCommonName)
        private val tvScientificName: TextView = itemView.findViewById(R.id.tvHistoryScientificName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvHistoryTime)
        private val tvDateHeader: TextView = itemView.findViewById(R.id.tvDateHeader)
        private val divider: View = itemView.findViewById(R.id.divider)
        private val itemContainer: View = itemView.findViewById(R.id.itemContainer)

        fun bind(
            entry: HistoryEntry,
            isFirstItemOfDay: Boolean,
            isLastItemOfDay: Boolean,
            clickListener: (HistoryEntry) -> Unit
        ) {
            val context = itemView.context

            // Data binding
            tvCommonName.text = entry.speciesInfo.commonName.ifEmpty { context.getString(R.string.unknown_common_name) }
            tvScientificName.text = entry.speciesInfo.scientificName.ifEmpty { context.getString(R.string.unknown_scientific_name) }
            tvTime.text = timeFormatter.format(Date(entry.timestamp))

            Glide.with(context)
                .load(entry.imagePath)
                .centerCrop()
                .into(ivImage)

            // 1. Xử lý Date Header
            if (isFirstItemOfDay) {
                tvDateHeader.text = dateFormatter.format(Date(entry.timestamp))
                tvDateHeader.visibility = View.VISIBLE
            } else {
                tvDateHeader.visibility = View.GONE
            }

            // 2. Xử lý Divider (Ẩn nếu là item cuối của nhóm)
            divider.visibility = if (isLastItemOfDay) View.GONE else View.VISIBLE

            // 3. Xử lý Bo góc Background (Tạo hiệu ứng Grouped List như ảnh)
            val bgDrawable = android.graphics.drawable.GradientDrawable()
            bgDrawable.setColor(context.getColor(R.color.white))
            val radius = context.resources.displayMetrics.density * 16 // 16dp

            if (isFirstItemOfDay && isLastItemOfDay) {
                // Item duy nhất trong ngày: Bo cả 4 góc
                bgDrawable.cornerRadii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
            } else if (isFirstItemOfDay) {
                // Đầu danh sách: Bo 2 góc trên
                bgDrawable.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
            } else if (isLastItemOfDay) {
                // Cuối danh sách: Bo 2 góc dưới
                bgDrawable.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius)
            } else {
                // Ở giữa: Không bo góc
                bgDrawable.cornerRadius = 0f
            }
            itemContainer.background = bgDrawable

            // --- SỬA LỖI TẠI ĐÂY ---
            // Đặt listener vào itemContainer thay vì itemView vì trong XML itemContainer có clickable=true
            itemContainer.setOnClickListener { clickListener(entry) }
        }
    }
}