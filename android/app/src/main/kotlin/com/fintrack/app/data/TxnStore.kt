package com.fintrack.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fintrack.core.Direction
import com.fintrack.core.ParsedTxn
import com.fintrack.core.Txn
import java.time.LocalDate
import java.time.YearMonth

/** On-device SQLite storage. One database file per signed-in account. */
class TxnStore(context: Context, dbName: String) : SQLiteOpenHelper(context, dbName, null, 1) {

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
                raw TEXT
            )""",
        )
        db.execSQL("CREATE INDEX idx_txn_date ON transactions (txn_date)")
        // The same UPI payment often arrives twice (bank SMS + UPI app SMS).
        db.execSQL("CREATE UNIQUE INDEX idx_txn_ref ON transactions (ref, direction, amount) WHERE ref IS NOT NULL")
        db.execSQL("CREATE TABLE merchant_rules (merchant TEXT PRIMARY KEY, category TEXT NOT NULL)")
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    /** Returns the new row id, or null if this transaction is already stored. */
    fun insert(p: ParsedTxn, category: String, source: String, raw: String): Long? {
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
        }
        val id = writableDatabase.insertWithOnConflict("transactions", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return id.takeIf { it != -1L }
    }

    fun addManual(date: LocalDate, amount: Double, direction: Direction, merchant: String, category: String) {
        writableDatabase.insert("transactions", null, ContentValues().apply {
            put("txn_date", date.toString())
            put("amount", amount)
            put("direction", direction.name)
            put("merchant", merchant)
            put("category", category)
            put("source", "manual")
        })
    }

    fun month(month: YearMonth): List<Txn> = readableDatabase.rawQuery(
        "SELECT * FROM transactions WHERE txn_date BETWEEN ? AND ? ORDER BY txn_date DESC, id DESC",
        arrayOf(month.atDay(1).toString(), month.atEndOfMonth().toString()),
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
                "UPDATE transactions SET category=? WHERE lower(merchant)=? AND direction=?",
                arrayOf(category, txn.merchant.lowercase(), txn.direction.name),
            )
        } else {
            db.execSQL("UPDATE transactions SET category=? WHERE id=?", arrayOf(category, txn.id))
        }
    }

    /** AI suggestions only fill in "Other"; they never override the user's own choices. */
    fun fillOtherCategory(merchant: String, category: String) {
        writableDatabase.execSQL(
            "UPDATE transactions SET category=? WHERE merchant=? AND category='Other'",
            arrayOf(category, merchant),
        )
    }

    fun otherMerchants(): List<String> = readableDatabase.rawQuery(
        "SELECT DISTINCT merchant FROM transactions WHERE category='Other' AND direction='DEBIT' AND merchant != 'Unknown'",
        null,
    ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

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
    )
}
