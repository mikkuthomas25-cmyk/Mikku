package com.fintrack.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fintrack.core.Balances
import com.fintrack.core.CategorySource
import com.fintrack.core.MissingMoney
import com.fintrack.core.Direction
import com.fintrack.core.Flow
import com.fintrack.core.FlowAnalyzer
import com.fintrack.core.ParsedTxn
import com.fintrack.core.StatementCover
import com.fintrack.core.StatementRow
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth

/** On-device SQLite storage. One database file per signed-in account. */
class TxnStore(context: Context, dbName: String) : SQLiteOpenHelper(context, dbName, null, 7) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sms_hash TEXT UNIQUE,
                txn_date TEXT NOT NULL,
                amount REAL NOT NULL,
                direction TEXT NOT NULL,
                merchant TEXT NOT NULL,
                category TEXT NOT NULL,
                account TEXT,
                ref TEXT,
                source TEXT NOT NULL,
                raw TEXT,
                cat_source TEXT NOT NULL DEFAULT 'keyword',
                user_flow TEXT
            )""",
        )
        db.execSQL("CREATE INDEX idx_txn_date ON transactions (txn_date)")
        // The same UPI payment often arrives twice (bank SMS + UPI app SMS).
        db.execSQL("CREATE UNIQUE INDEX idx_txn_ref ON transactions (ref, direction, amount) WHERE ref IS NOT NULL")
        db.execSQL("CREATE TABLE merchant_rules (merchant TEXT PRIMARY KEY, category TEXT NOT NULL)")
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)")
        createV3Tables(db)
    }

    /** The user's answers: "this payee is a shop / a person", and questions already answered. */
    private fun createV3Tables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS payee_flow (payee TEXT PRIMARY KEY, flow TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS review_done (key TEXT PRIMARY KEY)")
        // v4: messages and statement rows the user deleted, so a rescan doesn't bring them back.
        db.execSQL("CREATE TABLE IF NOT EXISTS deleted (hash TEXT PRIMARY KEY)")
        // v5: which account and dates each imported statement covers (the statement is the record there).
        db.execSQL("CREATE TABLE IF NOT EXISTS statement_cover (account TEXT NOT NULL, start TEXT NOT NULL, finish TEXT NOT NULL, PRIMARY KEY (account, start, finish))")
        // v6: each statement's closing balance, for the bank balance on Home.
        db.execSQL("CREATE TABLE IF NOT EXISTS statement_balance (account TEXT NOT NULL, day TEXT NOT NULL, amount REAL NOT NULL, PRIMARY KEY (account, day))")
        // v7: a balance the user entered ("my bank app says ₹16"), the starting point for later payments.
        db.execSQL("CREATE TABLE IF NOT EXISTS user_balance (account TEXT PRIMARY KEY, day TEXT NOT NULL, amount REAL NOT NULL, anchor INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // v2: remember where each category came from, so the learner trains only on trusted labels.
            db.execSQL("ALTER TABLE transactions ADD COLUMN cat_source TEXT NOT NULL DEFAULT 'keyword'")
            db.execSQL("UPDATE transactions SET cat_source='rule' WHERE lower(merchant) IN (SELECT merchant FROM merchant_rules)")
        }
        if (oldVersion < 3) {
            // v3: money-flow types (spending, own transfer, duplicate, …) the user sets, plus their answers.
            db.execSQL("ALTER TABLE transactions ADD COLUMN user_flow TEXT")
            createV3Tables(db)
        }
        if (oldVersion < 7) createV3Tables(db)
    }

    /** Returns the new row id, or null if this transaction is already stored. */
    fun insert(p: ParsedTxn, category: String, catSource: String, source: String, raw: String): Long? {
        if (wasDeleted(p.smsHash)) return null
        val values = ContentValues().apply {
            put("sms_hash", p.smsHash)
            put("txn_date", p.date.toString())
            put("amount", p.amount)
            put("direction", p.direction.name)
            put("merchant", p.merchant)
            put("category", category)
            put("account", p.account)
            put("ref", p.ref)
            put("source", source)
            put("raw", raw)
            put("cat_source", catSource)
        }
        val id = writableDatabase.insertWithOnConflict("transactions", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return id.takeIf { it != -1L }
    }

    /** A payment found from a balance difference (no SMS or notification came); it moves the bank balance. */
    fun addFound(date: LocalDate, amount: Double, direction: Direction, merchant: String, category: String, account: String?, raw: String): Long =
        writableDatabase.insert("transactions", null, ContentValues().apply {
            put("txn_date", date.toString())
            put("amount", amount)
            put("direction", direction.name)
            put("merchant", merchant)
            put("category", category)
            put("account", account)
            put("source", MissingMoney.SOURCE)
            put("raw", raw)
            put("cat_source", CategorySource.USER)
        })

    /** Adds a transaction the user entered; returns its id. */
    fun addManual(date: LocalDate, amount: Double, direction: Direction, merchant: String, category: String, catSource: String): Long =
        writableDatabase.insert("transactions", null, ContentValues().apply {
            put("txn_date", date.toString())
            put("amount", amount)
            put("direction", direction.name)
            put("merchant", merchant)
            put("category", category)
            put("source", "manual")
            put("cat_source", catSource)
        })

    fun month(month: YearMonth): List<Txn> = readableDatabase.rawQuery(
        "SELECT * FROM transactions WHERE txn_date BETWEEN ? AND ? ORDER BY txn_date DESC, id DESC",
        arrayOf(month.atDay(1).toString(), month.atEndOfMonth().toString()),
    ).use { c -> generateSequence { if (c.moveToNext()) c.toTxn() else null }.toList() }

    /** Transactions from [start] to [end] inclusive, newest first. */
    fun range(start: LocalDate, end: LocalDate): List<Txn> = readableDatabase.rawQuery(
        "SELECT * FROM transactions WHERE txn_date BETWEEN ? AND ? ORDER BY txn_date DESC, id DESC",
        arrayOf(start.toString(), end.toString()),
    ).use { c -> generateSequence { if (c.moveToNext()) c.toTxn() else null }.toList() }

    fun rules(): Map<String, String> = readableDatabase.rawQuery("SELECT merchant, category FROM merchant_rules", null)
        .use { c -> buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) } }

    /** Changes one transaction, or (applyToMerchant) remembers the choice for every payment to that merchant. */
    fun setCategory(txn: Txn, category: String, applyToMerchant: Boolean) {
        val db = writableDatabase
        if (applyToMerchant) {
            db.insertWithOnConflict("merchant_rules", null, ContentValues().apply {
                put("merchant", txn.merchant.lowercase())
                put("category", category)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.execSQL(
                "UPDATE transactions SET category=?, cat_source='rule' WHERE lower(merchant)=? AND direction=?",
                arrayOf(category, txn.merchant.lowercase(), txn.direction.name),
            )
        }
        db.execSQL("UPDATE transactions SET category=?, cat_source='user' WHERE id=?", arrayOf(category, txn.id))
    }

    /** Adds a statement row for [account] (last 4 digits); false if it was imported before (its account is filled in if missing). */
    fun insertStatementRow(row: StatementRow, category: String, catSource: String, account: String?): Boolean {
        if (wasDeleted(row.key)) return false
        val values = ContentValues().apply {
            put("account", account)
            put("sms_hash", row.key)
            put("txn_date", row.date.toString())
            put("amount", row.amount)
            put("direction", row.direction.name)
            put("merchant", row.merchant)
            put("category", category)
            put("ref", row.ref)
            put("source", "statement")
            put("raw", row.narration)
            put("cat_source", catSource)
        }
        val added = writableDatabase.insertWithOnConflict("transactions", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
        if (!added && account != null) {
            writableDatabase.execSQL("UPDATE transactions SET account=? WHERE sms_hash=? AND account IS NULL", arrayOf(account, row.key))
        }
        return added
    }

    fun addCover(cover: StatementCover) {
        writableDatabase.insertWithOnConflict("statement_cover", null, ContentValues().apply {
            put("account", cover.account)
            put("start", cover.start.toString())
            put("finish", cover.end.toString())
        }, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun addClosing(closing: Balances.Closing) {
        writableDatabase.insertWithOnConflict("statement_balance", null, ContentValues().apply {
            put("account", closing.account)
            put("day", closing.date.toString())
            put("amount", closing.amount)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun closings(): List<Balances.Closing> = readableDatabase.rawQuery("SELECT account, day, amount FROM statement_balance", null).use { c ->
        buildList { while (c.moveToNext()) add(Balances.Closing(c.getString(0), LocalDate.parse(c.getString(1)), c.getDouble(2))) }
    }

    /** The user's own figure for an account today; transactions stored so far are already in it. */
    fun setUserBalance(accountKey: String, amount: Double) {
        val anchor = readableDatabase.rawQuery("SELECT COALESCE(MAX(id), 0) FROM transactions", null).use { it.moveToFirst(); it.getLong(0) }
        writableDatabase.insertWithOnConflict("user_balance", null, ContentValues().apply {
            put("account", accountKey)
            put("day", LocalDate.now().toString())
            put("amount", amount)
            put("anchor", anchor)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Back to the automatic balance (statement and SMS) for this account. */
    fun clearUserBalance(accountKey: String) {
        writableDatabase.delete("user_balance", "account=?", arrayOf(accountKey))
    }

    fun userBalances(): List<Balances.UserBalance> = readableDatabase.rawQuery("SELECT account, day, amount, anchor FROM user_balance", null).use { c ->
        buildList { while (c.moveToNext()) add(Balances.UserBalance(c.getString(0), LocalDate.parse(c.getString(1)), c.getDouble(2), c.getLong(3))) }
    }

    fun covers(): List<StatementCover> = readableDatabase.rawQuery("SELECT account, start, finish FROM statement_cover", null).use { c ->
        buildList { while (c.moveToNext()) add(StatementCover(c.getString(0), LocalDate.parse(c.getString(1)), LocalDate.parse(c.getString(2)))) }
    }

    /** A statement whose header had no account number: the user said which account it is. */
    fun assignStatementAccount(cover: StatementCover) {
        writableDatabase.execSQL(
            "UPDATE transactions SET account=? WHERE source='statement' AND account IS NULL AND txn_date BETWEEN ? AND ?",
            arrayOf(cover.account, cover.start.toString(), cover.end.toString()),
        )
        addCover(cover)
    }

    /** Replaces a placeholder name from an older parser; names the user typed are left alone. */
    fun repairMerchant(smsHash: String, merchant: String) {
        if (merchant == "Unknown") return
        writableDatabase.execSQL(
            "UPDATE transactions SET merchant=? WHERE sms_hash=? AND merchant IN ('Rs', 'Rs.', 'INR', 'Unknown')",
            arrayOf(merchant, smsHash),
        )
    }

    /** Corrects a transaction the reader got wrong (amount or name). */
    fun updateDetails(id: Long, merchant: String, amount: Double, date: LocalDate, direction: Direction) {
        writableDatabase.execSQL(
            "UPDATE transactions SET merchant=?, amount=?, txn_date=?, direction=? WHERE id=?",
            arrayOf(merchant, amount, date.toString(), direction.name, id),
        )
    }

    /** Flow types the user set on individual transactions. */
    fun userFlows(): Map<Long, Flow> = readableDatabase.rawQuery("SELECT id, user_flow FROM transactions WHERE user_flow IS NOT NULL", null)
        .use { c -> buildMap { while (c.moveToNext()) runCatching { Flow.valueOf(c.getString(1)) }.getOrNull()?.let { put(c.getLong(0), it) } } }

    fun setUserFlow(id: Long, flow: Flow?) {
        writableDatabase.execSQL("UPDATE transactions SET user_flow=? WHERE id=?", arrayOf(flow?.name, id))
    }

    /** The user's per-payee answers, keyed by [FlowAnalyzer.norm] of the payee name. */
    fun payeeFlows(): Map<String, Flow> = readableDatabase.rawQuery("SELECT payee, flow FROM payee_flow", null)
        .use { c -> buildMap { while (c.moveToNext()) runCatching { Flow.valueOf(c.getString(1)) }.getOrNull()?.let { put(c.getString(0), it) } } }

    fun clearPayeeFlow(payee: String) {
        writableDatabase.delete("payee_flow", "payee=?", arrayOf(FlowAnalyzer.norm(payee)))
    }

    fun setPayeeFlow(payee: String, flow: Flow) {
        writableDatabase.insertWithOnConflict("payee_flow", null, ContentValues().apply {
            put("payee", FlowAnalyzer.norm(payee))
            put("flow", flow.name)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun reviewDone(): Set<String> = readableDatabase.rawQuery("SELECT key FROM review_done", null)
        .use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }

    fun markReviewDone(key: String) {
        writableDatabase.insertWithOnConflict("review_done", null, ContentValues().apply { put("key", key) }, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /** Every transaction (training data for the on-device learner). */
    fun all(): List<Txn> = readableDatabase.rawQuery("SELECT * FROM transactions", null)
        .use { c -> generateSequence { if (c.moveToNext()) c.toTxn() else null }.toList() }

    /** Runs [block] in one database transaction (much faster for many small updates). */
    fun <T> transaction(block: () -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        try {
            return block().also { db.setTransactionSuccessful() }
        } finally {
            db.endTransaction()
        }
    }

    /** Records the learner's verdict for a row it is allowed to change. */
    fun setLearned(id: Long, category: String, catSource: String) {
        writableDatabase.execSQL("UPDATE transactions SET category=?, cat_source=? WHERE id=?", arrayOf(category, catSource, id))
    }

    /** Deletes a transaction for good: its SMS or statement row is remembered and never re-added. */
    fun delete(id: Long) {
        val db = writableDatabase
        db.execSQL("INSERT OR IGNORE INTO deleted (hash) SELECT sms_hash FROM transactions WHERE id=? AND sms_hash IS NOT NULL", arrayOf(id))
        db.delete("transactions", "id=?", arrayOf(id.toString()))
    }

    private fun wasDeleted(hash: String) =
        readableDatabase.rawQuery("SELECT 1 FROM deleted WHERE hash=?", arrayOf(hash)).use { it.moveToFirst() }

    fun getMeta(key: String): String? = readableDatabase.rawQuery("SELECT value FROM meta WHERE key=?", arrayOf(key))
        .use { c -> if (c.moveToFirst()) c.getString(0) else null }

    fun setMeta(key: String, value: String) {
        writableDatabase.insertWithOnConflict("meta", null, ContentValues().apply {
            put("key", key)
            put("value", value)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun Cursor.toTxn() = Txn(
        id = getLong(getColumnIndexOrThrow("id")),
        date = LocalDate.parse(getString(getColumnIndexOrThrow("txn_date"))),
        amount = getDouble(getColumnIndexOrThrow("amount")),
        direction = Direction.valueOf(getString(getColumnIndexOrThrow("direction"))),
        merchant = getString(getColumnIndexOrThrow("merchant")),
        category = getString(getColumnIndexOrThrow("category")),
        account = getString(getColumnIndexOrThrow("account")),
        source = getString(getColumnIndexOrThrow("source")),
        raw = getString(getColumnIndexOrThrow("raw")),
        categorySource = getString(getColumnIndexOrThrow("cat_source")),
        ref = getString(getColumnIndexOrThrow("ref")),
    )
}
