package com.kazukittin.vault.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface DlSiteApi {
    @GET("home/product/info/ajax")
    suspend fun getProductInfo(
        @Query("product_id") productId: String
    ): Map<String, DlSiteProductInfo>
}

data class DlSiteProductInfo(
    val work_name: String?,
    val maker_name: String?,
    val image_main: DlSiteImage?,
    val genres: List<DlSiteGenre>?,
    val creaters: DlSiteCreaters?,
    val work_type: String?
)

data class DlSiteImage(
    val url: String?
)

data class DlSiteGenre(
    val name: String?
)

data class DlSiteCreaters(
    val voice_by: List<DlSiteVoice>?,
    val author: List<DlSiteVoice>?, // 著者
    val illustration: List<DlSiteVoice>?, // イラスト
    val scenario: List<DlSiteVoice>? // シナリオ
)

data class DlSiteVoice(
    val name: String?
)
