package com.nguyendevs.ecolens.fragments

import android.content.*
import android.os.*
import android.text.Html
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ChatAdapter
import com.nguyendevs.ecolens.databinding.FragmentChatBinding
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment hiển thị giao diện chat với AI
 * Hỗ trợ streaming response, copy, share và renew messages
 * Tự động scroll và disable UI khi đang streaming
 */
class ChatFragment : Fragment(), ChatAdapter.OnChatActionListener {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private var currentSessionId: Long? = null

    companion object {
        private const val ARG_SESSION_ID = "session_id"
        private val REGEX_BOLD = Regex("\\*\\*(.*?)\\*\\*")
        private val REGEX_HEADER = Regex("##(.*?)##")
        private val REGEX_STRIKE = Regex("~~(.*?)~~")

        fun newInstance(sessionId: Long? = null): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    sessionId?.let { putLong(ARG_SESSION_ID, it) }
                }
            }
        }
    }

    // ==================== LIFECYCLE ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentSessionId = arguments?.getLong(ARG_SESSION_ID, -1L)?.takeIf { it != -1L }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ChatAdapter(this)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        initializeSession()

        binding.etChatInput.post { binding.etChatInput.requestFocus() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ==================== INITIALIZATION ====================

    /**
     * Khởi tạo hoặc load chat session
     * Load session cũ nếu có sessionId, tạo mới nếu không
     */
    private fun initializeSession() {
        if (currentSessionId != null) {
            viewModel.loadChatSession(currentSessionId!!)
        } else {
            viewModel.initNewChatSession(
                getString(R.string.chat_welcome),
                getString(R.string.new_chat)
            )
        }
    }

    // ==================== UI SETUP ====================

    /**
     * Cấu hình RecyclerView cho chat messages
     */
    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChat.layoutManager = layoutManager
        binding.rvChat.adapter = adapter
        binding.rvChat.itemAnimator = null
    }

    /**
     * Cấu hình click listeners cho UI elements
     */
    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etChatInput.text.toString().trim()
            if (text.isNotEmpty()) {
                performHapticFeedback()
                viewModel.sendChatMessage(text, getString(R.string.new_chat))
                binding.etChatInput.text.clear()
            }
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnMenu.setOnClickListener {
            showMenuPopup(it)
        }
    }

    // ==================== CHAT ADAPTER CALLBACKS ====================

    /**
     * Sao chép nội dung tin nhắn vào clipboard
     */
    override fun onCopy(text: String) {
        performHapticFeedback()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val cleanText = stripHtml(text)
        val clip = ClipData.newPlainText("EcoLens", cleanText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Đã sao chép", Toast.LENGTH_SHORT).show()
    }

    /**
     * Chia sẻ nội dung tin nhắn
     */
    override fun onShare(text: String) {
        val cleanText = stripHtml(text)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, cleanText)
        }
        startActivity(Intent.createChooser(intent, "Chia sẻ tin nhắn"))
    }

    /**
     * Tạo lại response từ AI cho tin nhắn này
     */
    override fun onRenew(position: Int, message: ChatMessage) {
        performHapticFeedback()
        viewModel.renewAiResponse(message)
    }

    // ==================== MENU & DIALOGS ====================

    /**
     * Hiển thị popup menu với các action cho chat
     */
    private fun showMenuPopup(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_chat, popup.menu)

        runCatching {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                .invoke(mPopup, true)
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_chat -> {
                    showDeleteConfirmDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * Hiển thị dialog xác nhận xóa chat
     */
    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_chat_title)
            .setMessage(R.string.dialog_delete_chat_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                currentSessionId?.let { sessionId ->
                    viewModel.deleteChatSession(sessionId)
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    // ==================== VIEWMODEL OBSERVERS ====================

    /**
     * Observe các state từ ViewModel
     * Cập nhật UI khi có messages mới hoặc streaming state thay đổi
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatMessages.collectLatest { messages ->
                handleMessagesUpdate(messages)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isStreamingActive.collectLatest { isStreaming ->
                updateUIForStreamingState(isStreaming)
            }
        }
    }

    /**
     * Xử lý cập nhật danh sách messages
     * Tự động scroll xuống khi có message mới hoặc đang ở bottom
     */
    private fun handleMessagesUpdate(messages: List<ChatMessage>) {
        val isNewMessageAdded = messages.size > adapter.itemCount
        val layoutManager = binding.rvChat.layoutManager as LinearLayoutManager
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        val isAtBottom = lastVisibleItemPosition == adapter.itemCount - 1

        adapter.submitList(messages)

        if (messages.isNotEmpty()) {
            if (isNewMessageAdded) {
                binding.rvChat.scrollToPosition(messages.size - 1)
            } else if (isAtBottom) {
                val lastPos = messages.size - 1
                if (layoutManager.findLastCompletelyVisibleItemPosition() < lastPos) {
                    binding.rvChat.scrollToPosition(lastPos)
                }
            }
        }
    }

    /**
     * Cập nhật UI dựa trên trạng thái streaming
     * Disable input và buttons khi đang streaming
     */
    private fun updateUIForStreamingState(isStreaming: Boolean) {
        val alpha = if (isStreaming) 0.5f else 1f
        val enabled = !isStreaming

        binding.btnSend.isEnabled = enabled
        binding.btnSend.alpha = alpha
        binding.etChatInput.isEnabled = enabled
        binding.etChatInput.alpha = if (isStreaming) 0.7f else 1f
        binding.btnBack.isEnabled = enabled
        binding.btnBack.alpha = alpha
        binding.btnMenu.isEnabled = enabled
        binding.btnMenu.alpha = alpha
    }

    // ==================== HELPER METHODS ====================

    /**
     * Thực hiện haptic feedback
     */
    private fun performHapticFeedback() {
        binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    /**
     * Loại bỏ HTML tags và markdown formatting khỏi text
     */
    private fun stripHtml(html: String): String {
        var text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION") Html.fromHtml(html).toString()
        }
        text = text.replace(REGEX_BOLD, "$1")
        text = text.replace(REGEX_HEADER, "$1")
        text = text.replace(REGEX_STRIKE, "$1")
        return text.trim()
    }
}