package com.nguyendevs.ecolens.adapters

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemChatMessageBinding
import com.nguyendevs.ecolens.models.chat.ChatMessage
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.html.HtmlPlugin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val actionListener: OnChatActionListener) :
        RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    interface OnChatActionListener {
        fun onCopy(text: String)
        fun onShare(text: String)
        fun onRenew(position: Int, message: ChatMessage)
    }

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var markwon: Markwon
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        if (!::markwon.isInitialized) {
            markwon =
                    Markwon.builder(parent.context)
                            .usePlugin(HtmlPlugin.create())
                            .usePlugin(
                                    object : AbstractMarkwonPlugin() {
                                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                                            builder.headingTextSizeMultipliers(
                                                    floatArrayOf(
                                                            2.0f,
                                                            1.5f,
                                                            1.17f,
                                                            1.0f,
                                                            0.83f,
                                                            0.67f
                                                    )
                                            )
                                        }
                                    }
                            )
                            .build()
        }
        val binding =
                ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(
            holder: ChatViewHolder,
            position: Int,
            payloads: MutableList<Any>
    ) {
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

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) :
            RecyclerView.ViewHolder(binding.root) {

        private val colorWhite = ContextCompat.getColor(itemView.context, R.color.white)
        private val colorPrimary = ContextCompat.getColor(itemView.context, R.color.primary)
        private val colorPrimaryLight =
                ContextCompat.getColor(itemView.context, R.color.primary_light)
        private val colorEmerald50 = ContextCompat.getColor(itemView.context, R.color.emerald_50)
        private val colorEmerald100 = ContextCompat.getColor(itemView.context, R.color.emerald_100)
        private val colorBorderLight =
                ContextCompat.getColor(itemView.context, R.color.border_light)
        private val colorPrimaryDark =
                ContextCompat.getColor(itemView.context, R.color.primary_dark)

        private val radiusMd = itemView.resources.getDimension(R.dimen.radius_md)
        private val radiusSmall = itemView.resources.getDimension(R.dimen.radius_xxs)

        private val handler = Handler(Looper.getMainLooper())
        private var loopCount = 0
        private var cursorAnimator: ValueAnimator? = null

        private fun getThemeColor(attr: Int): Int {
            val typedValue = TypedValue()
            itemView.context.theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }

        private val aiBubbleShape =
                ShapeAppearanceModel.builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, radiusSmall)
                        .setTopRightCorner(CornerFamily.ROUNDED, radiusMd)
                        .setBottomRightCorner(CornerFamily.ROUNDED, radiusMd)
                        .setBottomLeftCorner(CornerFamily.ROUNDED, radiusMd)
                        .build()

        private val userBubbleShape =
                ShapeAppearanceModel.builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, radiusMd)
                        .setTopRightCorner(CornerFamily.ROUNDED, radiusMd)
                        .setBottomRightCorner(CornerFamily.ROUNDED, radiusSmall)
                        .setBottomLeftCorner(CornerFamily.ROUNDED, radiusMd)
                        .build()

        private val loadingAnimateRunnable =
                object : Runnable {
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

            showTimestamp(message)

            when {
                message.isLoading -> bindLoadingState(colorSurface, colorOnSurface)
                message.isStreaming -> bindStreamingState(message, colorSurface, colorOnSurface)
                message.isUser -> bindUserMessage(message)
                else -> bindAiMessage(message, position, colorSurface, colorOnSurface)
            }
        }

        private fun resetViews() {
            binding.tvMessage.alpha = 1f
            binding.tvMessage.setTypeface(null, Typeface.NORMAL)
            binding.tvTimestamp.visibility = View.GONE
            binding.viewAccentBar.visibility = View.GONE
            binding.cardMessage.strokeWidth = 0
            binding.cardMessage.setOnClickListener(null)
            binding.cardMessage.setOnLongClickListener(null)
        }

        private fun showTimestamp(message: ChatMessage) {
            binding.tvTimestamp.visibility = View.VISIBLE
            binding.tvTimestamp.text = timeFormat.format(Date(message.timestamp))
        }

        private fun configureAiLayout() {
            binding.cardMiniAvatar.visibility = View.VISIBLE
            binding.spacerEnd.visibility = View.VISIBLE
            binding.spacerAvatarFooter.visibility = View.VISIBLE
            binding.chatContainer.gravity = Gravity.START
            binding.layoutBubbleRow.gravity = Gravity.START
            binding.layoutFooterRow.gravity = Gravity.START

            binding.cardMessage.shapeAppearanceModel = aiBubbleShape
            binding.viewAccentBar.visibility = View.VISIBLE
            binding.cardMessage.strokeWidth = 1
            binding.cardMessage.strokeColor = colorBorderLight
        }

        private fun configureUserLayout() {
            binding.cardMiniAvatar.visibility = View.GONE
            binding.spacerEnd.visibility = View.GONE
            binding.spacerAvatarFooter.visibility = View.GONE
            binding.chatContainer.gravity = Gravity.END
            binding.layoutBubbleRow.gravity = Gravity.END

            // Fix: User footer needs to push content to the end
            val params =
                    binding.tvTimestamp.layoutParams as android.widget.LinearLayout.LayoutParams
            params.gravity = Gravity.END
            binding.tvTimestamp.layoutParams = params
            binding.layoutFooterRow.gravity = Gravity.END

            binding.cardMessage.shapeAppearanceModel = userBubbleShape
            binding.viewAccentBar.visibility = View.GONE
            binding.cardMessage.strokeWidth = 0
        }

        private fun bindLoadingState(bgColor: Int, textColor: Int) {
            configureAiLayout()
            binding.cardMessage.setCardBackgroundColor(bgColor)
            binding.tvMessage.setTextColor(textColor)
            binding.tvMessage.text = "..."
            binding.tvTimestamp.visibility = View.GONE
            loopCount = 0
            loadingAnimateRunnable.run()
        }

        private fun bindStreamingState(message: ChatMessage, bgColor: Int, textColor: Int) {
            configureAiLayout()
            binding.cardMessage.setCardBackgroundColor(bgColor)
            binding.tvMessage.setTextColor(textColor)
            bindStreamingText(message)
            startCursorAnimation()
        }

        private fun bindUserMessage(message: ChatMessage) {
            configureUserLayout()
            markwon.setMarkdown(binding.tvMessage, message.content)
            binding.cardMessage.setCardBackgroundColor(colorPrimary)
            binding.tvMessage.setTextColor(colorWhite)
            binding.cardMessage.cardElevation =
                    itemView.resources.getDimension(R.dimen.elevation_sm)
            binding.cardMessage.setOnLongClickListener {
                actionListener.onCopy(message.content)
                true
            }
        }

        private fun bindAiMessage(
                message: ChatMessage,
                position: Int,
                bgColor: Int,
                textColor: Int
        ) {
            configureAiLayout()

            if (position == 0) {
                binding.cardMessage.setCardBackgroundColor(colorEmerald50)
                binding.cardMessage.strokeColor = colorEmerald100
                binding.tvMessage.setTextColor(colorPrimaryDark)
                binding.tvMessage.setTypeface(null, Typeface.ITALIC)
                binding.viewAccentBar.setBackgroundColor(colorPrimaryLight)
            } else {
                binding.cardMessage.setCardBackgroundColor(bgColor)
                binding.tvMessage.setTextColor(textColor)
                binding.viewAccentBar.setBackgroundColor(colorPrimaryLight)
            }

            markwon.setMarkdown(binding.tvMessage, message.content)

            if (position > 0) {
                binding.cardMessage.setOnLongClickListener { view ->
                    val popup = PopupMenu(view.context, view)
                    popup.menu.add(
                            0,
                            1,
                            0,
                            itemView.context.getString(R.string.copy_scientific_name)
                    )
                    popup.menu.add(0, 2, 0, itemView.context.getString(R.string.share_info))

                    if (position == messages.size - 1) {
                        popup.menu.add(
                                0,
                                3,
                                0,
                                itemView.context.getString(R.string.btn_retry_identification)
                        )
                    }

                    popup.setOnMenuItemClickListener { item: MenuItem ->
                        when (item.itemId) {
                            1 -> {
                                actionListener.onCopy(message.content)
                                true
                            }
                            2 -> {
                                actionListener.onShare(message.content)
                                true
                            }
                            3 -> {
                                actionListener.onRenew(position, message)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                    true
                }
            }
        }

        private fun startCursorAnimation() {
            if (cursorAnimator == null) {
                cursorAnimator =
                        ValueAnimator.ofFloat(1f, 0.4f).apply {
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
