package com.nguyendevs.ecolens.fragments.chat

import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.text.Html
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.ChatAdapter
import com.nguyendevs.ecolens.databinding.FragmentChatBinding
import com.nguyendevs.ecolens.models.chat.ChatMessage
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Fragment giao diện chat với AI, hỗ trợ streaming, copy, share và tạo lại response. */
class ChatFragment : Fragment(), ChatAdapter.OnChatActionListener {

    private val viewModel: EcoLensViewModel by activityViewModels()
    private var _binding: FragmentChatBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private var currentSessionId: Long? = null

    companion object {
        private const val ARG_SESSION_ID = "session_id"
        private val REGEX_BOLD = Regex("\\*\\*(.*?)\\*\\*")
        private val REGEX_HEADER = Regex("##(.*?)##")
        private val REGEX_STRIKE = Regex("~~(.*?)~~")

        /** Tạo instance mới của ChatFragment, optionally với sessionId đã tồn tại. */
        fun newInstance(sessionId: Long? = null): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply { sessionId?.let { putLong(ARG_SESSION_ID, it) } }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentSessionId = arguments?.getLong(ARG_SESSION_ID, -1L)?.takeIf { it != -1L }
    }

    /** Inflate layout của fragment. */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Khởi tạo adapter, RecyclerView, listener và observer. */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ChatAdapter(this)
        setupRecyclerView()
        setupListeners()
        setupKeyboardInsets()
        observeViewModel()
        initializeSession()

        binding.etChatInput.post {
            if (!binding.etChatInput.isFocused) {
                binding.etChatInput.requestFocus()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Load session cũ nếu có sessionId, hoặc tạo chat session mới. */
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

    /** Cấu hình RecyclerView cho danh sách tin nhắn. */
    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.rvChat.layoutManager = layoutManager
        binding.rvChat.adapter = adapter
        binding.rvChat.itemAnimator = null
    }

    /** Thiết lập listener cho nút gửi, back và menu. */
    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etChatInput.text.toString().trim()
            if (text.isNotEmpty()) {
                performHapticFeedback()
                viewModel.sendChatMessage(text, getString(R.string.new_chat))
                binding.etChatInput.text.clear()
            }
        }

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnMenu.setOnClickListener { showMenuPopup(it) }
        binding.etChatInput.setOnClickListener {
            if (!binding.etChatInput.isFocused) {
                binding.etChatInput.requestFocus()
            }
        }
    }

    /** Xử lý WindowInsets để thêm margin 4dp khi bàn phím hiện. */
    private fun setupKeyboardInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val immInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            binding.rvChat.setPadding(
                binding.rvChat.paddingLeft,
                binding.rvChat.paddingTop,
                binding.rvChat.paddingRight,
                if (isKeyboardVisible) 0 else binding.rvChat.paddingBottom
            )
            try {
                val inputContainer = binding.etChatInput.parent.parent.parent as View
                inputContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    this.bottomMargin = bottomMargin
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            insets
        }
    }

    /** Sao chép nội dung tin nhắn vào clipboard. */
    override fun onCopy(text: String) {
        performHapticFeedback()
        val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val cleanText = stripHtml(text)
        val clip = ClipData.newPlainText("EcoLens", cleanText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Đã sao chép", Toast.LENGTH_SHORT).show()
    }

    /** Chia sẻ nội dung tin nhắn qua Intent. */
    override fun onShare(text: String) {
        val cleanText = stripHtml(text)
        val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, cleanText)
                }
        startActivity(Intent.createChooser(intent, "Chia sẻ tin nhắn"))
    }

    /** Yêu cầu AI tạo lại response cho tin nhắn được chọn. */
    override fun onRenew(position: Int, message: ChatMessage) {
        performHapticFeedback()
        viewModel.renewAiResponse(message)
    }

    /** Hiển thị popup menu cho chat. */
    private fun showMenuPopup(anchor: View) {
        val widthPx = (280 * resources.displayMetrics.density).toInt()
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_chat_menu, null)

        val popup = PopupWindow(popupView, widthPx, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            exitTransition = android.transition.Fade(android.transition.Fade.OUT).apply {
                duration = 250
                interpolator = AccelerateDecelerateInterpolator()
            }
        }

        val btnDeleteChat = popupView.findViewById<ViewGroup>(R.id.itemDeleteChat)
        btnDeleteChat?.setOnClickListener {
            popup.dismiss()
            showDeleteConfirmDialog()
        }

        // Animation preparation
        btnDeleteChat?.let {
            it.alpha = 0f
            it.translationY = -12f
        }

        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val xOffset = anchorLoc[0] + anchor.width - widthPx
        val yOffset = anchorLoc[1] + anchor.height + (6 * resources.displayMetrics.density).toInt()

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, xOffset, yOffset)

        // Animation mở popup
        popupView.alpha = 0f
        popupView.scaleX = 0.90f
        popupView.scaleY = 0.90f
        popupView.pivotX = widthPx.toFloat()
        popupView.pivotY = 0f
        popupView.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(320)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animation cho item
        btnDeleteChat?.animate()
            ?.alpha(1f)?.translationY(0f)
            ?.setStartDelay(80L)
            ?.setDuration(300)
            ?.setInterpolator(AccelerateDecelerateInterpolator())
            ?.start()
    }

    /** Hiển thị dialog xác nhận xóa chat session. */
    private fun showDeleteConfirmDialog() {
        com.nguyendevs.ecolens.utils.CustomDialogUtils.showConfirmationDialog(
                context = requireContext(),
                title = getString(R.string.dialog_delete_chat_title),
                message = getString(R.string.dialog_delete_chat_message),
                confirmText = getString(R.string.action_delete),
                onConfirm = {
                    val sessionId = viewModel.currentChatSessionId ?: currentSessionId
                    sessionId?.let { id ->
                        viewModel.deleteChatSession(id)
                        parentFragmentManager.popBackStack()
                    }
                }
        )
    }

    /** Observe danh sách tin nhắn và trạng thái streaming từ ViewModel. */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatMessages.collectLatest { messages -> handleMessagesUpdate(messages) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isStreamingActive.collectLatest { isStreaming ->
                updateUIForStreamingState(isStreaming)
            }
        }
    }

    /** Cập nhật adapter và tự động scroll xuống khi có tin nhắn mới. */
    private fun handleMessagesUpdate(messages: List<ChatMessage>) {
        val isNewMessageAdded = messages.size > adapter.itemCount
        val layoutManager = binding.rvChat.layoutManager as LinearLayoutManager
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        val isAtBottom = lastVisibleItemPosition >= adapter.itemCount - 2

        adapter.submitList(messages) {
            if (messages.isNotEmpty()) {
                if (isNewMessageAdded || isAtBottom || messages.lastOrNull()?.isStreaming == true) {
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    /** Vô hiệu hóa input và các nút khi đang streaming response. */
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

        binding.tvStatus.text =
                if (isStreaming) {
                    getString(R.string.chat_responding)
                } else {
                    getString(R.string.chat_active)
                }
    }

    /** Thực hiện haptic feedback. */
    private fun performHapticFeedback() {
        binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    /** Xóa HTML tags và markdown formatting khỏi text. */
    private fun stripHtml(html: String): String {
        var text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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
