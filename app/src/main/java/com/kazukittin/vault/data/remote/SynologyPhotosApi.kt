package com.kazukittin.vault.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface SynologyPhotosApi {
    
    /**
     * Synology Photosのフォルダリストを取得する（Personal Space）
     */
    @GET("webapi/entry.cgi")
    suspend fun getFolders(
        @Query("api") api: String = "SYNO.FileStation.List",
        @Query("version") version: Int = 2,
        @Query("method") method: String = "list_share",
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("_sid") sessionId: String
    ): FolderResponse
}

data class FolderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: FolderData?,
    @SerializedName("error") val error: FolderError?
)

data class FolderError(
    @SerializedName("code") val code: Int
)

data class FolderData(
    @SerializedName("shares") val shares: List<SynoFolder>?
)

data class SynoFolder(
    @SerializedName("path") val path: String, // "/photo" など
    @SerializedName("name") val name: String,
    @SerializedName("isdir") val isDir: Boolean
)
