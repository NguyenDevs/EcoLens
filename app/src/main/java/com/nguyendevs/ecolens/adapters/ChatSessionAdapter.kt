package com.nguyendevs.ecolens.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemChatHistoryBinding
import com.nguyendevs.ecolens.models.chat.ChatSession
import io.noties.markwon.Markwon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Adapter hiển thị danh sách phiên chat với phân nhóm theo ngày. */
class ChatSessionAdapter(
    private var sessions: MutableList<ChatSession>,
    private val onClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val loadingInterpolator = LinearInterpolator()
    private lateinit var markwon: Markwon
    private var isLoading = false

    /** Cập nhật toàn bộ danh sách phiên chat. */
    fun updateList(newList: List<ChatSession>) {
        sessions.clear()
        sessions.addAll(newList)
        notifyDataSetChanged()
    }

    /** Thêm các phiên chat mới vào cuối danh sách. */
    fun addItems(newItems: List<ChatSession>) {
        val startPosition = sessions.size
        sessions.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    /** Đặt trạng thái loading và thêm/xóa item loading indicator. */
    fun setLoading(loading: Boolean) {
        if (isLoading == loading) return
        isLoading = loading
        if (isLoading) {
            notifyItemInserted(sessions.size)
        } else {
            notifyItemRemoved(sessions.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoading && position == sessions.size) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    /** Tạo ViewHolder phù hợp (item hoặc loading). */
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

        if (!::markwon.isInitialized) {
            markwon = Markwon.create(parent.context)
        }
        val binding = ItemChatHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatSessionViewHolder(binding)
    }

    /** Bind dữ liệu phiên chat, bao gồm header ngày nếu là phiên đầu trong ngày. */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ChatSessionViewHolder) {
            val session = sessions[position]
            val currentDateTime = Instant.ofEpochMilli(session.timestamp)
                .atZone(ZoneId.systemDefault())

            val isFirstOfDay = if (position == 0) {
                true
            } else {
                val prevDateTime = Instant.ofEpochMilli(sessions[position - 1].timestamp)
                    .atZone(ZoneId.systemDefault())
                currentDateTime.toLocalDate() != prevDateTime.toLocalDate()
            }

            holder.bind(session, isFirstOfDay, currentDateTime)
        }
    }

    override fun getItemCount() = sessions.size + if (isLoading) 1 else 0

    /** ViewHolder cho một phiên chat, có animation loading khi click. */
    inner class ChatSessionViewHolder(
        private val binding: ItemChatHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Hiển thị thông tin phiên chat và header ngày nếu cần. */
        fun bind(session: ChatSession, showHeader: Boolean, dateTime: java.time.ZonedDateTime) {
            binding.dateHeaderContainer.visibility = if (showHeader) View.VISIBLE else View.GONE
            if (showHeader) {
                binding.tvDateHeader.text = when {
                    isToday(dateTime) -> binding.root.context.getString(R.string.today).uppercase()
                    isYesterday(dateTime) -> binding.root.context.getString(R.string.yesterday).uppercase()
                    else -> dateFormatter.format(dateTime).uppercase()
                }
            }

            binding.ivLoadingRing.visibility = View.INVISIBLE
            binding.ivLoadingRing.animate().cancel()
            binding.tvTitle.text = session.title
            markwon.setMarkdown(binding.tvLastMessage, session.lastMessage)
            binding.tvTime.text = timeFormatter.format(dateTime)

            binding.itemContainer.setOnClickListener {
                animateLoading()
                onClick(session)
            }
        }

        /** Kiểm tra ngày có phải hôm nay không. */
        private fun isToday(dt: java.time.ZonedDateTime): Boolean {
            return dt.toLocalDate() == java.time.LocalDate.now()
        }

        /** Kiểm tra ngày có phải hôm qua không. */
        private fun isYesterday(dt: java.time.ZonedDateTime): Boolean {
            return dt.toLocalDate() == java.time.LocalDate.now().minusDays(1)
        }

        /** Chạy animation loading ring khi click vào phiên chat. */
        private fun animateLoading() {
            binding.ivLoadingRing.apply {
                visibility = View.VISIBLE
                alpha = 1f
                animate()
                    .rotationBy(360f)
                    .setDuration(800)
                    .setInterpolator(loadingInterpolator)
                    .withEndAction {
                        animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction {
                                visibility = View.INVISIBLE
                                rotation = 0f
                            }
                            .start()
                    }
                    .start()
            }
        }
    }
}