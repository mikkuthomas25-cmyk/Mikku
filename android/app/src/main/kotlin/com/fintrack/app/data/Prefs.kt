package com.fintrack.app.data

import android.content.Context

/** Small app settings, private to this app's sandbox. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("fintrack", Context.MODE_PRIVATE)

    /** Database of the signed-in account; the SMS receiver writes here. Null when signed out. */
    var activeDb: String?
        get() = sp.getString("active_db", null)
        set(v) = sp.edit().putString("active_db", v).apply()

    var activeUserLabel: String?
        get() = sp.getString("active_user", null)
        set(v) = sp.edit().putString("active_user", v).apply()

    var claudeApiKey: String
        get() = sp.getString("claude_api_key", "") ?: ""
        set(v) = sp.edit().putString("claude_api_key", v.trim()).apply()

    companion object {
        const val LOCAL_DB = "fintrack_local.db"
        fun dbFor(uid: String) = "fintrack_${uid.filter { it.isLetterOrDigit() }}.db"
    }
}
