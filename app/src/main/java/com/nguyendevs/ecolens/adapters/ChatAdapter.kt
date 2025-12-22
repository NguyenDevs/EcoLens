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
        val oldSize = messages.size
        val newSize = newMessages.size

        messages.clear()
        messages.addAll(newMessages)

        if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize)
            if (oldSize > 0) {
                notifyItemChanged(oldSize - 1)
            }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
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
        val message = messages[position]
        holder.bind(message, position)
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
            tvMessage.alpha = 1f
        }

        fun bindStreamingText(message: ChatMessage) {
            if (message.isStreaming) {
                tvMessage.text = message.content + " ▌"
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
                    tvMessage.text = message.content
                    container.gravity = Gravity.END
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.green_primary))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))

                    cardView.setOnLongClickListener {
                        actionListener.onCopy(message.content)
                        true
                    }
                }
                else -> {
                    val formattedText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(message.content, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        @Suppress("DEPRECATION") Html.fromHtml(message.content)
                    }
                    tvMessage.text = formattedText

                    container.gravity = Gravity.START
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))

                    if (position > 0) {
                        layoutAiActions.visibility = View.VISIBLE

                        if (position == messages.size - 1) {
                            btnRenewAi.visibility = View.VISIBLE
                        } else {
                            btnRenewAi.visibility = View.GONE
                        }

                        btnCopyAi.visibility = View.VISIBLE
                        btnShareAi.visibility = View.VISIBLE

                        btnCopyAi.setOnClickListener {
                            val plainText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Html.fromHtml(message.content, Html.FROM_HTML_MODE_COMPACT).toString()
                            } else {
                                @Suppress("DEPRECATION") Html.fromHtml(message.content).toString()
                            }
                            actionListener.onCopy(plainText)
                        }
                        btnShareAi.setOnClickListener {
                            val plainText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Html.fromHtml(message.content, Html.FROM_HTML_MODE_COMPACT).toString()
                            } else {
                                @Suppress("DEPRECATION") Html.fromHtml(message.content).toString()
                            }
                            actionListener.onShare(plainText)
                        }
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