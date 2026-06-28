package com.cobra.cards.utils

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREF_NAME = "cobra_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_BALANCE = "balance"
    private const val KEY_BLOCKED = "blocked"
    private const val KEY_ROLE = "role"
    private const val KEY_LOGGED_IN = "is_logged_in"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(token: String, username: String, balance: Int, role: String = "user") {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .putInt(KEY_BALANCE, balance)
            .putString(KEY_ROLE, role)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    fun getToken(): String = "Bearer ${prefs.getString(KEY_TOKEN, "") ?: ""}"

    fun getRawToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun getBalance(): Int = prefs.getInt(KEY_BALANCE, 0)

    fun updateBalance(balance: Int) {
        prefs.edit().putInt(KEY_BALANCE, balance).apply()
    }

    fun isAdmin(): Boolean = prefs.getString(KEY_ROLE, "user") == "admin"

    fun isBlocked(): Boolean = prefs.getBoolean(KEY_BLOCKED, false)

    fun setBlocked(blocked: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCKED, blocked).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
