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

    // Cập nhật danh sách lịch sử
    fun updateList(newList: List<HistoryEntry>) {
        historyList = newList
        notifyDataSetChanged()
    }

    // Tạo ViewHolder mới
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_entry, parent, false)
        return HistoryViewHolder(view)
    }

    // Gắn dữ liệu vào ViewHolder
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = historyList[position]

        val isFirstItemOfDay = position == 0 || !isSameDay(entry.timestamp, historyList[position - 1].timestamp)
        val isLastItemOfDay = position == historyList.size - 1 || !isSameDay(entry.timestamp, historyList[position + 1].timestamp)

        holder.bind(entry, isFirstItemOfDay, isLastItemOfDay, clickListener)
    }

    // Lấy số lượng item
    override fun getItemCount(): Int = historyList.size

    // Kiểm tra hai timestamp có cùng ngày không
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        return dateFormatter.format(Date(timestamp1)) == dateFormatter.format(Date(timestamp2))
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemContainer: View = itemView.findViewById(R.id.itemContainer)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivHistoryImage)
        private val tvCommonName: TextView = itemView.findViewById(R.id.tvHistoryCommonName)
        private val tvDateHeader: TextView = itemView.findViewById(R.id.tvDateHeader)
        private val tvScientificName: TextView = itemView.findViewById(R.id.tvHistoryScientificName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvHistoryTime)

        // Gắn dữ liệu vào các view
        fun bind(
            entry: HistoryEntry,
            isFirstItemOfDay: Boolean,
            isLastItemOfDay: Boolean,
            clickListener: (HistoryEntry) -> Unit
        ) {
            val context = itemView.context

            tvCommonName.text = entry.speciesInfo.commonName.ifEmpty { context.getString(R.string.unknown_common_name) }
            tvScientificName.text = entry.speciesInfo.scientificName.ifEmpty { context.getString(R.string.unknown_scientific_name) }
            tvTime.text = timeFormatter.format(Date(entry.timestamp))

            Glide.with(context)
                .load(entry.imagePath)
                .centerCrop()
                .into(ivImage)

            // Xử lý hiển thị Header ngày
            if (isFirstItemOfDay) {
                tvDateHeader.text = dateFormatter.format(Date(entry.timestamp))
                tvDateHeader.visibility = View.VISIBLE
            } else {
                tvDateHeader.visibility = View.GONE
            }

            // --- ĐOẠN CODE MỚI: Xử lý margin âm để gộp viền ---
            // Tính độ dày viền (1dp) ra pixel
            val strokeWidth = (1 * context.resources.displayMetrics.density).toInt()

            // Lấy LayoutParams của itemView (item gốc của row)
            val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams

            if (!isFirstItemOfDay) {
                // Nếu không phải đầu ngày (tức là nằm dưới item khác cùng nhóm),
                // kéo lên 1 khoảng bằng độ dày viền để 2 viền chồng lên nhau
                layoutParams.topMargin = -strokeWidth
            } else {
                // Nếu là đầu ngày, giữ nguyên margin (không kéo lên)
                layoutParams.topMargin = 0
            }
            itemView.layoutParams = layoutParams
            // ---------------------------------------------------

            // Tạo Background với bo góc và viền
            val bgDrawable = android.graphics.drawable.GradientDrawable()
            bgDrawable.setColor(context.getColor(R.color.white))

            val strokeColor = android.graphics.Color.parseColor("#E0E0E0")
            bgDrawable.setStroke(strokeWidth, strokeColor)

            val radius = context.resources.displayMetrics.density * 16

            if (isFirstItemOfDay && isLastItemOfDay) {
                bgDrawable.cornerRadii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
            } else if (isFirstItemOfDay) {
                bgDrawable.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
            } else if (isLastItemOfDay) {
                bgDrawable.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius)
            } else {
                bgDrawable.cornerRadius = 0f
            }
            itemContainer.background = bgDrawable

            itemContainer.setOnClickListener { clickListener(entry) }
        }
    }
}