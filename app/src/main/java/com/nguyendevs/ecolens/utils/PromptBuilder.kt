package com.nguyendevs.ecolens.utils

object PromptBuilder {

    fun buildTaxonomyPrompt(scientificName: String, isVietnamese: Boolean): String {
        return if (isVietnamese) {
            """
            Cung cấp thông tin phân loại khoa học về "$scientificName" bằng Tiếng Việt.
            
            Trả về JSON với format:
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
            
            CHỈ TRẢ VỀ JSON, KHÔNG THÊM TEXT KHÁC.
            """.trimIndent()
        } else {
            """
            Provide taxonomic classification for "$scientificName" in English.
            
            Return JSON format:
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
        }
    }

    fun buildDetailsPrompt(scientificName: String, isVietnamese: Boolean): String {
        return if (isVietnamese) {
            """
            Cung cấp thông tin chi tiết về "$scientificName" bằng Tiếng Việt.
            
            === QUY TẮC FORMAT ===
            - Dùng ** để in đậm (ví dụ: **từ khóa**)
            - Dùng ## để highlight xanh (ví dụ: ##Việt Nam##)
            - Dùng • cho bullet points
            
            === JSON FORMAT ===
            {
              "description": "Tổng quan 4 câu ngắn gọn, dùng **in đậm** cho đặc điểm nổi bật và ##xanh đậm## cho địa danh, tên riêng, số đo.",
              "characteristics": "Danh sách gạch đầu dòng, mỗi dòng bắt đầu với • và một ý về hình thái, kích thước, màu sắc. Dùng **in đậm** và ##xanh đậm##.",
              "distribution": "Ưu tiên Việt Nam trước (nếu có), sau đó toàn cầu. Dùng ##xanh đậm## cho tên địa danh.",
              "habitat": "Mô tả chi tiết môi trường sống.",
              "conservationStatus": "Chỉ ghi một trong các trạng thái: Cực kỳ nguy cấp, Nguy cấp, Sách Đỏ Việt Nam, Sắp nguy cấp, Ít lo ngại, Chưa đánh giá. Thêm một chút thông tin bổ sung từ IUCN nếu có."
            }
            
            CHỈ TRẢ VỀ JSON.
            """.trimIndent()
        } else {
            """
            Provide detailed information about "$scientificName" in English.
            
            === FORMAT RULES ===
            - Use ** for bold (e.g., **keyword**)
            - Use ## for green highlight (e.g., ##Vietnam##)
            - Use • for bullet points
            
            === JSON FORMAT ===
            {
              "description": "4-sentence overview with **bold** for key features and ##green highlight## for places/names/measurements.",
              "characteristics": "Bullet list, each line starts with • covering morphology, size, colors. Use **bold** and ##green highlight##.",
              "distribution": "Vietnam first if applicable, then worldwide. Use ##green highlight## for locations.",
              "habitat": "Environment details",
              "conservationStatus": "Only write one of these statuses: Critically Endangered, Endangered, Vulnerable (Vietnam Red Data Book), Near Threatened, Least Concern, Not Evaluated. Add some additional IUCN info if available."
            }
            
            RETURN ONLY JSON.
            """.trimIndent()
        }
    }
}
