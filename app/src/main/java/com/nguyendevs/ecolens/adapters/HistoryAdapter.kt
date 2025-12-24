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
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HistoryAdapter(
    private var historyList: List<HistoryEntry>,
    private val markwon: Markwon,
    private val clickListener: (HistoryEntry) -> Unit,
    private val favoriteClickListener: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    // Khởi tạo các formatter dùng chung để tránh tạo lại nhiều lần
    companion object {
        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    // Cập nhật danh sách lịch sử và refresh giao diện
    fun updateList(newList: List<HistoryEntry>) {
        historyList = newList
        notifyDataSetChanged()
    }

    // Tạo ViewHolder mới (Markwon đã được inject nên không cần khởi tạo ở đây)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_entry, parent, false)
        return HistoryViewHolder(view)
    }

    // Xác định vị trí item và gọi hàm bind dữ liệu
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = historyList[position]

        val isFirstItemOfDay = position == 0 || !isSameDay(entry.timestamp, historyList[position - 1].timestamp)
        val isLastItemOfDay = position == historyList.size - 1 || !isSameDay(entry.timestamp, historyList[position + 1].timestamp)

        holder.bind(entry, isFirstItemOfDay, isLastItemOfDay, clickListener)
    }

    // Trả về tổng số lượng item trong danh sách
    override fun getItemCount(): Int = historyList.size

    // Kiểm tra xem hai mốc thời gian có thuộc cùng một ngày không
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

        // Gắn dữ liệu vào các view component và xử lý giao diện nhóm theo ngày
        fun bind(
            entry: HistoryEntry,
            isFirstItemOfDay: Boolean,
            isLastItemOfDay: Boolean,
            clickListener: (HistoryEntry) -> Unit
        ) {
            val context = itemView.context

            val commonText = entry.speciesInfo.commonName.ifEmpty { context.getString(R.string.unknown_common_name) }
            val scientificText = entry.speciesInfo.scientificName.ifEmpty { context.getString(R.string.unknown_scientific_name) }

            markwon.setMarkdown(tvCommonName, commonText)
            markwon.setMarkdown(tvScientificName, scientificText)

            tvTime.text = timeFormatter.format(Date(entry.timestamp))

            Glide.with(context)
                .load(entry.imagePath)
                .centerCrop()
                .into(ivImage)

            if (isFirstItemOfDay) {
                tvDateHeader.text = dateFormatter.format(Date(entry.timestamp))
                tvDateHeader.visibility = View.VISIBLE
            } else {
                tvDateHeader.visibility = View.GONE
            }

            val strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
            val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams

            if (!isFirstItemOfDay) {
                layoutParams.topMargin = -strokeWidth
            } else {
                layoutParams.topMargin = 0
            }
            itemView.layoutParams = layoutParams

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