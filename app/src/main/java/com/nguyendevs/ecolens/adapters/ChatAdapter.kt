package com.nguyendevs.ecolens.adapters

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemChatMessageBinding
import com.nguyendevs.ecolens.model.chat.ChatMessage
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.html.HtmlPlugin

/**
 * Adapter hiển thị danh sách tin nhắn chat với hỗ trợ Markdown
 * Hỗ trợ hiệu ứng loading, streaming text và các action (copy, share, renew)
 */
class ChatAdapter(
    private val actionListener: OnChatActionListener
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    interface OnChatActionListener {
        fun onCopy(text: String)
        fun onShare(text: String)
        fun onRenew(position: Int, message: ChatMessage)
    }

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var markwon: Markwon

    // ==================== ADAPTER METHODS ====================

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        if (!::markwon.isInitialized) {
            markwon = Markwon.builder(parent.context)
                .usePlugin(HtmlPlugin.create())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder.headingTextSizeMultipliers(floatArrayOf(
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
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
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

    // ==================== VIEW HOLDER ====================

    inner class ChatViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val colorWhite = ContextCompat.getColor(itemView.context, R.color.white)
        private val colorGreenPrimary = ContextCompat.getColor(itemView.context, R.color.green_primary)

        private val handler = Handler(Looper.getMainLooper())
        private var loopCount = 0
        private var cursorAnimator: ValueAnimator? = null

        private fun getThemeColor(attr: Int): Int {
            val typedValue = TypedValue()
            itemView.context.theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }

        private val loadingAnimateRunnable = object : Runnable {
            override fun run() {
                loopCount++
                val spannable = SpannableString("...")
                val visibleDots = (loopCount % 3) + 1
                if (visibleDots < 3) {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.TRANSPARENT),
                        visibleDots,
                        3,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                binding.tvMessage.text = spannable
                handler.postDelayed(this, 400)
            }
        }

        fun stopAnimation() {
            handler.removeCallbacks(loadingAnimateRunnable)
            cursorAnimator?.cancel()
            cursorAnimator = null
            binding.tvMessage.alpha = 1f
        }

        fun bindStreamingText(message: ChatMessage) {
            if (message.isStreaming) {
                markwon.setMarkdown(binding.tvMessage, message.content + " ▌")
            }
        }

        fun bind(message: ChatMessage, position: Int) {
            stopAnimation()
            resetViews()
            val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)
            val colorOnSurface = getThemeColor(com.google.android.material.R.attr.colorOnSurface)

            when {
                message.isLoading -> bindLoadingState(colorSurface, colorOnSurface)
                message.isStreaming -> bindStreamingState(message, colorSurface, colorOnSurface)
                message.isUser -> bindUserMessage(message)
                else -> bindAiMessage(message, position, colorSurface, colorOnSurface)
            }
        }

        private fun resetViews() {
            binding.layoutAiActions.visibility = android.view.View.GONE
            binding.tvMessage.alpha = 1f
            binding.cardMessage.setOnClickListener(null)
            binding.cardMessage.setOnLongClickListener(null)
            binding.btnCopyAi.setOnClickListener(null)
            binding.btnShareAi.setOnClickListener(null)
            binding.btnRenewAi.setOnClickListener(null)
        }

        private fun bindLoadingState(bgColor: Int, textColor: Int) {
            binding.chatContainer.gravity = Gravity.START
            binding.cardMessage.setCardBackgroundColor(bgColor)
            binding.tvMessage.setTextColor(textColor)
            binding.tvMessage.text = "..."
            loopCount = 0
            loadingAnimateRunnable.run()
        }

        private fun bindStreamingState(message: ChatMessage, bgColor: Int, textColor: Int) {
            binding.chatContainer.gravity = Gravity.START
            binding.cardMessage.setCardBackgroundColor(bgColor)
            binding.tvMessage.setTextColor(textColor)
            bindStreamingText(message)
            startCursorAnimation()
            binding.layoutAiActions.visibility = android.view.View.GONE
        }

        private fun bindUserMessage(message: ChatMessage) {
            markwon.setMarkdown(binding.tvMessage, message.content)
            binding.chatContainer.gravity = Gravity.END
            binding.cardMessage.setCardBackgroundColor(colorGreenPrimary)
            binding.tvMessage.setTextColor(colorWhite)
            binding.cardMessage.setOnLongClickListener {
                actionListener.onCopy(message.content)
                true
            }
        }

        private fun bindAiMessage(message: ChatMessage, position: Int, bgColor: Int, textColor: Int) {
            markwon.setMarkdown(binding.tvMessage, message.content)
            binding.chatContainer.gravity = Gravity.START
            binding.cardMessage.setCardBackgroundColor(bgColor)
            binding.tvMessage.setTextColor(textColor)

            if (position > 0) {
                binding.layoutAiActions.visibility = android.view.View.VISIBLE
                binding.btnRenewAi.visibility = if (position == messages.size - 1)
                    android.view.View.VISIBLE else android.view.View.GONE
                binding.btnCopyAi.visibility = android.view.View.VISIBLE
                binding.btnShareAi.visibility = android.view.View.VISIBLE

                binding.btnCopyAi.setOnClickListener { actionListener.onCopy(message.content) }
                binding.btnShareAi.setOnClickListener { actionListener.onShare(message.content) }
                binding.btnRenewAi.setOnClickListener { actionListener.onRenew(position, message) }
            }
        }

        private fun startCursorAnimation() {
            if (cursorAnimator == null) {
                cursorAnimator = ValueAnimator.ofFloat(1f, 0.4f).apply {
                    duration = 600
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    addUpdateListener { animator ->
                        binding.tvMessage.alpha = animator.animatedValue as Float
                    }
                    start()
                }
            }
        }
    }
}