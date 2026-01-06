package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemLanguageModernBinding
import com.nguyendevs.ecolens.model.Language

/**
 * Adapter hiển thị danh sách ngôn ngữ với visual feedback cho item được chọn
 * Item được chọn sẽ có stroke màu xanh và text màu đậm hơn
 */
class LanguageAdapter(
    private var languages: List<Language>,
    private val onLanguageClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolderModern>() {

    // ==================== ADAPTER METHODS ====================

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolderModern {
        val binding = ItemLanguageModernBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolderModern(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolderModern, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount() = languages.size

    /**
     * Cập nhật danh sách ngôn ngữ
     */
    fun updateList(newLanguages: List<Language>) {
        languages = newLanguages
        notifyDataSetChanged()
    }

    // ==================== VIEW HOLDER ====================

    inner class LanguageViewHolderModern(
        private val binding: ItemLanguageModernBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val colorGreenPrimary = ContextCompat.getColor(itemView.context, R.color.green_primary)
        private val colorGreenDark = ContextCompat.getColor(itemView.context, R.color.green_dark)
        private val colorTextPrimary = ContextCompat.getColor(itemView.context, R.color.text_primary)
        private val strokeWidthPx = (2 * itemView.resources.displayMetrics.density).toInt()

        init {
            binding.cardLanguage.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLanguageClick(languages[position])
                }
            }
        }

        /**
         * Bind dữ liệu ngôn ngữ vào view
         * Tự động thay đổi màu stroke và text dựa vào trạng thái selected
         */
        fun bind(language: Language) {
            binding.ivFlag.setImageResource(language.flagDrawable)
            binding.tvLanguageName.text = language.name

            if (language.isSelected) {
                binding.cardLanguage.strokeWidth = strokeWidthPx
                binding.cardLanguage.strokeColor = colorGreenPrimary
                binding.tvLanguageName.setTextColor(colorGreenDark)
            } else {
                binding.cardLanguage.strokeWidth = 0
                binding.tvLanguageName.setTextColor(colorTextPrimary)
            }
        }
    }
}