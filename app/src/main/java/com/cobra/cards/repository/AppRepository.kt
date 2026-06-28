package com.cobra.cards.repository

import com.cobra.cards.api.RetrofitClient
import com.cobra.cards.model.*
import com.cobra.cards.utils.SessionManager

class AppRepository {

    private val api = RetrofitClient.apiService

    // ── Auth ──
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("خطأ في الاتصال: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال بالسيرفر: ${e.message}"))
        }
    }

    // ── Balance ──
    suspend fun getBalance(): Result<BalanceResponse> {
        return try {
            val response = api.getBalance(SessionManager.getToken())
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.blocked) SessionManager.setBlocked(true)
                else SessionManager.updateBalance(body.balance)
                Result.success(body)
            } else {
                Result.failure(Exception("فشل جلب الرصيد"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    // ── Profile ──
    suspend fun getProfile(): Result<ProfileResponse> {
        return try {
            val response = api.getProfile(SessionManager.getToken())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب البيانات"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    // ── History ──
    suspend fun getHistory(): Result<HistoryResponse> {
        return try {
            val response = api.getHistory(SessionManager.getToken())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب السجل"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    // ── Notifications ──
    suspend fun getNotifications(): Result<NotificationsResponse> {
        return try {
            val response = api.getNotifications(SessionManager.getToken())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب الإشعارات"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    // ── Recharge ──
    suspend fun authorize(productId: String, receiver: String, pin: String): Result<RechargeResponse> {
        return try {
            val response = api.authorizeRecharge(
                SessionManager.getToken(),
                RechargeRequest(productId, receiver, pin)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل التفويض"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    suspend fun report(authId: String, success: Boolean, message: String): Result<GenericResponse> {
        return try {
            val response = api.reportRecharge(
                SessionManager.getToken(),
                ReportRequest(authId, success, message)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل الإبلاغ"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    // ── Admin ──
    suspend fun getUsers(): Result<AdminUsersResponse> {
        return try {
            val response = api.getUsers(SessionManager.getToken())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("غير مصرح"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    suspend fun createUser(username: String, password: String, balance: Int): Result<GenericResponse> {
        return try {
            val response = api.createUser(
                SessionManager.getToken(),
                CreateUserRequest(username, password, balance)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل إنشاء المستخدم"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    suspend fun editBalance(username: String, balance: Int): Result<GenericResponse> {
        return try {
            val response = api.editBalance(
                SessionManager.getToken(),
                EditBalanceRequest(username, balance)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل تعديل الرصيد"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    suspend fun deleteUser(username: String): Result<GenericResponse> {
        return try {
            val response = api.deleteUser(
                SessionManager.getToken(),
                DeleteUserRequest(username)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل الحذف"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }

    suspend fun blockUser(username: String, blocked: Boolean): Result<GenericResponse> {
        return try {
            val response = api.blockUser(
                SessionManager.getToken(),
                BlockUserRequest(username, blocked)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل الحظر"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال: ${e.message}"))
        }
    }
}
