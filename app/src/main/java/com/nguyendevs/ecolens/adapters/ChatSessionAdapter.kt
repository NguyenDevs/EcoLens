package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.databinding.ItemChatHistoryBinding
import com.nguyendevs.ecolens.model.chat.ChatSession
import io.noties.markwon.Markwon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Adapter hiển thị danh sách các phiên chat
 * Tự động nhóm theo ngày với date header và hiệu ứng loading khi click
 */
class ChatSessionAdapter(
    private var sessions: List<ChatSession>,
    private val onClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatSessionAdapter.ChatSessionViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val loadingInterpolator = LinearInterpolator()
    private lateinit var markwon: Markwon

    // ==================== ADAPTER METHODS ====================

    /**
     * Cập nhật danh sách phiên chat
     */
    fun updateList(newList: List<ChatSession>) {
        sessions = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSessionViewHolder {
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

    override fun onBindViewHolder(holder: ChatSessionViewHolder, position: Int) {
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

    override fun getItemCount() = sessions.size

    // ==================== VIEW HOLDER ====================

    inner class ChatSessionViewHolder(
        private val binding: ItemChatHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind dữ liệu phiên chat vào view
         * @param showHeader Hiển thị date header nếu là phiên đầu tiên trong ngày
         */
        fun bind(session: ChatSession, showHeader: Boolean, dateTime: java.time.ZonedDateTime) {
            binding.tvDateHeader.visibility = if (showHeader) View.VISIBLE else View.GONE
            if (showHeader) {
                binding.tvDateHeader.text = dateFormatter.format(dateTime)
            }

            binding.ivLoadingRing.visibility = View.INVISIBLE
            binding.ivLoadingRing.animate().cancel()
            binding.tvTitle.text = session.title
            markwon.setMarkdown(binding.tvLastMessage, session.lastMessage)
            binding.tvTime.text = timeFormatter.format(dateTime)

            binding.cardSession.setOnClickListener {
                animateLoading()
                onClick(session)
            }
        }

        /**
         * Hiệu ứng loading ring khi click vào phiên chat
         * Animation xoay 360 độ rồi fade out
         */
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