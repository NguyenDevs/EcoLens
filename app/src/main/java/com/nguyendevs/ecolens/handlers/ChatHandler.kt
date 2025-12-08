package com.nguyendevs.ecolens.handlers

import android.content.Context
import android.view.View
import android.widget.EditText
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ChatAdapter
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatHandler(
    private val context: Context,
    private val view: View,
    private val viewModel: EcoLensViewModel,
    private val lifecycleOwner: LifecycleOwner
) {
    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: FloatingActionButton
    private val adapter = ChatAdapter()

    fun setup() {
        rvChat = view.findViewById(R.id.rvChat)
        etInput = view.findViewById(R.id.etChatInput)
        btnSend = view.findViewById(R.id.btnSend)

        rvChat.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true // Tin nhắn mới ở dưới cùng
        }
        rvChat.adapter = adapter

        // Gửi tin nhắn chào mừng mặc định nếu list trống
        if (viewModel.chatMessages.value.isEmpty()) {
            viewModel.sendChatMessage("Chào EcoLens, bạn có thể giúp gì cho tôi?")
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendChatMessage(text)
                etInput.text.clear()
            }
        }

        // Quan sát dữ liệu tin nhắn
        lifecycleOwner.lifecycleScope.launch {
            viewModel.chatMessages.collectLatest { messages ->
                adapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    rvChat.smoothScrollToPosition(messages.size - 1)
                }
            }
        }
    }
}