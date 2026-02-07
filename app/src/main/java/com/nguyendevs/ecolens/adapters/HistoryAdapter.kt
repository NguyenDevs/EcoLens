package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
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

class HistoryAdapter(
    private val markwon: Markwon,
    private val clickListener: (HistoryEntry) -> Unit
) : ListAdapter<HistoryUiModel, RecyclerView.ViewHolder>(HistoryDiffCallback) {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private var isLoading = false
    private var lastPosition = -1

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        if (isLoading && position == super.getItemCount()) {
            return -1L
        }
        return getItem(position).entry.id.toLong()
    }

    fun setLoading(loading: Boolean) {
        if (isLoading == loading) return
        isLoading = loading
        if (isLoading) {
            notifyItemInserted(super.getItemCount())
        } else {
            notifyItemRemoved(super.getItemCount())
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (isLoading) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoading && position == super.getItemCount()) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_LOADING) {
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
            val item = getItem(position)
            holder.bind(item, clickListener)
            setAnimation(holder.itemView, position)
        }
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(viewToAnimate.context, R.anim.slide_in_bottom)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    object HistoryDiffCallback : DiffUtil.ItemCallback<HistoryUiModel>() {
        override fun areItemsTheSame(oldItem: HistoryUiModel, newItem: HistoryUiModel): Boolean {
            return oldItem.entry.id == newItem.entry.id
        }

        override fun areContentsTheSame(oldItem: HistoryUiModel, newItem: HistoryUiModel): Boolean {
            return oldItem == newItem
        }
        
        override fun getChangePayload(oldItem: HistoryUiModel, newItem: HistoryUiModel): Any? {
             return if (oldItem.entry.id == newItem.entry.id) {
                 newItem
             } else {
                 null
             }
        }
    }

    inner class HistoryViewHolder(private val binding: ItemSpeciesHistoryBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(
            uiModel: HistoryUiModel,
            clickListener: (HistoryEntry) -> Unit
        ) {
            val entry = uiModel.entry
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
            setupDateHeader(uiModel.isFirstOfDay, currentDateTime)
            setupCardAppearance(uiModel.isFirstOfDay, uiModel.isLastOfDay)
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
                    .override(200, 200)
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
