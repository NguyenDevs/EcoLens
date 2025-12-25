package com.nguyendevs.ecolens.adapters

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Handler
import android.os.Looper
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
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.html.HtmlPlugin

class ChatAdapter(private val actionListener: OnChatActionListener) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    interface OnChatActionListener {
        fun onCopy(text: String)
        fun onShare(text: String)
        fun onRenew(position: Int, message: ChatMessage)
    }

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var markwon: Markwon

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        if (!::markwon.isInitialized) {
            markwon = Markwon.builder(parent.context)
                .usePlugin(HtmlPlugin.create())
                // [THÊM ĐOẠN PLUGIN NÀY ĐỂ CẤU HÌNH H1-H6]
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            .headingTextSizeMultipliers(floatArrayOf(
                                2.0f,   // h1 (#)
                                1.5f,   // h2 (##)
                                1.17f,  // h3 (###)
                                1.0f,   // h4 (####)
                                0.83f,  // h5 (#####)
                                0.67f   // h6 (######)
                            ))
                    }
                })
                .build()
        }
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message_modern, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads[0] == "STREAMING") {
            holder.bindStreamingText(messages[position])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position], position)
    }

    override fun onViewRecycled(holder: ChatViewHolder) {
        super.onViewRecycled(holder)
        holder.stopAnimation()
    }

    override fun getItemCount(): Int = messages.size

    fun submitList(newMessages: List<ChatMessage>) {
        val oldSize = messages.size
        val newSize = newMessages.size

        messages.clear()
        messages.addAll(newMessages)

        if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize)
            if (oldSize > 0) notifyItemChanged(oldSize - 1)
        } else if (newSize == oldSize && newSize > 0) {
            val lastMsg = messages[newSize - 1]
            if (lastMsg.isStreaming) {
                notifyItemChanged(newSize - 1, "STREAMING")
            } else {
                notifyItemChanged(newSize - 1)
            }
        } else {
            notifyDataSetChanged()
        }
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.chatContainer)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardMessage)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val layoutAiActions: LinearLayout = itemView.findViewById(R.id.layoutAiActions)
        private val btnCopyAi: ImageView = itemView.findViewById(R.id.btnCopyAi)
        private val btnShareAi: ImageView = itemView.findViewById(R.id.btnShareAi)
        private val btnRenewAi: ImageView = itemView.findViewById(R.id.btnRenewAi)

        private val handler = Handler(Looper.getMainLooper())
        private var loopCount = 0
        private var cursorAnimator: ValueAnimator? = null

        private val loadingAnimateRunnable = object : Runnable {
            override fun run() {
                loopCount++
                val spannable = SpannableString("...")
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
            tvMessage.alpha = 1f
        }

        fun bindStreamingText(message: ChatMessage) {
            if (message.isStreaming) {
                markwon.setMarkdown(tvMessage, message.content + " ▌")
            }
        }

        fun bind(message: ChatMessage, position: Int) {
            stopAnimation()
            layoutAiActions.visibility = View.GONE
            tvMessage.alpha = 1f

            cardView.setOnClickListener(null)
            cardView.setOnLongClickListener(null)
            btnCopyAi.setOnClickListener(null)
            btnShareAi.setOnClickListener(null)
            btnRenewAi.setOnClickListener(null)

            when {
                message.isLoading -> {
                    container.gravity = Gravity.START
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                    tvMessage.text = "..."
                    loopCount = 0
                    loadingAnimateRunnable.run()
                }
                message.isStreaming -> {
                    container.gravity = Gravity.START
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                    bindStreamingText(message)
                    startCursorAnimation()
                    layoutAiActions.visibility = View.GONE
                }
                message.isUser -> {
                    markwon.setMarkdown(tvMessage, message.content)
                    container.gravity = Gravity.END
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.green_primary))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                    cardView.setOnLongClickListener {
                        actionListener.onCopy(message.content)
                        true
                    }
                }
                else -> {
                    markwon.setMarkdown(tvMessage, message.content)
                    container.gravity = Gravity.START
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))

                    if (position > 0) {
                        layoutAiActions.visibility = View.VISIBLE
                        btnRenewAi.visibility = if (position == messages.size - 1) View.VISIBLE else View.GONE
                        btnCopyAi.visibility = View.VISIBLE
                        btnShareAi.visibility = View.VISIBLE

                        btnCopyAi.setOnClickListener { actionListener.onCopy(message.content) }
                        btnShareAi.setOnClickListener { actionListener.onShare(message.content) }
                        btnRenewAi.setOnClickListener { actionListener.onRenew(position, message) }
                    }
                }
            }
        }

        private fun startCursorAnimation() {
            if (cursorAnimator == null) {
                cursorAnimator = ValueAnimator.ofFloat(1f, 0.4f).apply {
                    duration = 600
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    addUpdateListener { animator ->
                        tvMessage.alpha = animator.animatedValue as Float
                    }
                    start()
                }
            }
        }
    }
}