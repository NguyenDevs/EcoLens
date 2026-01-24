package com.nguyendevs.ecolens.utils

/**
 * Builder for generating optimized prompts for Gemini API.
 * Token-optimized (~25% reduction) while maintaining output quality.
 */
object PromptBuilder {

    /**
     * Generates a taxonomic classification prompt.
     *
     * @param scientificName Scientific name of the species
     * @param isVietnamese Whether to request Vietnamese output
     * @return Formatted prompt string for Gemini API
     */
    fun buildTaxonomyPrompt(scientificName: String, isVietnamese: Boolean): String {
        return if (isVietnamese) {
            buildVietnameseTaxonomyPrompt(scientificName)
        } else {
            buildEnglishTaxonomyPrompt(scientificName)
        }
    }

    /**
     * Generates a detailed species information prompt.
     *
     * @param scientificName Scientific name of the species
     * @param isVietnamese Whether to request Vietnamese output
     * @return Formatted prompt string for Gemini API
     */
    fun buildDetailsPrompt(scientificName: String, isVietnamese: Boolean): String {
        return if (isVietnamese) {
            buildVietnameseDetailsPrompt(scientificName)
        } else {
            buildEnglishDetailsPrompt(scientificName)
        }
    }

    // ========== Private Helper Methods ==========

    private fun buildVietnameseTaxonomyPrompt(scientificName: String): String = """
        Cung cấp phân loại khoa học về "$scientificName" bằng Tiếng Việt.
        
        Trả về JSON:
        {
          "commonName": "Tên thường gọi Tiếng Việt",
          "kingdom": "Tên Tiếng Việt",
          "phylum": "Tên Tiếng Việt",
          "className": "Tên Tiếng Việt",
          "taxorder": "Tên Tiếng Việt",
          "family": "Tên khoa học",
          "genus": "Tên khoa học",
          "species": "Tên khoa học"
        }
        
        CHỈ TRẢ VỀ JSON, KHÔNG THÊM TEXT.
    """.trimIndent()

    private fun buildEnglishTaxonomyPrompt(scientificName: String): String = """
        Provide taxonomic classification for "$scientificName" in English.
        
        Return JSON:
        {
          "commonName": "Common name",
          "kingdom": "Kingdom name",
          "phylum": "Phylum name",
          "className": "Class name",
          "taxorder": "Order name",
          "family": "Family name",
          "genus": "Genus name",
          "species": "Species name"
        }
        
        RETURN ONLY JSON, NO ADDITIONAL TEXT.
    """.trimIndent()

    private fun buildVietnameseDetailsPrompt(scientificName: String): String = """
        Cung cấp thông tin chi tiết về "$scientificName" bằng Tiếng Việt.
        
        QUY TẮC FORMAT:
        • Dùng **text** để in đậm từ khóa quan trọng
        • Dùng ##text## để highlight xanh cho địa danh, tên riêng, số đo
        • Dùng • cho bullet points
        
        JSON FORMAT:
        {
          "description": "Tổng quan 4-5 câu đầy đủ. Dùng **in đậm** và ##xanh## cho đặc điểm nổi bật, địa danh và số đo.",
          "characteristics": "Danh sách gạch đầu dòng, mỗi dòng bắt đầu với dấu •:\n• Hình thái cơ thể\n• Cấu trúc cơ thể\n• Kích thước (dùng ##số đo##)\n• Màu sắc\n• Đặc điểm nhận dạng\n• Đặc điểm sinh học đặc biệt",
          "distribution": "Ưu tiên Việt Nam trước (nếu có), sau đó toàn cầu. Dùng ##xanh đậm## cho tên địa danh.",
          "habitat": "Mô tả chi tiết môi trường sống: độ cao, khí hậu, thảm thực vật, nguồn thức ăn.",
          "conservationStatus": "Chọn một: Cực kỳ nguy cấp (CR)/Nguy cấp (EN)/Sách Đỏ Việt Nam (VU)/Sắp nguy cấp (NT)/Ít lo ngại (LC)/Chưa đánh giá (NE). Thêm thông tin IUCN và các mối đe dọa chính."
        }
        
        CHỈ TRẢ VỀ JSON.
    """.trimIndent()

    private fun buildEnglishDetailsPrompt(scientificName: String): String = """
        Provide detailed information about "$scientificName" in English.
        
        FORMAT RULES:
        • Use **text** to bold important keywords
        • Use ##text## to green highlight places, names, measurements
        • Use • for bullet points
        
        JSON FORMAT:
        {
          "description": "Comprehensive 4-5 sentence overview. Use **bold** and ##green## for key features, places and measurements.",
          "characteristics": "Bullet list, each line starts with •:\n• Body morphology\n• Body structure\n• Size dimensions (use ##measurements##)\n• Colors\n• Identifying features\n• Special biological traits",
          "distribution": "Vietnam first if applicable, then worldwide. Use ##green highlight## for locations.",
          "habitat": "Detailed environment: elevation, climate, vegetation, food sources.",
          "conservationStatus": "Choose one: Critically Endangered (CR)/Endangered (EN)/Vulnerable (VU)/Near Threatened (NT)/Least Concern (LC)/Not Evaluated (NE). Add IUCN info and main threats."
        }
        
        RETURN ONLY JSON.
    """.trimIndent()
}