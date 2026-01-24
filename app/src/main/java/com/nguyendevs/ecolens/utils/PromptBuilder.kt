package com.nguyendevs.ecolens.utils

/**
 * Builder tạo prompts cho Gemini API. Tối ưu token (~25% so với bản gốc) nhưng giữ chi tiết output.
 */
object PromptBuilder {

  /**
   * Tạo prompt phân loại sinh học.
   * @param scientificName Tên khoa học của loài
   * @param isVietnamese True nếu output tiếng Việt
   * @return Prompt string cho Gemini API
   */
  fun buildTaxonomyPrompt(scientificName: String, isVietnamese: Boolean): String {
    return if (isVietnamese) {
      """Cung cấp phân loại khoa học đầy đủ về "$scientificName" bằng Tiếng Việt.

Trả về JSON:
{"commonName":"Tên thường gọi Tiếng Việt","kingdom":"Tên Tiếng Việt","phylum":"Tên Tiếng Việt","className":"Tên Tiếng Việt","taxorder":"Tên Tiếng Việt","family":"Tên khoa học","genus":"Tên khoa học","species":"Tên khoa học"}

CHỈ TRẢ VỀ JSON, KHÔNG THÊM TEXT."""
    } else {
      """Provide complete taxonomic classification for "$scientificName" in English.

Return JSON:
{"commonName":"Common name","kingdom":"Kingdom name","phylum":"Phylum name","className":"Class name","taxorder":"Order name","family":"Family name","genus":"Genus name","species":"Species name"}

RETURN ONLY JSON, NO ADDITIONAL TEXT."""
    }
  }

  /**
   * Tạo prompt thông tin chi tiết loài.
   * @param scientificName Tên khoa học của loài
   * @param isVietnamese True nếu output tiếng Việt
   * @return Prompt string cho Gemini API
   */
  fun buildDetailsPrompt(scientificName: String, isVietnamese: Boolean): String {
    return if (isVietnamese) {
      """Cung cấp thông tin chi tiết về "$scientificName" bằng Tiếng Việt.

QUY TẮC FORMAT:
• Dùng **text** để in đậm từ khóa quan trọng
• Dùng ##text## để highlight xanh cho địa danh, tên riêng, số đo
• Dùng • cho bullet points

JSON FORMAT:
{
  "description": "Tổng quan 4-5 câu đầy đủ. Dùng **in đậm** cho đặc điểm nổi bật, ##xanh## cho địa danh và số đo.",
  "characteristics": "Danh sách gạch đầu dòng chi tiết:\n• Hình thái và cấu trúc cơ thể\n• Kích thước (dùng ##số đo##)\n• Màu sắc và đặc điểm nhận dạng\n• Đặc điểm sinh học đặc biệt",
  "distribution": "Ưu tiên ##Việt Nam## trước (vùng miền cụ thể), sau đó các quốc gia khác. Dùng ##xanh## cho tất cả địa danh.",
  "habitat": "Mô tả chi tiết môi trường sống: độ cao, khí hậu, thảm thực vật, nguồn thức ăn.",
  "conservationStatus": "Chọn một: Cực kỳ nguy cấp (CR)/Nguy cấp (EN)/Sách Đỏ Việt Nam (VU)/Sắp nguy cấp (NT)/Ít lo ngại (LC)/Chưa đánh giá (NE). Thêm thông tin IUCN và các mối đe dọa chính."
}

CHỈ TRẢ VỀ JSON."""
    } else {
      """Provide detailed information about "$scientificName" in English.

FORMAT RULES:
• Use **text** to bold important keywords
• Use ##text## to green highlight places, names, measurements
• Use • for bullet points

JSON FORMAT:
{
  "description": "Comprehensive 4-5 sentence overview. Use **bold** for key features, ##green## for places and measurements.",
  "characteristics": "Detailed bullet list:\n• Body morphology and structure\n• Size dimensions (use ##measurements##)\n• Colors and identifying features\n• Special biological traits",
  "distribution": "##Vietnam## first if applicable (specific regions), then other countries. Use ##green## for all locations.",
  "habitat": "Detailed environment: elevation, climate, vegetation, food sources.",
  "conservationStatus": "Choose one: Critically Endangered (CR)/Endangered (EN)/Vulnerable (VU)/Near Threatened (NT)/Least Concern (LC)/Not Evaluated (NE). Add IUCN info and main threats."
}

RETURN ONLY JSON."""
    }
  }
}
