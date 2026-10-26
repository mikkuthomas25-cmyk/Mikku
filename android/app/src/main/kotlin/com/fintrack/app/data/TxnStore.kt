package com.fintrack.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fintrack.core.Direction
import com.fintrack.core.ParsedTxn
import com.fintrack.core.StatementRow
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth

/** On-device SQLite storage. One database file per signed-in account. */
class TxnStore(context: Context, dbName: String) : SQLiteOpenHelper(context, dbName, null, 2) {

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
                cat_source TEXT NOT NULL DEFAULT 'keyword'
            )""",
        )
        db.execSQL("CREATE INDEX idx_txn_date ON transactions (txn_date)")
        // The same UPI payment often arrives twice (bank SMS + UPI app SMS).
        db.execSQL("CREATE UNIQUE INDEX idx_txn_ref ON transactions (ref, direction, amount) WHERE ref IS NOT NULL")
        db.execSQL("CREATE TABLE merchant_rules (merchant TEXT PRIMARY KEY, category TEXT NOT NULL)")
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // v2: remember where each category came from, so the learner trains only on trusted labels.
            db.execSQL("ALTER TABLE transactions ADD COLUMN cat_source TEXT NOT NULL DEFAULT 'keyword'")
            db.execSQL("UPDATE transactions SET cat_source='rule' WHERE lower(merchant) IN (SELECT merchant FROM merchant_rules)")
        }
    }

    /** Returns the new row id, or null if this transaction is already stored. */
    fun insert(p: ParsedTxn, category: String, catSource: String, source: String, raw: String): Long? {
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

    fun addManual(date: LocalDate, amount: Double, direction: Direction, merchant: String, category: String, catSource: String) {
        writableDatabase.insert("transactions", null, ContentValues().apply {
            put("txn_date", date.toString())
            put("amount", amount)
            put("direction", direction.name)
            put("merchant", merchant)
            put("category", category)
            put("source", "manual")
            put("cat_source", catSource)
        })
    }

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

    /**
     * The existing transaction this statement row corresponds to, if any: same UPI/IMPS reference,
     * or the same amount and direction within a day either side (SMS and statement dates can differ
     * by a day), closest date first. [exclude] holds ids already matched in this import, so one SMS
     * transaction can't stand in for two separate statement rows.
     */
    fun findMatch(date: LocalDate, amount: Double, direction: Direction, ref: String?, exclude: Set<Long>): Long? {
        val notIn = if (exclude.isEmpty()) "" else " AND id NOT IN (${exclude.joinToString(",")})"
        if (ref != null) {
            readableDatabase.rawQuery(
                "SELECT id FROM transactions WHERE (ref=? OR raw LIKE ?) AND direction=?$notIn LIMIT 1",
                arrayOf(ref, "%$ref%", direction.name),
            ).use { if (it.moveToFirst()) return it.getLong(0) }
        }
        return readableDatabase.rawQuery(
            "SELECT id FROM transactions WHERE direction=? AND abs(amount-?)<0.005 AND txn_date BETWEEN ? AND ?$notIn " +
                "ORDER BY abs(julianday(txn_date)-julianday(?)) LIMIT 1",
            arrayOf(direction.name, amount.toString(), date.minusDays(1).toString(), date.plusDays(1).toString(), date.toString()),
        ).use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    /** Adds a statement row; false if this exact row was imported before. */
    fun insertStatementRow(row: StatementRow, category: String, catSource: String): Boolean {
        val values = ContentValues().apply {
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
        return writableDatabase.insertWithOnConflict("transactions", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
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

    fun delete(id: Long) {
        writableDatabase.delete("transactions", "id=?", arrayOf(id.toString()))
    }

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
    )
}
