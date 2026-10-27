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
import com.fintrack.core.Balances
import com.fintrack.core.Accounts
import com.fintrack.core.Diagnostics
import com.fintrack.core.Direction
import com.fintrack.core.Flow
import com.fintrack.core.FlowAnalyzer
import com.fintrack.core.FlowDecision
import com.fintrack.core.Reconciliation
import com.fintrack.core.Onboarding
import com.fintrack.core.PayeeMemory
import com.fintrack.core.ReviewItem
import com.fintrack.core.Salary
import com.fintrack.core.MissedSms
import com.fintrack.core.MonthSummary
import com.fintrack.core.SmsParser
import com.fintrack.core.StatementCover
import com.fintrack.core.StatementLedger
import com.fintrack.core.StatementResult
import com.fintrack.core.Summaries
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** First-run setup, statement first: import a month, sort its spending, maybe add a month, then SMS. */
enum class SetupStep { Statement, Sort, More, Sms, Done }

enum class Kind(val label: String) { All("All"), Spent("Money out"), Received("Money in") }

/** Result of importing one statement file; [result] is null when the file couldn't be read. */
data class ImportReport(
    val fileName: String,
    val result: StatementResult?,
    val added: Int,
    val alreadyKnown: Int,
    val error: String?,
    /** Last 4 digits of the statement's account: from its header, or chosen by the user. */
    val account: String? = result?.accountLast4,
)

data class PasswordAsk(val fileName: String, val wrongPassword: Boolean)

/** [taught]: categories the user set; [autoSorted]: transactions the model categorized; [accuracy]: on the user's own corrections, when there are enough. */
data class LearningStats(val taught: Int, val autoSorted: Int, val accuracy: Double?)

sealed interface TxnFilter {
    data class Day(val date: LocalDate) : TxnFilter
    data class Category(val name: String) : TxnFilter
    data class Merchant(val name: String) : TxnFilter
    data class OfFlow(val flow: Flow) : TxnFilter
    /** Spends FinTrack categorized by itself (learned from the user, or the built-in list). */
    data object Auto : TxnFilter
    /** Spends still in "Other". */
    data object Unsorted : TxnFilter
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
    /** Accounts found in the messages, and the ones the user chose not to track (e.g. a friend's). */
    var accounts by mutableStateOf<List<Accounts.Info>>(emptyList()); private set
    var untracked by mutableStateOf<Set<String>>(emptySet()); private set
    /** False until the user has said which accounts to track (asked once there are two or more). */
    var accountsChosen by mutableStateOf(true); private set
    /** Latest known balance of each tracked account (statement closing or SMS "Avl Bal"). */
    var balances by mutableStateOf<List<Balances.Balance>>(emptyList()); private set
    /** For each imported statement with rows this month: its totals and how each rupee was counted. */
    var tallies by mutableStateOf<List<StatementLedger.Tally>>(emptyList()); private set
    /** Where the user is in first-run setup; Done once finished or skipped. */
    var setup by mutableStateOf(SetupStep.Done); private set
    /** Setup's plan for the statement: sorted from brands, small payees asked together, the rest one by one. */
    var plan by mutableStateOf<Onboarding.Plan?>(null); private set
    /** Payees asked one by one, and which one is showing. */
    var toSort by mutableStateOf<List<Onboarding.Payee>>(emptyList()); private set
    var sortIndex by mutableStateOf(0); private set
    /** Whether the small-payees question has been answered. */
    var smallDecided by mutableStateOf(false); private set
    /** Money covered by the user's answers so far, and whether the small payees went to daily spends. */
    private var answeredTotal by mutableStateOf(0.0)
    private var smallAccepted by mutableStateOf(false)

