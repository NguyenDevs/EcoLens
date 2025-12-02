package com.nguyendevs.ecolens.activity

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.view.EcoLensViewModel

class HistoryDetailFragment : Fragment() {
    private lateinit var entry: HistoryEntry
    private val viewModel: EcoLensViewModel by activityViewModels()

    companion object {
        fun newInstance(entry: HistoryEntry): HistoryDetailFragment {
            val fragment = HistoryDetailFragment()
            // Truyền object qua Bundle (cần HistoryEntry implement Serializable hoặc Parcelable)
            // Hoặc đơn giản truyền ID và query lại
            return fragment
        }
    }

    // Set data cho fragment (gọi từ Activity/Adapter)
    fun setData(historyEntry: HistoryEntry) {
        this.entry = historyEntry
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_history_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind View
        val imageView = view.findViewById<ImageView>(R.id.detailImageView)
        val btnFavorite = view.findViewById<FloatingActionButton>(R.id.btnFavorite)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)

        // Load ảnh từ đường dẫn file
        Glide.with(this).load(entry.imagePath).into(imageView)

        // Set thông tin text...
        view.findViewById<TextView>(R.id.tvCommonName).text = entry.speciesInfo.commonName
        view.findViewById<TextView>(R.id.tvDescription).text = entry.speciesInfo.description

        // Xử lý nút Favorite
        updateFavoriteIcon(btnFavorite, entry.isFavorite)
        btnFavorite.setOnClickListener {
            viewModel.toggleFavorite(entry)
            entry = entry.copy(isFavorite = !entry.isFavorite) // Update local state
            updateFavoriteIcon(btnFavorite, entry.isFavorite)
        }

        // Xử lý nút Back
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun updateFavoriteIcon(fab: FloatingActionButton, isFav: Boolean) {
        if (isFav) {
            fab.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite))
            fab.setColorFilter(Color.RED)
        } else {
            fab.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite_border)) // Cần icon viền
            fab.setColorFilter(Color.GRAY)
        }
    }
}