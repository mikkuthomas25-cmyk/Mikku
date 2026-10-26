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
import com.fintrack.core.CategoryLearner
import com.fintrack.core.CategorySource
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

/** [taught]: categories the user set; [autoSorted]: transactions the model categorized; [accuracy]: on the user's own corrections, when there are enough. */
data class LearningStats(val taught: Int, val autoSorted: Int, val accuracy: Double?)

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
    /** What the on-device learner knows; shown on the Insights screen. */
    var learning by mutableStateOf<LearningStats?>(null); private set
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
        learning = null
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
            val (added, stats) = withContext(Dispatchers.IO) { SmsImporter.scanInbox(app, s, full) to relearn(s) }
            learning = stats
            if (full) message = "Found $added transactions in your messages."
            refresh()
            busy = false
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
            val (added, stats) = withContext(Dispatchers.IO) {
                SmsParser.parse(text)?.takeIf { SmsImporter.ingest(s, text, System.currentTimeMillis(), "shared") } to relearn(s)
            }
            learning = stats
            message = when {
                added != null -> "Added ${rupees(added.amount)} · ${added.merchant}"
                SmsParser.parse(text) != null -> "Already added."
                else -> "That doesn't look like a bank transaction message."
            }
            refresh()
        }
    }

    fun addManual(date: LocalDate, amount: Double, direction: Direction, merchant: String) = io {
        val (category, source) = Categorizer.categorizeWithSource(merchant.trim(), direction, it.rules())
        it.addManual(date, amount, direction, merchant.trim(), category, source)
    }

    /** Runs a change, lets the model learn from it, then redraws. */
    private fun io(block: (TxnStore) -> Unit) {
        val s = store ?: return
        viewModelScope.launch {
            learning = withContext(Dispatchers.IO) {
                block(s)
                relearn(s)
            }
            refresh()
        }
    }

    // ------------------------------------------------------------------ on-device learning

    /**
     * Retrains the on-device model on the user's history and re-sorts the rows it may change
     * (its own earlier guesses and "Other"/default fallbacks). The user's choices are never touched.
     * Runs on a background thread; takes milliseconds for a few thousand transactions.
     */
    private fun relearn(s: TxnStore): LearningStats {
        val all = s.all()
        val learner = CategoryLearner(all)
        s.transaction {
            for (t in all) {
                if (!CategoryLearner.isLearnable(t)) continue
                val guess = learner.suggest(t)
                val category = guess?.category ?: Categorizer.keywordCategory(t.merchant, t.direction)
                val source = if (guess != null) CategorySource.LEARNED else CategorySource.KEYWORD
                if (category != t.category || source != t.categorySource) s.setLearned(t.id, category, source)
            }
        }
        val after = s.all()
        return LearningStats(
            taught = learner.userTaught,
            autoSorted = after.count { it.categorySource == CategorySource.LEARNED },
            accuracy = CategoryLearner.estimateAccuracy(after),
        )
    }

    override fun onCleared() {
        store?.close()
    }
}
