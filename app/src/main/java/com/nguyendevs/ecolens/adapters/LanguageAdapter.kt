package com.nguyendevs.ecolens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.Language

class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardLanguage: MaterialCardView = view.findViewById(R.id.cardLanguage)
        val tvFlag: TextView = view.findViewById(R.id.tvFlag)
        val tvLanguageName: TextView = view.findViewById(R.id.tvLanguageName)

        fun bind(language: Language) {
            tvFlag.text = language.flagEmoji
            tvLanguageName.text = language.name

            // Update selection state
            cardLanguage.strokeWidth = if (language.isSelected) {
                itemView.context.resources.getDimensionPixelSize(R.dimen.selected_stroke_width)
            } else {
                0
            }

            cardLanguage.setOnClickListener {
                onLanguageClick(language)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount() = languages.size
}