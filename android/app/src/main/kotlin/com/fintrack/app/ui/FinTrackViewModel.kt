package com.fintrack.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.Activity
import android.app.Application
import android.net.Uri
import com.fintrack.app.auth.Account
import com.fintrack.app.auth.AuthManager
import com.fintrack.app.data.Prefs
import com.fintrack.app.data.TxnStore
import com.fintrack.app.sms.SmsImporter
import com.fintrack.app.statement.PasswordRequired
import com.fintrack.app.statement.StatementImporter
import com.fintrack.core.Categorizer
import com.fintrack.core.CategoryLearner
import com.fintrack.core.CategorySource
import com.fintrack.core.AccountCheck
import com.fintrack.core.Diagnostics
import com.fintrack.core.Direction
import com.fintrack.core.Flow
import com.fintrack.core.FlowAnalyzer
import com.fintrack.core.FlowDecision
import com.fintrack.core.Reconciliation
import com.fintrack.core.ReviewItem
import com.fintrack.core.MissedSms
import com.fintrack.core.MonthSummary
import com.fintrack.core.SmsParser
import com.fintrack.core.StatementResult
import com.fintrack.core.Summaries
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Kind { All, Spent, Received }

/** Result of importing one statement file; [result] is null when the file couldn't be read. */
data class ImportReport(val fileName: String, val result: StatementResult?, val added: Int, val alreadyKnown: Int, val error: String?)

data class PasswordAsk(val fileName: String, val wrongPassword: Boolean)

/** [taught]: categories the user set; [autoSorted]: transactions the model categorized; [accuracy]: on the user's own corrections, when there are enough. */
data class LearningStats(val taught: Int, val autoSorted: Int, val accuracy: Double?)

