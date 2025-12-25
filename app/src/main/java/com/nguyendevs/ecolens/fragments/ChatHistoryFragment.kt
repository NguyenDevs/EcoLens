package com.nguyendevs.ecolens.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ChatSessionAdapter
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ChatHistoryFragment : Fragment(R.layout.screen_chat_history_modern) {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private lateinit var adapter: ChatSessionAdapter

    private lateinit var rvChatHistory: RecyclerView
    private lateinit var emptyStateContainer: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvChatHistory = view.findViewById(R.id.rvChatHistory)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.fabNewChat)

        setupRecyclerView(rvChatHistory)
        observeChatSessions()
        setupFabListener(fab)
    }

    private fun setupRecyclerView(rv: RecyclerView) {
        adapter = ChatSessionAdapter(emptyList()) { session ->
            openChatScreen(session.id)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
    }

    // Quan sát danh sách chat sessions từ ViewModel
    private fun observeChatSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allChatSessions.collectLatest { list ->
                if (list.isEmpty()) {
                    rvChatHistory.visibility = View.GONE
                    emptyStateContainer.visibility = View.VISIBLE
                } else {
                    rvChatHistory.visibility = View.VISIBLE
                    emptyStateContainer.visibility = View.GONE
                }
                adapter.updateList(list)
            }
        }
    }

    // Thiết lập listener cho nút tạo chat mới
    private fun setupFabListener(fab: ExtendedFloatingActionButton) {
        fab.setOnClickListener {
            performHapticFeedback()
            openChatScreen(null)
        }
    }

    // Mở màn hình chat
    private fun openChatScreen(sessionId: Long?) {
        val fragment = ChatFragment.newInstance(sessionId)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("chat_detail")
            .commit()
    }

    // Hàm tạo hiệu ứng rung
    private fun performHapticFeedback() {
        try {
            val context = requireContext()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}