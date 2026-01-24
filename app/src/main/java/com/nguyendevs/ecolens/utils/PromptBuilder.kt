package com.nguyendevs.ecolens.utils

/**
 * Builder cho các prompts gửi đến Gemini API Đã tối ưu để giảm token count (~20-30%) trong khi giữ
 * nguyên chất lượng output
 */
object PromptBuilder {

  /** Build prompt cho taxonomy classification Giảm tokens bằng cách compact JSON example */
  fun buildTaxonomyPrompt(scientificName: String, isVietnamese: Boolean): String {
    return if (isVietnamese) {
      """Phân loại khoa học "$scientificName" (Tiếng Việt). JSON only:
{"commonName":"Tên thường gọi","kingdom":"Giới","phylum":"Ngành","className":"Lớp","taxorder":"Bộ","family":"Họ (khoa học)","genus":"Chi (khoa học)","species":"Loài (khoa học)"}"""
    } else {
      """Taxonomy for "$scientificName" in English. JSON only:
{"commonName":"Common name","kingdom":"Kingdom","phylum":"Phylum","className":"Class","taxorder":"Order","family":"Family","genus":"Genus","species":"Species"}"""
    }
  }

  /** Build prompt cho species details Giữ nguyên format rules vì ảnh hưởng đến UI rendering */
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
