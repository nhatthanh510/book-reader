package com.example.docsach.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Response shape of GET /volumes. `items` is absent (not empty) when nothing matches. */
data class VolumesResponse(
    @SerializedName("totalItems") val totalItems: Int = 0,
    @SerializedName("items") val items: List<VolumeDto>? = null,
)

data class VolumeDto(
    @SerializedName("id") val id: String,
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfoDto? = null,
    @SerializedName("saleInfo") val saleInfo: SaleInfoDto? = null,
    @SerializedName("accessInfo") val accessInfo: AccessInfoDto? = null,
)

data class VolumeInfoDto(
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("authors") val authors: List<String>? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("categories") val categories: List<String>? = null,
    @SerializedName("imageLinks") val imageLinks: ImageLinksDto? = null,
    @SerializedName("publishedDate") val publishedDate: String? = null,
    @SerializedName("pageCount") val pageCount: Int? = null,
    @SerializedName("averageRating") val averageRating: Double? = null,
    @SerializedName("previewLink") val previewLink: String? = null,
)

data class ImageLinksDto(
    @SerializedName("smallThumbnail") val smallThumbnail: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("small") val small: String? = null,
    @SerializedName("medium") val medium: String? = null,
    @SerializedName("large") val large: String? = null,
)

data class SaleInfoDto(
    @SerializedName("saleability") val saleability: String? = null,
    @SerializedName("listPrice") val listPrice: PriceDto? = null,
    @SerializedName("retailPrice") val retailPrice: PriceDto? = null,
)

data class PriceDto(
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
)

data class AccessInfoDto(
    @SerializedName("viewability") val viewability: String? = null,
    @SerializedName("embeddable") val embeddable: Boolean? = null,
    @SerializedName("publicDomain") val publicDomain: Boolean? = null,
    @SerializedName("webReaderLink") val webReaderLink: String? = null,
)
