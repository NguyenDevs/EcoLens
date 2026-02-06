package com.nguyendevs.ecolens.models

/**
 * Thông tin chi tiết về một loài sinh vật
 *
 * @property commonName Tên thông thường
 * @property scientificName Tên khoa học
 * @property kingdom Giới
 * @property phylum Ngành
 * @property className Lớp
 * @property taxorder Bộ
 * @property family Họ
 * @property genus Chi
 * @property species Loài
 * @property description Mô tả chung
 * @property characteristics Đặc điểm nhận dạng
 * @property distribution Phân bố địa lý
 * @property habitat Môi trường sống
 * @property conservationStatus Tình trạng bảo tồn
 * @property confidence Độ tin cậy của kết quả nhận diện (0.0 - 1.0)
 * @property iucn Trạng thái hiển thị IUCN tại thời điểm nhận diện
 */
data class SpeciesInfo(
    val commonName: String = "",
    val scientificName: String = "",
    val kingdom: String = "",
    val phylum: String = "",
    val className: String = "",
    val taxorder: String = "",
    val family: String = "",
    val genus: String = "",
    val species: String = "",
    val description: String = "",
    val characteristics: String = "",
    val distribution: String = "",
    val habitat: String = "",
    val conservationStatus: String = "",
    val confidence: Double = 0.0,
    val iucn: Boolean = true
)