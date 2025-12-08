package com.nguyendevs.ecolens.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.ChatMessage

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    // Cập nhật danh sách tin nhắn
    fun submitList(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    // Tạo ViewHolder mới
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    // Gắn dữ liệu vào ViewHolder
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    // Lấy số lượng tin nhắn
    override fun getItemCount(): Int = messages.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.chatContainer)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardMessage)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)

        // Gắn dữ liệu tin nhắn vào view
        fun bind(message: ChatMessage) {
            tvMessage.text = message.content

            if (message.isUser) {
                container.gravity = Gravity.END
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.green_primary))
                tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            } else {
                container.gravity = Gravity.START
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                tvMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            }
        }
    }
}