    /** Share of the statement's spending that's sorted (from brands, small spends, and answers). */
    val sortedShare: Double
        get() {
            val p = plan ?: return 0.0
            if (p.total <= 0) return 1.0
            return (p.auto.sumOf { it.total } + (if (smallAccepted) p.small.sumOf { it.total } else 0.0) + answeredTotal) / p.total
        }
    /** How many months of statements have been imported. */
    var statementMonths by mutableStateOf(0); private set
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
                    TxnFilter.Auto -> t.effectiveFlow == Flow.SPEND && sortedByItself(t)
                    TxnFilter.Unsorted -> t.effectiveFlow == Flow.SPEND && t.category == "Other"
                }
                val byKind = when (kind) {
                    Kind.All -> true
                    Kind.Spent -> t.outValue > 0
                    Kind.Received -> t.effectiveFlow.countsAsMoneyIn
                }
                byFilter && byKind && (q.isEmpty() || q in t.merchant.lowercase() || q in t.category.lowercase())
            }
        }

    /** Categorized without the user: learned from their choices, the built-in brand list, or small daily spends. */
    fun sortedByItself(t: Txn) = t.category != "Other" &&
        (CategorySource.isAutomatic(t.categorySource) || t.categorySource == CategorySource.KEYWORD)

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
        if (acc != null) adoptLocalData(dbName)
        store = TxnStore(app, dbName)
        prefs.activeDb = dbName
        prefs.activeUserName = acc?.name ?: "there"
        prefs.activeUserEmail = acc?.email ?: "This device only"
        account = prefs.activeUserName
        accountEmail = prefs.activeUserEmail ?: ""
        setup = if (store?.getMeta(SETUP_DONE) == null) SetupStep.Statement else SetupStep.Done
        onResume()
        pendingShared?.let { pendingShared = null; addShared(it) }
    }

    /**
     * First sign-in on a phone that was used without an account: the transactions, rules and
     * answers already here become the account's, instead of starting empty.
     */
    private fun adoptLocalData(dbName: String) {
        val target = app.getDatabasePath(dbName)
        val local = app.getDatabasePath(Prefs.LOCAL_DB)
        if (target.exists() || !local.exists()) return
        for (suffix in listOf("", "-journal", "-wal", "-shm")) {
            val from = java.io.File(local.path + suffix)
            if (from.exists()) from.renameTo(java.io.File(target.path + suffix))
        }
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
        // During setup the statement comes first; messages are read once it's done.
        if (hasSmsPermission && setup == SetupStep.Done) scan(full = false) else refresh()
    }

    // ------------------------------------------------------------------ first-run setup

    /** After a statement is imported: list its payees to sort, biggest first. */
    private fun startSorting() {
        val s = store ?: return
        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) { s.all() }
            val p = Onboarding.plan(all)
            plan = p
            toSort = p.ask
            smallDecided = p.small.isEmpty()
            smallAccepted = false
            answeredTotal = 0.0
            statementMonths = Onboarding.statementMonths(all).size
            sortIndex = 0
            setup = if (p.ask.isEmpty() && p.small.isEmpty()) SetupStep.More else SetupStep.Sort
        }
    }

    /** The user's answer for the payee on screen (null = skip it); every payment to them is sorted. */
    fun sortAnswer(choice: Choice?) {
        val payee = toSort.getOrNull(sortIndex) ?: return
        if (choice != null) {
            payee.txns.distinctBy { it.merchant }.forEach { classify(it, choice, quiet = true) }
            answeredTotal += payee.total
        }
        sortIndex++
        if (sortIndex >= toSort.size && smallDecided) setup = SetupStep.More
    }

    /** "These small payments are daily spends": one answer for all of them; or ask about them one by one. */
    fun answerSmall(dailySpends: Boolean) {
        val small = plan?.small.orEmpty()
        if (dailySpends) {
            small.forEach { p -> p.txns.distinctBy { it.merchant }.forEach { classify(it, Choice.Category(Categorizer.SMALL), quiet = true) } }
        } else {
            toSort = toSort + small
        }
        smallDecided = true
        smallAccepted = dailySpends
        if (dailySpends && sortIndex >= toSort.size) setup = SetupStep.More
    }

    fun finishSorting() { setup = SetupStep.More }

    fun toSmsStep() { setup = SetupStep.Sms }

    /** Setup is over (finished or skipped): from now on messages are read as usual. */
    fun finishSetup() {
        store?.setMeta(SETUP_DONE, "1")
        setup = SetupStep.Done
        onResume()
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
            var found = emptyList<Accounts.Info>()
            var skip = emptySet<String>()
            var chosen = true
            var latestBalances = emptyList<Balances.Balance>()
            val (all, analysis) = withContext(Dispatchers.IO) {
                // A few days past month-end so a transfer or duplicate straddling it is still paired.
                val everything = s.range(m.minusMonths(HISTORY_MONTHS).atDay(1), m.atEndOfMonth().plusDays(3))
                skip = s.getMeta(UNTRACKED)?.split(",")?.filter { it.isNotBlank() }?.toSet().orEmpty()
                found = Accounts.list(everything)
                chosen = s.getMeta(ACCOUNTS_CHOSEN) != null || found.size < 2
                // Where a statement was imported, it's the record for its account and dates; then only
                // the accounts the user tracks (a friend's account on this phone isn't their money).
                val resolver = Accounts.Resolver(everything)
                // Bank balances are "now", whatever month is shown: from recent messages and statements.
                val recent = s.range(LocalDate.now().minusDays(120), LocalDate.now())
                latestBalances = Balances.latest(recent, s.closings()).filter { it.accountKey !in skip }
                val raw = Accounts.tracked(StatementLedger.apply(everything, s.covers(), resolver), skip, resolver)
                val a = FlowAnalyzer.analyze(raw, s.userFlows(), s.payeeFlows(), s.reviewDone())
                raw.map { t ->
                    val d = a.decisions[t.id]
                    t.copy(flow = d?.flow ?: Flow.basic(t), category = d?.category ?: t.category)
                }.filter { it.date <= m.atEndOfMonth() } to a
            }
            accounts = found
            balances = latestBalances
            untracked = skip
            accountsChosen = chosen
            tallies = all.filter { YearMonth.from(it.date) == m && it.source == "statement" }
                .mapNotNull { it.account }.distinct()
                .filter { "acct:$it" !in skip }
                .mapNotNull { acct -> StatementLedger.tally(all.filter { YearMonth.from(it.date) == m }, acct) }
            // Late-month salary counts toward the month it pays for (see Salary).
            val salary = Salary.ids(all)
            val current = all.filter { Salary.monthOf(it, salary) == m }
            history = all
            txns = current
            flowDecisions = analysis.decisions
            review = analysis.review
            accountChecks = Reconciliation.byAccount(all)
            summary = Summaries.build(m, current, all.filter { Salary.monthOf(it, salary) == m.minusMonths(1) })
        }
    }

    /** Saves which accounts to leave out; everything is recalculated from the tracked ones. */
    fun chooseAccounts(skip: Set<String>) {
        val s = store ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                s.setMeta(UNTRACKED, skip.joinToString(","))
                s.setMeta(ACCOUNTS_CHOSEN, "1")
            }
            message = if (skip.isEmpty()) "Tracking all accounts" else "Left out ${skip.size} account${if (skip.size == 1) "" else "s"}"
            refresh()
        }
    }

    fun changeMonth(delta: Long) = pickMonth(month.plusMonths(delta))

    fun pickMonth(m: YearMonth) {
        month = m
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

    /** A cash (or any missed) transaction, with what it is: shows up in its category like any other. */
    fun addManual(date: LocalDate, amount: Double, direction: Direction, merchant: String, choice: Choice) = io {
        val category = when (choice) {
            is Choice.Category -> choice.name
            is Choice.Kind -> when (choice.flow) {
                Flow.INVESTED -> "Investments"
                Flow.TO_PEOPLE, Flow.FROM_PEOPLE -> "Transfers"
                Flow.OWN_TRANSFER -> "Self Transfer"
                else -> if (direction == Direction.CREDIT) "Income" else "Other"
            }
        }
        val id = it.addManual(date, amount, direction, merchant.trim(), category, CategorySource.USER)
        it.setUserFlow(id, (choice as? Choice.Kind)?.flow ?: Flow.SPEND)
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

    /** Fixes an amount or name the reader got wrong. */
    fun editDetails(t: Txn, merchant: String, amount: Double, date: LocalDate, direction: Direction) {
        io {
            it.updateDetails(t.id, merchant.trim().ifBlank { t.merchant }, amount, date, direction)
            // "Rent" or "Salary" no longer fits once money in becomes money out (or back): decide again.
            if (direction != t.direction) it.setUserFlow(t.id, null)
        }
        message = "Saved"
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

    /** Why an automatic category was chosen ("You put Manoop V U in Rent"), for the picker. */
    fun categoryReason(t: Txn): String? =
        if (t.categorySource == CategorySource.MEMORY) PayeeMemory(history).match(t)?.reason else null

    /**
     * The answer to "What is this?". A category makes it spending in that category; a kind (a person,
     * my own account, investment, don't count…) says what money it is. Either way it's remembered for
     * the payee, so the next payment to them is sorted the same, and both models learn from it.
     */
    fun classify(t: Txn, choice: Choice, quiet: Boolean = false) {
        val payee = t.merchant != "Unknown"
        io { s ->
            when (choice) {
                is Choice.Category -> {
                    s.setUserFlow(t.id, Flow.SPEND)
                    if (payee) s.clearPayeeFlow(t.merchant)
                    s.setCategory(t, choice.name, applyToMerchant = payee)
                }
                is Choice.Kind -> {
                    s.setUserFlow(t.id, choice.flow)
                    if (payee && choice.flow in PAYEE_FLOWS) s.setPayeeFlow(t.merchant, choice.flow)
                    if (choice.flow == Flow.INVESTED) s.setCategory(t, "Investments", applyToMerchant = payee)
                }
            }
        }
        if (!quiet) message = "${t.merchant} → ${choice.label}" + if (payee) " (and from now on)" else ""
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

    /** The user said which account a statement is for (its header didn't say). */
    fun assignStatement(report: ImportReport, accountKey: String) {
        val s = store ?: return
        val result = report.result ?: return
        val start = result.start ?: return
        val end = result.end ?: return
        val last4 = accountKey.removePrefix("acct:")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                s.assignStatementAccount(StatementCover(last4, start, end))
                result.check.closing?.let { s.addClosing(Balances.Closing(last4, end, it)) }
            }
            importReports = importReports.map { if (it === report) it.copy(account = last4) else it }
            refresh()
        }
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
            // Show the statement's month, so its numbers are on screen straight away.
            val latest = importReports.mapNotNull { it.result?.end }.maxOrNull()
            if (latest != null && YearMonth.from(latest) != month) {
                pickMonth(YearMonth.from(latest))
                message = "Showing ${YearMonth.from(latest).label()}, the month of your statement"
            } else {
                refresh()
            }
            importing = false
            if ((setup == SetupStep.Statement || setup == SetupStep.More) && importReports.any { it.result?.empty == false }) startSorting()
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
        val memory = PayeeMemory(all)
        s.transaction {
            for (t in all) {
                if (t.categorySource == CategorySource.USER || t.categorySource == CategorySource.RULE) continue
                // What the user taught about this payee (or this monthly payment) comes first,
                // then the built-in brand list, then the model, then the fallback (Other / Income).
                val remembered = memory.match(t)
                val keyword = Categorizer.keywordCategory(t.merchant, t.direction, Categorizer.contextOf(t))
                val known = keyword != "Other" && keyword != "Income"
                if (remembered == null && !CategoryLearner.isLearnable(t)) continue
                val guess = if (remembered != null || known) null else learner.suggest(t)
                val (category, source) = when {
                    remembered != null -> remembered.category to CategorySource.MEMORY
                    guess != null -> guess.category to CategorySource.LEARNED
                    else -> Categorizer.withSmallSpends(keyword, t.direction, t.amount) to CategorySource.KEYWORD
                }
                if (category != t.category || source != t.categorySource) s.setLearned(t.id, category, source)
            }
        }
        val after = s.all()
        return LearningStats(
            taught = learner.userTaught,
            autoSorted = after.count { CategorySource.isAutomatic(it.categorySource) },
            accuracy = CategoryLearner.estimateAccuracy(after),
        )
    }

    companion object {
        const val HISTORY_MONTHS = 6L
        private const val UNTRACKED = "untracked_accounts"
        private const val ACCOUNTS_CHOSEN = "accounts_chosen"
        private const val SETUP_DONE = "setup_done"
        /** Flows that make sense as a standing answer for a payee. */
        val PAYEE_FLOWS = setOf(Flow.SPEND, Flow.TO_PEOPLE, Flow.FROM_PEOPLE, Flow.INVESTED, Flow.INCOME, Flow.OWN_TRANSFER)
    }

    override fun onCleared() {
        store?.close()
    }
}
