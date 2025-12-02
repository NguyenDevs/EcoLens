package com.nguyendevs.ecolens.activity

import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.view.EcoLensViewModel

class HistoryDetailFragment : Fragment() {

    private var historyEntry: HistoryEntry? = null
    // Sử dụng activityViewModels để chia sẻ ViewModel với MainActivity
    private val viewModel: EcoLensViewModel by activityViewModels()

    // Hàm này dùng để truyền dữ liệu từ Activity vào Fragment
    fun setData(entry: HistoryEntry) {
        this.historyEntry = entry
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout cho fragment này
        return inflater.inflate(R.layout.fragment_history_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val entry = historyEntry ?: return // Nếu không có dữ liệu thì thoát

        // Ánh xạ View
        val imageView = view.findViewById<ImageView>(R.id.detailImageView)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnFavorite = view.findViewById<FloatingActionButton>(R.id.btnFavorite)

        val tvCommonName = view.findViewById<TextView>(R.id.tvCommonName)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)

        // Hiển thị thông tin cơ bản
        // Load ảnh từ đường dẫn file (imagePath)
        Glide.with(this)
            .load(entry.imagePath)
            .centerCrop()
            .into(imageView)

        tvCommonName.text = entry.speciesInfo.commonName

        // Hiển thị mô tả (có thể chứa HTML)
        val descriptionText = entry.speciesInfo.description
        tvDescription.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(descriptionText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(descriptionText)
        }

        // Xử lý nút Back (Đóng Fragment)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Xử lý nút Favorite
        updateFavoriteIcon(btnFavorite, entry.isFavorite) // Set trạng thái ban đầu

        btnFavorite.setOnClickListener {
            // 1. Gọi ViewModel để update vào Database
            viewModel.toggleFavorite(entry)

            // 2. Cập nhật trạng thái UI tạm thời (đảo ngược trạng thái hiện tại)
            val newStatus = !entry.isFavorite

            // Cập nhật object hiện tại để đồng bộ
            historyEntry = entry.copy(isFavorite = newStatus)

            updateFavoriteIcon(btnFavorite, newStatus)
        }
    }

    private fun updateFavoriteIcon(fab: FloatingActionButton, isFav: Boolean) {
        if (isFav) {
            fab.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite))
            fab.setColorFilter(Color.RED) // Đổi màu icon thành đỏ
        } else {
            fab.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite))
            fab.setColorFilter(Color.GRAY) // Đổi màu icon thành xám
        }
    }
}