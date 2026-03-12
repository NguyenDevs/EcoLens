package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemSpeciesHistoryBinding
import com.nguyendevs.ecolens.databinding.ItemSpeciesHistoryGridBinding
import com.nguyendevs.ecolens.models.history.HistoryEntry
import io.noties.markwon.Markwon
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data class HistoryUiModel(
    val entry: HistoryEntry,
    val isFirstOfDay: Boolean,
    val isLastOfDay: Boolean
)

enum class HistoryViewMode { LIST, GRID }

class HistoryAdapter(
    private val markwon: Markwon,
    private val clickListener: (HistoryEntry) -> Unit
) : ListAdapter<HistoryUiModel, RecyclerView.ViewHolder>(HistoryDiffCallback) {

    companion object {
        private const val VIEW_TYPE_LIST = 0
        private const val VIEW_TYPE_GRID = 1
        private const val VIEW_TYPE_LOADING = 2
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private var isLoading = false
    private var lastPosition = -1
    var viewMode: HistoryViewMode = HistoryViewMode.LIST
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        if (isLoading && position == super.getItemCount()) return -1L
        return getItem(position).entry.id.toLong()
    }

    fun setLoading(loading: Boolean) {
        if (isLoading == loading) return
        isLoading = loading
        if (isLoading) notifyItemInserted(super.getItemCount())
        else notifyItemRemoved(super.getItemCount())
    }

    override fun getItemCount() = super.getItemCount() + if (isLoading) 1 else 0

    override fun getItemViewType(position: Int): Int {
        if (isLoading && position == super.getItemCount()) return VIEW_TYPE_LOADING
        return if (viewMode == HistoryViewMode.LIST) VIEW_TYPE_LIST else VIEW_TYPE_GRID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LOADING -> {
                val context = parent.context
                val frame = FrameLayout(context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, 32, 0, 32)
                }
                val progress = CircularProgressIndicator(context).apply {
                    isIndeterminate = true
                    val ta = context.resources.obtainTypedArray(R.array.gemini_colors)
                    val colors = IntArray(ta.length()) { i -> ta.getColor(i, 0) }
                    ta.recycle()
                    setIndicatorColor(*colors)
                }
                val lp = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER }
                frame.addView(progress, lp)
                object : RecyclerView.ViewHolder(frame) {}
            }
            VIEW_TYPE_GRID -> {
                val binding = ItemSpeciesHistoryGridBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                GridViewHolder(binding)
            }
            else -> {
                val binding = ItemSpeciesHistoryBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ListViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ListViewHolder -> {
                holder.bind(getItem(position), clickListener)
                setAnimation(holder.itemView, position)
            }
            is GridViewHolder -> {
                holder.bind(getItem(position), clickListener)
                setAnimation(holder.itemView, position)
            }
        }
    }

    private fun setAnimation(view: View, position: Int) {
        if (position > lastPosition) {
            val anim = AnimationUtils.loadAnimation(view.context, R.anim.slide_in_bottom)
            view.startAnimation(anim)
            lastPosition = position
        }
    }

    object HistoryDiffCallback : DiffUtil.ItemCallback<HistoryUiModel>() {
        override fun areItemsTheSame(o: HistoryUiModel, n: HistoryUiModel) = o.entry.id == n.entry.id
        override fun areContentsTheSame(o: HistoryUiModel, n: HistoryUiModel) = o == n
        override fun getChangePayload(o: HistoryUiModel, n: HistoryUiModel) =
            if (o.entry.id == n.entry.id) n else null
    }

    inner class ListViewHolder(private val b: ItemSpeciesHistoryBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(uiModel: HistoryUiModel, click: (HistoryEntry) -> Unit) {
            val entry = uiModel.entry
            val dt = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())

            markwon.setMarkdown(b.tvHistoryCommonName, entry.speciesInfo.commonName.ifEmpty {
                itemView.context.getString(R.string.unknown_common_name)
            })
            markwon.setMarkdown(b.tvHistoryScientificName, entry.speciesInfo.scientificName.ifEmpty {
                itemView.context.getString(R.string.unknown_scientific_name)
            })

            b.tvHistoryTime.text = timeFormatter.format(dt)
            b.tvConfidence.text = "${entry.speciesInfo.confidence.toInt()}%"

            setupCategoryBadge(b.tvCategoryBadge, entry.speciesInfo.kingdom)
            setupDateHeader(uiModel.isFirstOfDay, dt)
            setupCardAppearance(uiModel.isFirstOfDay, uiModel.isLastOfDay)
            setupConfidenceBadge(entry)
            loadImage(entry, b.ivHistoryImage)

            b.itemContainer.setOnClickListener { click(entry) }
        }

        private fun setupDateHeader(isFirst: Boolean, dt: java.time.ZonedDateTime) {
            b.dateHeaderContainer.visibility = if (isFirst) View.VISIBLE else View.GONE
            if (isFirst) {
                b.tvDateHeader.text = when {
                    isToday(dt) -> b.root.context.getString(R.string.today).uppercase()
                    isYesterday(dt) -> b.root.context.getString(R.string.yesterday).uppercase()
                    else -> dateFormatter.format(dt).uppercase()
                }
            }
            b.timelineLine.visibility = if (viewMode == HistoryViewMode.LIST) View.VISIBLE else View.GONE
        }

        private fun isToday(dt: java.time.ZonedDateTime): Boolean {
            return dt.toLocalDate() == java.time.LocalDate.now()
        }

        private fun isYesterday(dt: java.time.ZonedDateTime): Boolean {
            return dt.toLocalDate() == java.time.LocalDate.now().minusDays(1)
        }

        private fun setupCardAppearance(isFirst: Boolean, isLast: Boolean) {
            val bg = when {
                isFirst && isLast -> R.drawable.bg_history_item_single
                isFirst -> R.drawable.bg_history_item_top
                isLast -> R.drawable.bg_history_item_bottom
                else -> R.drawable.bg_history_item_middle
            }
            b.itemContainer.setBackgroundResource(bg)
            b.itemDivider.visibility = if (isLast) View.GONE else View.VISIBLE
        }

        private fun setupConfidenceBadge(entry: HistoryEntry) {
            val (colorRes, iconRes) = when {
                entry.speciesInfo.confidence >= 50 -> Pair(R.color.confidence_high, R.drawable.ic_check_circle)
                entry.speciesInfo.confidence >= 25 -> Pair(R.color.confidence_medium, R.drawable.ic_check_warning_circle)
                else -> Pair(R.color.confidence_low, R.drawable.ic_info)
            }
            b.badgeSuccess.setCardBackgroundColor(ContextCompat.getColor(itemView.context, colorRes))
            b.ivBadgeIcon.setImageResource(iconRes)
        }
    }

    inner class GridViewHolder(private val b: ItemSpeciesHistoryGridBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(uiModel: HistoryUiModel, click: (HistoryEntry) -> Unit) {
            val entry = uiModel.entry
            val dt = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())

            markwon.setMarkdown(b.tvHistoryCommonName, entry.speciesInfo.commonName.ifEmpty {
                itemView.context.getString(R.string.unknown_common_name)
            })
            markwon.setMarkdown(b.tvHistoryScientificName, entry.speciesInfo.scientificName.ifEmpty {
                itemView.context.getString(R.string.unknown_scientific_name)
            })

            b.tvHistoryTime.text = timeFormatter.format(dt)
            b.tvConfidence.text = "${entry.speciesInfo.confidence.toInt()}%"

            if (uiModel.isFirstOfDay) {
                b.tvDateHeader.visibility = View.VISIBLE
                b.tvDateHeader.text = dateFormatter.format(dt).uppercase()
            } else {
                b.tvDateHeader.visibility = View.GONE
            }

            setupConfidenceBadge(entry)
            loadImage(entry, b.ivHistoryImage)

            b.itemContainer.setOnClickListener { click(entry) }
        }

        private fun setupConfidenceBadge(entry: HistoryEntry) {
            val (colorRes, iconRes) = when {
                entry.speciesInfo.confidence >= 50 -> Pair(R.color.confidence_high, R.drawable.ic_check_circle)
                entry.speciesInfo.confidence >= 25 -> Pair(R.color.confidence_medium, R.drawable.ic_check_warning_circle)
                else -> Pair(R.color.confidence_low, R.drawable.ic_info)
            }
            b.badgeSuccess.setCardBackgroundColor(ContextCompat.getColor(itemView.context, colorRes))
            b.ivBadgeIcon.setImageResource(iconRes)
        }
    }

    private fun setupCategoryBadge(tv: TextView, kingdom: String) {
        val k = kingdom.lowercase()
        when {
            k.contains("animal") || k.contains("động vật") -> {
                tv.setBackgroundResource(R.drawable.bg_badge_animal)
                tv.setTextColor(0xFF92400E.toInt())
                tv.text = tv.context.getString(R.string.history_chipAnimals)
            }
            k.contains("plant") || k.contains("thực vật") -> {
                tv.setBackgroundResource(R.drawable.bg_badge_plant)
                tv.setTextColor(0xFF065F46.toInt())
                tv.text = tv.context.getString(R.string.history_chipPlants)
            }
            k.contains("fungi") || k.contains("nấm") -> {
                tv.setBackgroundResource(R.drawable.bg_badge_fungi)
                tv.setTextColor(0xFF7E22CE.toInt()) // purple-700
                tv.text = tv.context.getString(R.string.history_chipFungi)
            }
            else -> {
                tv.setBackgroundResource(R.drawable.bg_badge_animal)
                tv.setTextColor(0xFF92400E.toInt())
                tv.text = kingdom
            }
        }
    }

    private fun loadImage(entry: HistoryEntry, imageView: com.google.android.material.imageview.ShapeableImageView) {
        val localPath = entry.localImagePath
        var model: Any? = null
        if (!localPath.isNullOrEmpty()) {
            val f = File(localPath)
            if (f.exists()) model = f
        }
        if (model == null && entry.imagePath.isNotEmpty()) {
            model = if (entry.imagePath.startsWith("http")) entry.imagePath else File(entry.imagePath)
        }
        Glide.with(imageView)
            .load(model)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .centerCrop()
            .override(200, 200)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .into(imageView)
    }
}