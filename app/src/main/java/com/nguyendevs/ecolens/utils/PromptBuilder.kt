package com.nguyendevs.ecolens.utils

/** Builder tạo prompts cho Gemini API. Đã tối ưu giảm ~40% tokens. */
object PromptBuilder {

  /**
   * Tạo prompt phân loại sinh học.
   * @param scientificName Tên khoa học của loài
   * @param isVietnamese True nếu output tiếng Việt
   * @return Prompt string cho Gemini API
   */
  fun buildTaxonomyPrompt(scientificName: String, isVietnamese: Boolean): String {
    return if (isVietnamese) {
      """Phân loại khoa học "$scientificName" (Tiếng Việt). JSON only:
{"commonName":"Tên thường gọi","kingdom":"Giới","phylum":"Ngành","className":"Lớp","taxorder":"Bộ","family":"Họ (khoa học)","genus":"Chi (khoa học)","species":"Loài (khoa học)"}"""
    } else {
      """Taxonomy for "$scientificName" in English. JSON only:
{"commonName":"Common name","kingdom":"Kingdom","phylum":"Phylum","className":"Class","taxorder":"Order","family":"Family","genus":"Genus","species":"Species"}"""
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
      """Thông tin "$scientificName" (Tiếng Việt).
Format: **bold**, ##green##, • bullets
JSON: {"description":"4 câu, **đặc điểm nổi bật**, ##địa danh/số đo##","characteristics":"• hình thái, kích thước, màu sắc","distribution":"##Việt Nam## trước, toàn cầu sau","habitat":"Môi trường sống","conservationStatus":"Cực kỳ nguy cấp/Nguy cấp/Sách Đỏ VN/Sắp nguy cấp/Ít lo ngại/Chưa đánh giá + IUCN info"}
JSON only."""
    } else {
      """Details for "$scientificName" in English.
Format: **bold**, ##green##, • bullets
JSON: {"description":"4 sentences, **key features**, ##places/measurements##","characteristics":"• morphology, size, colors","distribution":"##Vietnam## first if applicable, then worldwide","habitat":"Environment details","conservationStatus":"Critically Endangered/Endangered/Vulnerable/Near Threatened/Least Concern/Not Evaluated + IUCN info"}
JSON only."""
    }
  }
}
