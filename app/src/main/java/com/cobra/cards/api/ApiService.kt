package com.cobra.cards.api

import com.cobra.cards.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ── User ──
    @GET("api/user/balance")
    suspend fun getBalance(
        @Header("Authorization") token: String
    ): Response<BalanceResponse>

    @GET("api/user/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ProfileResponse>

    @GET("api/user/history")
    suspend fun getHistory(
        @Header("Authorization") token: String
    ): Response<HistoryResponse>

    @GET("api/user/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<NotificationsResponse>

    // ── Recharge ──
    @POST("api/recharge/authorize")
    suspend fun authorizeRecharge(
        @Header("Authorization") token: String,
        @Body request: RechargeRequest
    ): Response<RechargeResponse>

    @POST("api/recharge/report")
    suspend fun reportRecharge(
        @Header("Authorization") token: String,
        @Body request: ReportRequest
    ): Response<GenericResponse>

    // ── Admin ──
    @GET("api/admin/users")
    suspend fun getUsers(
        @Header("Authorization") token: String
    ): Response<AdminUsersResponse>

    @POST("api/admin/users/create")
    suspend fun createUser(
        @Header("Authorization") token: String,
        @Body request: CreateUserRequest
    ): Response<GenericResponse>

    @POST("api/admin/users/balance")
    suspend fun editBalance(
        @Header("Authorization") token: String,
        @Body request: EditBalanceRequest
    ): Response<GenericResponse>

    @POST("api/admin/users/delete")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Body request: DeleteUserRequest
    ): Response<GenericResponse>

    @POST("api/admin/users/block")
    suspend fun blockUser(
        @Header("Authorization") token: String,
        @Body request: BlockUserRequest
    ): Response<GenericResponse>
}
