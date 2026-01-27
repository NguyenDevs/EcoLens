package com.nguyendevs.ecolens.utils

object PromptBuilder {

    fun buildCommonNamePrompt(scientificName: String, isVietnamese: Boolean): String {
        return if (isVietnamese) {
            """
            Cho biết tên thường gọi phổ biến nhất của "$scientificName" bằng Tiếng Việt.
            Trả về JSON duy nhất: {"commonName": "Tên Tiếng Việt"}
            CHỈ TRẢ VỀ JSON, KHÔNG MARKDOWN.
            """.trimIndent()
        } else {
            """
            Provide the most common name for "$scientificName" in English.
            Return JSON only: {"commonName": "Name"}
            RETURN ONLY JSON, NO MARKDOWN.
            """.trimIndent()
        }
    }

    fun buildDetailsPrompt(scientificName: String, isVietnamese: Boolean): String {
        return if (isVietnamese) {
            buildVietnameseDetailsPrompt(scientificName)
        } else {
            buildEnglishDetailsPrompt(scientificName)
        }
    }

    fun buildConservationPrompt(scientificName: String, iucnCode: String, isVietnamese: Boolean): String {
        return if (isVietnamese) {
            if (iucnCode.isNotBlank()) {
                """
                Phân tích tình trạng bảo tồn IUCN "$iucnCode" cho loài "$scientificName" bằng Tiếng Việt.
                
                Yêu cầu định dạng kết quả trong JSON (sử dụng \n để xuống dòng):
                • **Tình trạng bảo tồn:** $iucnCode (Giải nghĩa ngắn gọn)
                • **Giải thích tình trạng:** (Mô tả ngắn gọn về tình trạng này đối với loài)
                • **Các mối đe doạ chính:** (Liệt kê các mối đe dọa chính)
                
                Trả về JSON:
                { "conservationStatus": "Nội dung đã định dạng..." }
                
                CHỈ TRẢ VỀ JSON.
                """.trimIndent()
            } else {
                """
                Hãy xác định tình trạng bảo tồn IUCN cho loài "$scientificName" bằng Tiếng Việt.
                
                Yêu cầu định dạng kết quả trong JSON (sử dụng \n để xuống dòng):
                • **Tình trạng bảo tồn:** [Mã IUCN] (Giải nghĩa ngắn gọn)
                • **Giải thích tình trạng:** (Mô tả ngắn gọn về tình trạng này đối với loài)
                • **Các mối đe doạ chính:** (Liệt kê các mối đe dọa chính)
                
                Trả về JSON:
                { "conservationStatus": "Nội dung đã định dạng..." }
                
                CHỈ TRẢ VỀ JSON.
                """.trimIndent()
            }
        } else {
            if (iucnCode.isNotBlank()) {
                """
                Analyze IUCN conservation status "$iucnCode" for species "$scientificName" in English.
                
                Format the result in JSON (use \n for new lines):
                • **Conservation Status:** $iucnCode (Brief meaning)
                • **Status Explanation:** (Brief description of the status for this species)
                • **Main Threats:** (List main threats)
                
                Return JSON:
                { "conservationStatus": "Formatted content..." }
                
                RETURN ONLY JSON.
                """.trimIndent()
            } else {
                """
                Determine the IUCN conservation status for species "$scientificName" in English.
                
                Format the result in JSON (use \n for new lines):
                • **Conservation Status:** [IUCN Code] (Brief meaning)
                • **Status Explanation:** (Brief description of the status for this species)
                • **Main Threats:** (List main threats)
                
                Return JSON:
                { "conservationStatus": "Formatted content..." }
                
                RETURN ONLY JSON.
                """.trimIndent()
            }
        }
    }

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
          "habitat": "Mô tả chi tiết môi trường sống: độ cao, khí hậu, thảm thực vật, nguồn thức ăn."
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
          "habitat": "Detailed environment: elevation, climate, vegetation, food sources."
        }
        
        RETURN ONLY JSON.
    """.trimIndent()
}
