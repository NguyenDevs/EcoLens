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
import com.nguyendevs.ecolens.models.chat.ChatSession
import com.nguyendevs.ecolens.utils.FabAnimationHelper
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Fragment danh sách các phiên chat, hỗ trợ phân trang và tạo chat mới. */
class ChatHistoryFragment : Fragment() {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private var _binding: ScreenChatHistoryBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var adapter: ChatSessionAdapter
    private var fullChatList: List<ChatSession> = emptyList()
    private var currentPage = 0
    private var isLoadingMore = false
    private val pageSize = 20

    /** Inflate layout của fragment. */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = ScreenChatHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Khởi tạo RecyclerView, observer và FAB listener. */
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

    /** Cấu hình RecyclerView với phân trang khi scroll đến cuối. */
    private fun setupRecyclerView() {
        adapter =
                ChatSessionAdapter(mutableListOf()) { session ->
                    binding.rvChatHistory.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    openChatScreen(session.id)
                }

        binding.rvChatHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatHistoryFragment.adapter

            addOnScrollListener(
                    object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
                            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                            val totalItemCount = layoutManager.itemCount
                            val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                            if (!isLoadingMore && totalItemCount <= (lastVisibleItem + 5)) {
                                loadNextPage()
                            }
                        }
                    }
            )
        }
    }

    /** Thiết lập FAB để tạo chat session mới. */
    private fun setupFabListener() {
        binding.fabNewChat.setOnClickListener {
            binding.fabNewChat.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            FabAnimationHelper.animateClick(
                    binding.fabNewChat
            ) { openChatScreen(null) }
        }
    }

    /** Observe danh sách chat sessions, toggle empty state khi cần. */
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

    /** Mở màn hình chat theo sessionId, hoặc tạo mới nếu null. */
    private fun openChatScreen(sessionId: Long?) {
        val fragment = ChatFragment.newInstance(sessionId)

        parentFragmentManager
                .beginTransaction()
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

    /** Tải trang tiếp theo của danh sách chat (lazy loading). */
    private fun loadNextPage() {
        val start = (currentPage + 1) * pageSize
        if (start >= fullChatList.size) return

        isLoadingMore = true
        adapter.setLoading(true)

        Handler(Looper.getMainLooper())
                .postDelayed(
                        {
                            val end = (start + pageSize).coerceAtMost(fullChatList.size)
                            val newItems = fullChatList.subList(start, end)

                            adapter.setLoading(false)
                            adapter.addItems(newItems)

                            currentPage++
                            isLoadingMore = false
                        },
                        500
                )
    }
}
