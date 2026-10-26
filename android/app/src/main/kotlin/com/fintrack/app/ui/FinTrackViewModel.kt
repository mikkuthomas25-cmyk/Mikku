package com.fintrack.app.ui

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.auth.Account
import com.fintrack.app.auth.AuthManager
import com.fintrack.app.data.Prefs
import com.fintrack.app.data.TxnStore
import com.fintrack.app.sms.SmsImporter
import com.fintrack.core.Categorizer
import com.fintrack.core.ClaudeAi
import com.fintrack.core.Direction
import com.fintrack.core.MonthSummary
import com.fintrack.core.Summaries
import com.fintrack.core.Txn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

sealed interface TxnFilter {
    data class Day(val date: LocalDate) : TxnFilter
    data class Category(val name: String) : TxnFilter
    data class Merchant(val name: String) : TxnFilter
}

class FinTrackViewModel(private val app: Application) : AndroidViewModel(app) {
    val auth = AuthManager(app)
    val prefs = Prefs(app)
    private var store: TxnStore? = null

    var account by mutableStateOf<String?>(null); private set
    var month by mutableStateOf(YearMonth.now()); private set
    var summary by mutableStateOf<MonthSummary?>(null); private set
    var txns by mutableStateOf<List<Txn>>(emptyList()); private set
    var filter by mutableStateOf<TxnFilter?>(null)
    var hasSmsPermission by mutableStateOf(SmsImporter.hasPermission(app)); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)
    var insights by mutableStateOf<String?>(null); private set

    val filteredTxns: List<Txn>
        get() = when (val f = filter) {
            null -> txns
            is TxnFilter.Day -> txns.filter { it.date == f.date }
            is TxnFilter.Category -> txns.filter { it.category == f.name && it.direction == Direction.DEBIT }
            is TxnFilter.Merchant -> txns.filter { it.merchant == f.name && it.direction == Direction.DEBIT }
        }

    init {
        when {
            auth.configured -> auth.current()?.let { openSession(it) }
            // No login available in this build: reopen the local session if one was started.
            prefs.activeDb == Prefs.LOCAL_DB -> openSession(null)
        }
    }

    // ------------------------------------------------------------------ session

    private fun openSession(acc: Account?) {
        val dbName = acc?.let { Prefs.dbFor(it.uid) } ?: Prefs.LOCAL_DB
        store?.close()
        store = TxnStore(app, dbName)
        prefs.activeDb = dbName
        prefs.activeUserLabel = acc?.label ?: "This device only"
        account = prefs.activeUserLabel
        onResume()
    }

    fun continueWithoutAccount() = openSession(null)

    fun signIn(action: suspend AuthManager.() -> Account) = viewModelScope.launch {
        busy = true
        try {
            openSession(auth.action())
        } catch (e: Exception) {
            message = e.message ?: "Sign-in failed."
        } finally {
            busy = false
        }
    }

    fun signInWithGoogle(activity: Activity) = signIn { signInWithGoogle(activity) }

    fun resetPassword(email: String) = viewModelScope.launch {
        message = try {
            auth.sendPasswordReset(email)
            "Password reset link sent to $email."
        } catch (e: Exception) {
            e.message
        }
    }

    fun signOut() = viewModelScope.launch {
        auth.signOut()
        prefs.activeDb = null
        store?.close()
        store = null
        account = null
        txns = emptyList()
        summary = null
        insights = null
    }

    // ------------------------------------------------------------------ data

    /** Called whenever the app comes to the foreground: pick up new SMS, then redraw. */
    fun onResume() {
        hasSmsPermission = SmsImporter.hasPermission(app)
        if (hasSmsPermission) scan(full = false) else refresh()
    }

    fun onPermissionResult() {
        hasSmsPermission = SmsImporter.hasPermission(app)
        if (hasSmsPermission) scan(full = true)
    }

    fun scan(full: Boolean) {
        val s = store ?: return
        viewModelScope.launch {
            busy = true
            val added = withContext(Dispatchers.IO) { SmsImporter.scanInbox(app, s, full) }
            if (full) message = "Found $added transactions in your messages."
            refresh()
            busy = false
            if (added > 0) aiCategorize()
        }
    }

    fun refresh() {
        val s = store ?: return
        viewModelScope.launch {
            val m = month
            val (current, previous) = withContext(Dispatchers.IO) { s.month(m) to s.month(m.minusMonths(1)) }
            txns = current
            summary = Summaries.build(m, current, previous)
        }
    }

    fun changeMonth(delta: Long) {
        month = month.plusMonths(delta)
        filter = null
        insights = null
        refresh()
    }

    fun setCategory(txn: Txn, category: String, applyToMerchant: Boolean) = io {
        it.setCategory(txn, category, applyToMerchant)
    }

    fun delete(txn: Txn) = io { it.delete(txn.id) }

    fun addManual(date: LocalDate, amount: Double, direction: Direction, merchant: String) = io {
        val category = Categorizer.categorize(merchant, direction, it.rules())
        it.addManual(date, amount, direction, merchant.trim(), category)
    }

    private fun io(block: (TxnStore) -> Unit) {
        val s = store ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { block(s) }
            refresh()
        }
    }

    // ------------------------------------------------------------------ AI

    val aiEnabled get() = prefs.claudeApiKey.isNotBlank()

    /** Sends only merchant names of payments the keyword rules couldn't place. */
    private fun aiCategorize() {
        val s = store ?: return
        val key = prefs.claudeApiKey.ifBlank { return }
        viewModelScope.launch {
            val changed = withContext(Dispatchers.IO) {
                runCatching {
                    val merchants = s.otherMerchants().take(100)
                    ClaudeAi(key).categorizeMerchants(merchants).onEach { (m, c) -> s.fillOtherCategory(m, c) }
                }.getOrDefault(emptyMap())
            }
            if (changed.isNotEmpty()) refresh()
        }
    }

    fun generateInsights() {
        val s = summary ?: return
        val key = prefs.claudeApiKey.ifBlank {
            insights = "Add your Claude API key in Settings to turn on AI insights."
            return
        }
        viewModelScope.launch {
            busy = true
            insights = withContext(Dispatchers.IO) {
                try {
                    ClaudeAi(key).monthlyInsights(s)
                } catch (e: Exception) {
                    "Couldn't reach Claude: ${e.javaClass.simpleName}. Check your internet connection and API key."
                }
            }
            busy = false
        }
    }

    fun saveApiKey(key: String) {
        prefs.claudeApiKey = key
        message = if (key.isBlank()) "API key removed." else "API key saved."
        if (key.isNotBlank()) aiCategorize()
    }

    override fun onCleared() {
        store?.close()
    }
}
