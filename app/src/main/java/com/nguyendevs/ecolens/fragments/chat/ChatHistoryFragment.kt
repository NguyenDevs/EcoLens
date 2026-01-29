package com.nguyendevs.ecolens.fragments.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ChatSessionAdapter
import com.nguyendevs.ecolens.databinding.ScreenChatHistoryBinding
import com.nguyendevs.ecolens.model.chat.ChatSession
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment hiển thị danh sách các phiên chat
 * Hỗ trợ tạo chat mới và mở lại chat cũ
 * Tự động hiển thị empty state khi không có chat
 */
class ChatHistoryFragment : Fragment() {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private var _binding: ScreenChatHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatSessionAdapter
    private var fullChatList: List<ChatSession> = emptyList()
    private var currentPage = 0
    private var isLoadingMore = false
    private val pageSize = 20

    // ==================== LIFECYCLE ====================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenChatHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeChatSessions()
        setupFabListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ==================== UI SETUP ====================

    /**
     * Cấu hình RecyclerView hiển thị danh sách chat sessions
     */
    private fun setupRecyclerView() {
        adapter = ChatSessionAdapter(mutableListOf()) { session ->
            binding.rvChatHistory.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            openChatScreen(session.id)
        }

        binding.rvChatHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatHistoryFragment.adapter
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (!isLoadingMore && totalItemCount <= (lastVisibleItem + 5)) {
                        loadNextPage()
                    }
                }
            })
        }
    }

    /**
     * Cấu hình FAB để tạo chat mới
     */
    private fun setupFabListener() {
        binding.fabNewChat.setOnClickListener {
            binding.fabNewChat.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            openChatScreen(null)
        }
    }

    // ==================== VIEWMODEL OBSERVERS ====================

    /**
     * Observe danh sách chat sessions từ ViewModel
     * Tự động toggle giữa RecyclerView và empty state
     */
    private fun observeChatSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allChatSessions.collectLatest { list ->
                if (list.isEmpty()) {
                    binding.rvChatHistory.visibility = View.GONE
                    binding.emptyStateContainer.visibility = View.VISIBLE
                } else {
                    binding.rvChatHistory.visibility = View.VISIBLE
                    binding.emptyStateContainer.visibility = View.GONE
                }
                
                fullChatList = list
                currentPage = 0
                val firstPage = fullChatList.take(pageSize)
                adapter.updateList(firstPage)
            }
        }
    }

    // ==================== NAVIGATION ====================

    /**
     * Mở màn hình chat
     * @param sessionId ID của session cũ, hoặc null để tạo session mới
     */
    private fun openChatScreen(sessionId: Long?) {
        val fragment = ChatFragment.newInstance(sessionId)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("chat_detail")
            .commit()
    }

    /**
     * Tải trang tiếp theo của danh sách chat
     */
    private fun loadNextPage() {
        val start = (currentPage + 1) * pageSize
        if (start >= fullChatList.size) return

        isLoadingMore = true
        adapter.setLoading(true)

        Handler(Looper.getMainLooper()).postDelayed({
            val end = (start + pageSize).coerceAtMost(fullChatList.size)
            val newItems = fullChatList.subList(start, end)

            adapter.setLoading(false)
            adapter.addItems(newItems)

            currentPage++
            isLoadingMore = false
        }, 500)
    }
}