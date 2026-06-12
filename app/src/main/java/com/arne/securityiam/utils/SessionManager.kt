package com.arne.securityiam.utils

import android.content.Context
import android.content.SharedPreferences
import com.arne.securityiam.models.User

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID = "user_id"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_ROLE = "user_role"
        private const val KEY_BIRTH_DATE = "user_birth_date"
        private const val KEY_ADDRESS = "user_address"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUserSession(user: User) {
        val editor = prefs.edit()
        editor.putInt(KEY_ID, user.id)
        editor.putString(KEY_NAME, user.name)
        editor.putString(KEY_EMAIL, user.email)
        editor.putString(KEY_ROLE, user.role)
        editor.putString(KEY_BIRTH_DATE, user.birthDate)
        editor.putString(KEY_ADDRESS, user.address)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    fun getUserSession(): User? {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return null

        return User(
            id = prefs.getInt(KEY_ID, -1),
            name = prefs.getString(KEY_NAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            role = prefs.getString(KEY_ROLE, "") ?: "",
            birthDate = prefs.getString(KEY_BIRTH_DATE, null),
            address = prefs.getString(KEY_ADDRESS, null)
        )
    }

    fun logout() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
}
