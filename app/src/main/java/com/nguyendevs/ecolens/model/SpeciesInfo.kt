package com.nguyendevs.ecolens.model

data class SpeciesInfo(
    val commonName: String = "",
    val scientificName: String = "",
    val kingdom: String = "",
    val phylum: String = "",
    val className: String = "",
    val order: String = "",
    val family: String = "",
    val genus: String = "",
    val species: String = "",
    val rank: String = "",
    val description: String = "",
    val characteristics: String = "",
    val distribution: String = "",
    val habitat: String = "",
    val conservationStatus: String = "",
    val confidence: Double = 0.0
)