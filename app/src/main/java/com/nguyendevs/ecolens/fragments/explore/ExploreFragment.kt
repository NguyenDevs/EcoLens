package com.nguyendevs.ecolens.fragments.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nguyendevs.ecolens.adapters.ExploreAllAdapter
import com.nguyendevs.ecolens.database.ExploreRepository
import com.nguyendevs.ecolens.databinding.ScreenExploreLayoutBinding
import kotlinx.coroutines.launch

/**
 * Fragment hiển thị toàn bộ explore items từ Firebase dạng danh sách dọc.
 * Được mở khi người dùng nhấn "Xem tất cả" ở màn hình Home.
 */
class ExploreFragment : Fragment() {

    private var _binding: ScreenExploreLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var exploreAllAdapter: ExploreAllAdapter
    private val exploreRepository by lazy { ExploreRepository() }

    /** Callback khi click vào item (truyền từ ngoài vào). */
    var onItemClick: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ScreenExploreLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupBackPress()
        loadAllExploreItems()
    }

    /** Khởi tạo RecyclerView với adapter. */
    private fun setupRecyclerView() {
        exploreAllAdapter = ExploreAllAdapter { item ->
            if (item.image.isNotEmpty()) {
                onItemClick?.invoke(item.image)
            }
        }

        binding.rvExploreAll.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exploreAllAdapter
        }
    }

    /** Xử lý nút back hệ thống để đóng fragment. */
    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            }
        )
    }

    /** Tải toàn bộ explore items từ Firebase và hiển thị. */
    private fun loadAllExploreItems() {
        binding.loadingContainer.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val items = exploreRepository.getAllExploreItems()

            binding.loadingContainer.visibility = View.GONE

            if (items.isEmpty()) {
                binding.emptyStateContainer.visibility = View.VISIBLE
            } else {
                exploreAllAdapter.submitList(items)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
