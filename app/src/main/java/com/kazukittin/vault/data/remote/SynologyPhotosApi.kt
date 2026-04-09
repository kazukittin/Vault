package com.kazukittin.vault.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface SynologyPhotosApi {
    
    /**
     * Synology Photosのフォルダリストを取得する（Personal Space）
     */
    @GET("photo/webapi/entry.cgi")
    suspend fun getFolders(
        @Query("api") api: String = "SYNO.Foto.Browse.Folder",
        @Query("version") version: Int = 1,
        @Query("method") method: String = "list",
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
    @SerializedName("list") val list: List<SynoFolder>
)

data class SynoFolder(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("parent") val parent: Int
)
