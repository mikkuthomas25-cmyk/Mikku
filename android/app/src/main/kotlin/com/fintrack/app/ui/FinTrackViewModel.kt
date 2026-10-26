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
import com.fintrack.core.SmsParser
import com.fintrack.core.ClaudeAi
import com.fintrack.core.Direction
import com.fintrack.core.MissedSms
import com.fintrack.core.MonthSummary
import com.fintrack.core.Summaries
import com.fintrack.core.Txn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

enum class Kind { All, Spent, Received }

sealed interface TxnFilter {
    data class Day(val date: LocalDate) : TxnFilter
    data class Category(val name: String) : TxnFilter
    data class Merchant(val name: String) : TxnFilter
}

class FinTrackViewModel(private val app: Application) : AndroidViewModel(app) {
    val auth = AuthManager(app)
    val prefs = Prefs(app)
    private var store: TxnStore? = null

    /** Text shared into FinTrack before sign-in; added once a session opens. */
    private var pendingShared: String? = null

    /** Display name of the signed-in user; null means signed out. */
    var account by mutableStateOf<String?>(null); private set
    var accountEmail by mutableStateOf(""); private set
    var month by mutableStateOf(YearMonth.now()); private set
    var summary by mutableStateOf<MonthSummary?>(null); private set
    var txns by mutableStateOf<List<Txn>>(emptyList()); private set
    var filter by mutableStateOf<TxnFilter?>(null)
    var query by mutableStateOf("")
    var kind by mutableStateOf(Kind.All)
    var hasSmsPermission by mutableStateOf(SmsImporter.hasPermission(app)); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)
    var insights by mutableStateOf<String?>(null); private set
    /** Bank-looking SMS the parser couldn't read; null until loaded. */
    var missed by mutableStateOf<List<MissedSms>?>(null); private set

    val filteredTxns: List<Txn>
        get() {
            val q = query.trim().lowercase()
            return txns.filter { t ->
                val byFilter = when (val f = filter) {
                    null -> true
                    is TxnFilter.Day -> t.date == f.date
                    is TxnFilter.Category -> t.category == f.name && t.direction == Direction.DEBIT
                    is TxnFilter.Merchant -> t.merchant == f.name && t.direction == Direction.DEBIT
                }
                val byKind = when (kind) {
                    Kind.All -> true
                    Kind.Spent -> t.direction == Direction.DEBIT
                    Kind.Received -> t.direction == Direction.CREDIT
                }
                byFilter && byKind && (q.isEmpty() || q in t.merchant.lowercase() || q in t.category.lowercase())
            }
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
        prefs.activeUserName = acc?.name ?: "there"
        prefs.activeUserEmail = acc?.email ?: "This device only"
        account = prefs.activeUserName
        accountEmail = prefs.activeUserEmail ?: ""
        onResume()
        pendingShared?.let { pendingShared = null; addShared(it) }
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
        query = ""
        insights = null
        refresh()
    }

    fun setCategory(txn: Txn, category: String, applyToMerchant: Boolean) = io {
        it.setCategory(txn, category, applyToMerchant)
    }

    fun delete(txn: Txn) = io { it.delete(txn.id) }

    fun loadMissed() {
        missed = null
        viewModelScope.launch {
            missed = withContext(Dispatchers.IO) { SmsImporter.findMissed(app) }
        }
    }

    /**
     * Adds a bank SMS the user shared into the app (Messages > Share > FinTrack).
     * Works without SMS permission, so the app stays useful if the phone blocks it.
     */
    fun addShared(text: String) {
        val s = store ?: run { pendingShared = text; return }
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                SmsParser.parse(text)?.takeIf { SmsImporter.ingest(s, text, System.currentTimeMillis(), "shared") }
            }
            message = when {
                added != null -> "Added ${rupees(added.amount)} · ${added.merchant}"
                SmsParser.parse(text) != null -> "Already added."
                else -> "That doesn't look like a bank transaction message."
            }
            refresh()
        }
    }

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
