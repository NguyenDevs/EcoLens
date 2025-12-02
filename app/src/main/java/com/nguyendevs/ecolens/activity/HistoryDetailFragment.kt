package com.nguyendevs.ecolens.activity

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        val info = entry.speciesInfo

        // Ánh xạ View
        val imageView = view.findViewById<ImageView>(R.id.detailImageView)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnFavorite = view.findViewById<FloatingActionButton>(R.id.btnFavorite)

        val tvCommonName = view.findViewById<TextView>(R.id.tvCommonName)
        val tvScientificName = view.findViewById<TextView>(R.id.tvScientificName)
        val tvTaxonomy = view.findViewById<TextView>(R.id.tvTaxonomy)

        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        val lblDescription = view.findViewById<TextView>(R.id.lblDescription)

        val tvCharacteristics = view.findViewById<TextView>(R.id.tvCharacteristics)
        val lblCharacteristics = view.findViewById<TextView>(R.id.lblCharacteristics)

        val tvDistribution = view.findViewById<TextView>(R.id.tvDistribution)
        val lblDistribution = view.findViewById<TextView>(R.id.lblDistribution)

        val tvHabitat = view.findViewById<TextView>(R.id.tvHabitat)
        val lblHabitat = view.findViewById<TextView>(R.id.lblHabitat)

        val tvConservation = view.findViewById<TextView>(R.id.tvConservationStatus)
        val lblConservation = view.findViewById<TextView>(R.id.lblConservation)


        // Load ảnh từ đường dẫn file (imagePath)
        Glide.with(this)
            .load(entry.imagePath)
            .centerCrop()
            .into(imageView)

        // Hiển thị tên
        tvCommonName.text = info.commonName
        tvScientificName.text = info.scientificName

        // Xây dựng chuỗi phân loại
        val taxonomyBuilder = StringBuilder()
        if (info.kingdom.isNotEmpty()) taxonomyBuilder.append("• Giới: ${info.kingdom}\n")
        if (info.phylum.isNotEmpty()) taxonomyBuilder.append("• Ngành: ${info.phylum}\n")
        if (info.className.isNotEmpty()) taxonomyBuilder.append("• Lớp: ${info.className}\n")
        if (info.order.isNotEmpty()) taxonomyBuilder.append("• Bộ: ${info.order}\n")
        if (info.family.isNotEmpty()) taxonomyBuilder.append("• Họ: ${info.family}\n")
        if (info.genus.isNotEmpty()) taxonomyBuilder.append("• Chi: ${info.genus}\n")
        if (info.species.isNotEmpty()) taxonomyBuilder.append("• Loài: ${info.species}")
        tvTaxonomy.text = taxonomyBuilder.toString().trim()

        // Hiển thị các section mô tả (có hỗ trợ HTML và ẩn nếu không có dữ liệu)
        displaySection(tvDescription, lblDescription, info.description)
        displaySection(tvCharacteristics, lblCharacteristics, info.characteristics)
        displaySection(tvDistribution, lblDistribution, info.distribution)
        displaySection(tvHabitat, lblHabitat, info.habitat)
        displaySection(tvConservation, lblConservation, info.conservationStatus)

        // Xử lý nút Back (Đóng Fragment)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Xử lý nút Favorite
        updateFavoriteIcon(btnFavorite, entry.isFavorite) // Set trạng thái ban đầu

        btnFavorite.setOnClickListener {
            // 1. Gọi ViewModel để update vào Database (Lưu vào/Xóa khỏi My Garden)
            viewModel.toggleFavorite(entry)

            // 2. Cập nhật trạng thái UI tạm thời (đảo ngược trạng thái hiện tại)
            val newStatus = !entry.isFavorite

            // Cập nhật object hiện tại để đồng bộ
            historyEntry = entry.copy(isFavorite = newStatus)

            updateFavoriteIcon(btnFavorite, newStatus)
        }
    }

    private fun displaySection(textView: TextView, labelView: TextView, content: String) {
        if (content.isNotEmpty()) {
            labelView.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            textView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(content)
            }
        } else {
            labelView.visibility = View.GONE
            textView.visibility = View.GONE
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