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
        holder.bind(entry, clickListener, favoriteClickListener)

        val showDateHeader = position == 0 || !isSameDay(entry.timestamp, historyList[position - 1].timestamp)
        holder.bindDateHeader(entry.timestamp, showDateHeader)
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
        private val ivFavorite: ImageView = itemView.findViewById(R.id.ivHistoryFavorite)

        fun bind(
            entry: HistoryEntry,
            clickListener: (HistoryEntry) -> Unit,
            favoriteClickListener: (HistoryEntry) -> Unit
        ) {
            val context = itemView.context

            tvCommonName.text = entry.speciesInfo.commonName.ifEmpty {
                context.getString(R.string.unknown_common_name)
            }
            tvScientificName.text = entry.speciesInfo.scientificName.ifEmpty {
                context.getString(R.string.unknown_scientific_name)
            }
            tvTime.text = "${context.getString(R.string.time_prefix)}${timeFormatter.format(Date(entry.timestamp))}"

            Glide.with(itemView.context)
                .load(entry.imagePath)
                .centerCrop()
                .into(ivImage)

            // Cập nhật icon yêu thích
            updateFavoriteIcon(ivFavorite, entry.isFavorite)

            itemView.setOnClickListener {
                clickListener(entry)
            }

            ivFavorite.setOnClickListener {
                favoriteClickListener(entry)
            }
        }

        fun bindDateHeader(timestamp: Long, show: Boolean) {
            if (show) {
                tvDateHeader.text = dateFormatter.format(Date(timestamp))
                tvDateHeader.visibility = View.VISIBLE
            } else {
                tvDateHeader.visibility = View.GONE
            }
        }

        private fun updateFavoriteIcon(imageView: ImageView, isFavorite: Boolean) {
            if (isFavorite) {
                imageView.setImageResource(R.drawable.ic_favorite)
                imageView.setColorFilter(android.graphics.Color.RED)
            } else {
                imageView.setImageResource(R.drawable.ic_favorite)
                imageView.setColorFilter(android.graphics.Color.GRAY)
            }
        }
    }
}