package com.cobra.cards.model

import com.google.gson.annotations.SerializedName

// ── Auth ──
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String? = null,
    val user: UserData? = null
)

data class UserData(
    val username: String,
    val balance: Int,
    val blocked: Boolean = false,
    val role: String = "user"
)

// ── Balance ──
data class BalanceResponse(
    val success: Boolean,
    val balance: Int = 0,
    val blocked: Boolean = false,
    val message: String? = null
)

// ── Recharge ──
data class RechargeRequest(
    @SerializedName("product_id") val productId: String,
    val receiver: String,
    val pin: String
)

data class RechargeResponse(
    val success: Boolean,
    val message: String? = null,
    @SerializedName("auth_id") val authId: String? = null,
    @SerializedName("new_balance") val newBalance: Int? = null
)

data class ReportRequest(
    @SerializedName("auth_id") val authId: String,
    val success: Boolean,
    val message: String
)

// ── History ──
data class HistoryItem(
    val id: Int,
    @SerializedName("product_id") val productId: String,
    val receiver: String,
    val success: Int,
    val message: String?,
    @SerializedName("created_at") val createdAt: String
)

data class HistoryResponse(
    val success: Boolean,
    val history: List<HistoryItem> = emptyList(),
    val message: String? = null
)

// ── Profile ──
data class ProfileResponse(
    val success: Boolean,
    val username: String = "",
    val balance: Int = 0,
    @SerializedName("total_recharges") val totalRecharges: Int = 0,
    @SerializedName("success_recharges") val successRecharges: Int = 0,
    val blocked: Boolean = false,
    val message: String? = null
)

// ── Admin ──
data class AdminUsersResponse(
    val success: Boolean,
    val users: List<AdminUser> = emptyList()
)

data class AdminUser(
    val id: Int,
    val username: String,
    val balance: Int,
    val blocked: Boolean = false
)

data class CreateUserRequest(
    val username: String,
    val password: String,
    val balance: Int
)

data class EditBalanceRequest(
    val username: String,
    val balance: Int
)

data class DeleteUserRequest(
    val username: String
)

data class BlockUserRequest(
    val username: String,
    val blocked: Boolean
)

data class GenericResponse(
    val success: Boolean,
    val message: String? = null
)

// ── Notifications ──
data class NotificationsResponse(
    val success: Boolean,
    val notifications: List<NotificationItem> = emptyList()
)

data class NotificationItem(
    val id: Int,
    val title: String,
    val body: String,
    @SerializedName("created_at") val createdAt: String,
    val read: Boolean = false
)