sealed interface TxnFilter {
    data class Day(val date: LocalDate) : TxnFilter
    data class Category(val name: String) : TxnFilter
    data class Merchant(val name: String) : TxnFilter
    data class OfFlow(val flow: Flow) : TxnFilter
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
    /** The selected month plus the 6 before it: what the charts and pattern learning look at. */
    var history by mutableStateOf<List<Txn>>(emptyList()); private set
    var filter by mutableStateOf<TxnFilter?>(null)
    var query by mutableStateOf("")
    var kind by mutableStateOf(Kind.All)
    var hasSmsPermission by mutableStateOf(SmsImporter.hasPermission(app)); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)
    /** Set together with [message] when the change can be undone from the snackbar. */
    var undo by mutableStateOf<(() -> Unit)?>(null)
    /** Why each transaction has its flow (shown when you open it). */
    var flowDecisions by mutableStateOf<Map<Long, FlowDecision>>(emptyMap()); private set
    /** Questions for the user ("Shop or person?", "Same payment twice?"). */
    var review by mutableStateOf<List<ReviewItem>>(emptyList()); private set
    /** Per-account check that captured transactions explain how the bank balance moved. */
    var accountChecks by mutableStateOf<List<AccountCheck>>(emptyList()); private set
    /** What the on-device learner knows; shown on the Insights screen. */
    var learning by mutableStateOf<LearningStats?>(null); private set
    /** Bank statement import: one report per file, and the file currently waiting for a password. */
    var importReports by mutableStateOf<List<ImportReport>>(emptyList()); private set
    var importing by mutableStateOf(false); private set
    var passwordFor by mutableStateOf<PasswordAsk?>(null); private set
    /** Bank-looking SMS the parser couldn't read; null until loaded. */
    var missed by mutableStateOf<List<MissedSms>?>(null); private set

    val filteredTxns: List<Txn>
        get() {
            val q = query.trim().lowercase()
            return txns.filter { t ->
                val byFilter = when (val f = filter) {
                    null -> true
                    is TxnFilter.Day -> t.date == f.date
                    is TxnFilter.Category -> t.category == f.name && t.spendValue != 0.0
                    is TxnFilter.Merchant -> t.merchant == f.name && t.effectiveFlow == Flow.SPEND
                    is TxnFilter.OfFlow -> t.effectiveFlow == f.flow
                }
                val byKind = when (kind) {
                    Kind.All -> true
                    Kind.Spent -> t.effectiveFlow == Flow.SPEND
                    Kind.Received -> t.effectiveFlow.countsAsMoneyIn
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
            val (all, analysis) = withContext(Dispatchers.IO) {
                // A few days past month-end so a transfer or duplicate straddling it is still paired.
                val raw = s.range(m.minusMonths(HISTORY_MONTHS).atDay(1), m.atEndOfMonth().plusDays(3))
                val a = FlowAnalyzer.analyze(raw, s.userFlows(), s.payeeFlows(), s.reviewDone())
                raw.map { t ->
                    val d = a.decisions[t.id]
                    t.copy(flow = d?.flow ?: Flow.basic(t), category = d?.category ?: t.category)
                }.filter { it.date <= m.atEndOfMonth() } to a
            }
            val current = all.filter { YearMonth.from(it.date) == m }
            history = all
            txns = current
            flowDecisions = analysis.decisions
            review = analysis.review
            accountChecks = Reconciliation.byAccount(all)
            summary = Summaries.build(m, current, all.filter { YearMonth.from(it.date) == m.minusMonths(1) })
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

    // ------------------------------------------------------------------ money flows

    /** Sets what a transaction is (spending, own transfer, duplicate, …); null returns it to automatic. */
    fun setFlow(txn: Txn, flow: Flow?, forPayee: Boolean) = io {
        it.setUserFlow(txn.id, flow)
        if (forPayee && flow != null && flow in PAYEE_FLOWS) it.setPayeeFlow(txn.merchant, flow)
    }

    /**
     * The one-tap fix: take a transaction out of the totals, or put it back. Remembers what it was,
     * so the snackbar can undo it.
     */
    fun toggleCounted(t: Txn) {
        val s = store ?: return
        val next = if (t.effectiveFlow.counted) Flow.IGNORED else if (t.direction == Direction.DEBIT) Flow.SPEND else Flow.INCOME
        viewModelScope.launch {
            val previous = withContext(Dispatchers.IO) { s.userFlows()[t.id].also { s.setUserFlow(t.id, next) } }
            refresh()
            message = if (next == Flow.IGNORED) "Not counted: ${t.merchant} ${rupees(t.amount)}" else "Counted again: ${t.merchant}"
            undo = { io { it.setUserFlow(t.id, previous) } }
        }
    }

    /** Fixes an amount or name the reader got wrong. */
    fun editDetails(t: Txn, merchant: String, amount: Double) = io {
        it.updateDetails(t.id, merchant.trim().ifBlank { t.merchant }, amount)
    }

    /** [flow]: SPEND (a shop), TO_PEOPLE (a person) or OWN_TRANSFER (it's me, another of my accounts). */
    fun answerShopOrPerson(item: ReviewItem.ShopOrPerson, flow: Flow) = io {
        it.setPayeeFlow(item.payee, flow)
        it.markReviewDone(item.key)
    }

    /** "Moved between your accounts?" Yes takes both halves out of money in and spending. */
    fun answerOwnTransfer(item: ReviewItem.PossibleOwnTransfer, isOwn: Boolean) = io {
        if (isOwn) {
            it.setUserFlow(item.out.id, Flow.OWN_TRANSFER)
            it.setUserFlow(item.into.id, Flow.OWN_TRANSFER)
        }
        it.markReviewDone(item.key)
    }

    /** The one-tap category fix from a list row: applies to this payee from now on, and teaches the model. */
    fun quickCategory(txn: Txn, category: String) {
        setCategory(txn, category, applyToMerchant = txn.merchant != "Unknown")
        message = "${txn.merchant} → $category" + if (txn.merchant != "Unknown") " (and from now on)" else ""
    }

    /** The month's transactions with how each was treated, as text to share for a check (numbers hidden). */
    fun checkReport(): String = Diagnostics.report(month, txns, flowDecisions, summary)

    fun answerDuplicate(item: ReviewItem.PossibleDuplicate, isDuplicate: Boolean) = io {
        if (isDuplicate) it.setUserFlow(item.duplicate.id, Flow.DUPLICATE)
        it.markReviewDone(item.key)
    }

    // ------------------------------------------------------------------ bank statements

    private val importQueue = ArrayDeque<Uri>()

    /** Imports one or more statements (PDF or CSV), asking for passwords as needed. */
    fun importStatements(uris: List<Uri>) {
        if (uris.isEmpty()) return
        importQueue.clear()
        importQueue.addAll(uris)
        importReports = emptyList()
        processImports(password = null)
    }

    fun submitPassword(password: String) {
        passwordFor = null
        processImports(password)
    }

    /** Skips the protected file and carries on with the rest. */
    fun skipPassword() {
        val uri = importQueue.removeFirstOrNull() ?: return
        importReports = importReports + ImportReport(StatementImporter.displayName(app, uri), null, 0, 0, "Skipped: password needed")
        passwordFor = null
        processImports(password = null)
    }

    private fun processImports(password: String?) {
        val s = store ?: return
        viewModelScope.launch {
            importing = true
            var pw = password
            while (importQueue.isNotEmpty()) {
                val uri = importQueue.first()
                val step = withContext(Dispatchers.IO) {
                    try {
                        val o = StatementImporter.import(app, s, uri, pw)
                        ImportReport(o.fileName, o.result, o.added, o.alreadyKnown, null)
                    } catch (e: PasswordRequired) {
                        e
                    } catch (e: Exception) {
                        ImportReport(StatementImporter.displayName(app, uri), null, 0, 0, "Couldn't read this file (${e.javaClass.simpleName}).")
                    }
                }
                if (step is PasswordRequired) {
                    passwordFor = PasswordAsk(StatementImporter.displayName(app, uri), step.wrongPassword)
                    importing = false
                    return@launch
                }
                importReports = importReports + (step as ImportReport)
                importQueue.removeFirst()
                pw = null
            }
            learning = withContext(Dispatchers.IO) { relearn(s) }
            refresh()
            importing = false
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
                // The built-in brand list first, then the model, then the fallback (Other / Income).
                val keyword = Categorizer.keywordCategory(t.merchant, t.direction, Categorizer.contextOf(t))
                val known = keyword != "Other" && keyword != "Income"
                val guess = if (known) null else learner.suggest(t)
                val category = guess?.category ?: keyword
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

    companion object {
        const val HISTORY_MONTHS = 6L
        /** Flows that make sense as a standing answer for a payee. */
        val PAYEE_FLOWS = setOf(Flow.SPEND, Flow.TO_PEOPLE, Flow.FROM_PEOPLE, Flow.INVESTED, Flow.INCOME, Flow.OWN_TRANSFER)
    }

    override fun onCleared() {
        store?.close()
    }
}
