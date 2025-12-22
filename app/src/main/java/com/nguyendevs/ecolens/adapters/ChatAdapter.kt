package com.nguyendevs.ecolens.adapters

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.ChatMessage

class ChatAdapter(private val actionListener: OnChatActionListener) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    interface OnChatActionListener {
        fun onCopy(text: String)
        fun onShare(text: String)
        fun onRenew(position: Int, message: ChatMessage)
    }

    private val messages = mutableListOf<ChatMessage>()

    fun submitList(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message, position)

        if (!message.isLoading && !message.isStreaming) {
            if (message.isUser) {
                holder.cardView.setOnLongClickListener {
                    actionListener.onCopy(message.content)
                    true
                }
                holder.cardView.setOnClickListener(null)
            } else {
                holder.cardView.setOnLongClickListener(null)
                val plainText = Html.fromHtml(message.content).toString()
                holder.btnCopyAi.setOnClickListener { actionListener.onCopy(plainText) }
                holder.btnShareAi.setOnClickListener { actionListener.onShare(plainText) }
                holder.btnRenewAi.setOnClickListener { actionListener.onRenew(position, message) }
            }
        } else {
            // Disable interactions khi đang streaming
            holder.cardView.setOnLongClickListener(null)
            holder.cardView.setOnClickListener(null)
            holder.btnCopyAi.setOnClickListener(null)
            holder.btnShareAi.setOnClickListener(null)
            holder.btnRenewAi.setOnClickListener(null)
        }
    }

    override fun onViewRecycled(holder: ChatViewHolder) {
        super.onViewRecycled(holder)
        holder.stopAnimation()
    }

    override fun getItemCount(): Int = messages.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.chatContainer)
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardMessage)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val layoutAiActions: LinearLayout = itemView.findViewById(R.id.layoutAiActions)
        val btnCopyAi: ImageView = itemView.findViewById(R.id.btnCopyAi)
        val btnShareAi: ImageView = itemView.findViewById(R.id.btnShareAi)
        val btnRenewAi: ImageView = itemView.findViewById(R.id.btnRenewAi)

        private val handler = Handler(Looper.getMainLooper())
        private var loopCount = 0
        private var cursorAnimator: ValueAnimator? = null

        private val loadingAnimateRunnable = object : Runnable {
            override fun run() {
                loopCount++
                val baseText = "..."
                val spannable = SpannableString(baseText)
                val visibleDots = (loopCount % 3) + 1
                if (visibleDots < 3) {
                    spannable.setSpan(ForegroundColorSpan(Color.TRANSPARENT), visibleDots, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                tvMessage.text = spannable
                handler.postDelayed(this, 400)
            }
        }

        fun stopAnimation() {
            handler.removeCallbacks(loadingAnimateRunnable)
            cursorAnimator?.cancel()
            cursorAnimator = null
        }

        fun bind(message: ChatMessage, position: Int) {
            stopAnimation()
            layoutAiActions.visibility = View.GONE

            when {
                message.isLoading -> {
                    // Loading state (chờ response)
                    container.gravity = Gravity.START
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                    tvMessage.text = "..."
                    loopCount = 0
                    loadingAnimateRunnable.run()
                }
                message.isStreaming -> {
                    // Streaming state - hiển thị nội dung đang được streaming
                    container.gravity = Gravity.START
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))

                    val formattedText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(message.content + "▌", Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        @Suppress("DEPRECATION") Html.fromHtml(message.content + "▌")
                    }
                    tvMessage.text = formattedText

                    // Thêm hiệu ứng nhấp nháy cursor
                    startCursorAnimation()

                    // Không hiển thị actions khi đang streaming
                    layoutAiActions.visibility = View.GONE
                }
                message.isUser -> {
                    // User message
                    tvMessage.text = message.content
                    container.gravity = Gravity.END
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.green_primary))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                else -> {
                    // AI message hoàn thành
                    val formattedText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(message.content, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        @Suppress("DEPRECATION") Html.fromHtml(message.content)
                    }
                    tvMessage.text = formattedText
                    container.gravity = Gravity.START
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))

                    // Hiển thị actions cho AI message (trừ message đầu tiên)
                    if (position > 0) {
                        layoutAiActions.visibility = View.VISIBLE
                    }
                }
            }
        }

        private fun startCursorAnimation() {
            cursorAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 530
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { animator ->
                    val alpha = animator.animatedValue as Float
                    tvMessage.alpha = 0.7f + (alpha * 0.3f) // Dao động từ 0.7 -> 1.0
                }
                start()
            }
        }
    }
}