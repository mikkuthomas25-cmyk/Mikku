package com.fintrack.app.data

import android.content.Context

/** Small app settings, private to this app's sandbox. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("fintrack", Context.MODE_PRIVATE)

    /** Database of the signed-in account; the SMS receiver writes here. Null when signed out. */
    var activeDb: String?
        get() = sp.getString("active_db", null)
        set(v) = sp.edit().putString("active_db", v).apply()

    var activeUserName: String?
        get() = sp.getString("active_user", null)
        set(v) = sp.edit().putString("active_user", v).apply()

    var activeUserEmail: String?
        get() = sp.getString("active_email", null)
        set(v) = sp.edit().putString("active_email", v).apply()

    init {
        // Earlier versions could store a Claude API key; categorizing is now on-device only.
        if (sp.contains("claude_api_key")) sp.edit().remove("claude_api_key").apply()
    }

    companion object {
        const val LOCAL_DB = "fintrack_local.db"
        fun dbFor(uid: String) = "fintrack_${uid.filter { it.isLetterOrDigit() }}.db"
    }
}
