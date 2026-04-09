package com.kazukittin.vault.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SynologyAuthApi {
    @GET("webapi/auth.cgi")
    suspend fun login(
        @Query("api") api: String = "SYNO.API.Auth",
        @Query("version") version: Int = 3,
        @Query("method") method: String = "login",
        @Query("account") account: String,
        @Query("passwd") passwd: String,
        @Query("session") session: String = "Foto", // Synology Photos standard session
        @Query("format") format: String = "sid"
    ): Response<SynologyLoginResponse>
}

data class SynologyLoginResponse(
    val data: LoginData?,
    val success: Boolean,
    val error: SynologyError?
)

data class LoginData(
    val sid: String
)

data class SynologyError(
    val code: Int
)
