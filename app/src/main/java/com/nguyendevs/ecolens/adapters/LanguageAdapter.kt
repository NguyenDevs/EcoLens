package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemLanguagesBinding
import com.nguyendevs.ecolens.models.Language

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
        val binding = ItemLanguagesBinding.inflate(
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

    fun updateList(newLanguages: List<Language>) {
        languages = newLanguages
        notifyDataSetChanged()
    }

    // ==================== VIEW HOLDER ====================

    inner class LanguageViewHolderModern(
        private val binding: ItemLanguagesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val colorPrimary = ContextCompat.getColor(itemView.context, R.color.primary)
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

        fun bind(language: Language) {
            binding.ivFlag.setImageResource(language.flagDrawable)
            binding.tvLanguageName.text = language.name

            if (language.isSelected) {
                binding.cardLanguage.strokeWidth = strokeWidthPx
                binding.cardLanguage.strokeColor = colorPrimary
                binding.tvLanguageName.setTextColor(colorPrimary)
            } else {
                binding.cardLanguage.strokeWidth = 0
                binding.tvLanguageName.setTextColor(colorTextPrimary)
            }
        }
    }
}