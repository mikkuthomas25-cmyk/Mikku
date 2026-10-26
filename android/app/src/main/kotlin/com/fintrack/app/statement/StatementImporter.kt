package com.fintrack.app.statement

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.fintrack.app.data.TxnStore
import com.fintrack.core.Categorizer
import com.fintrack.core.StatementParser
import com.fintrack.core.StatementResult
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper

/** Thrown when a PDF is password-protected and no (or a wrong) password was given. */
class PasswordRequired(val wrongPassword: Boolean) : Exception()

data class ImportOutcome(
    val fileName: String,
    val result: StatementResult,
    val added: Int,
    val alreadyKnown: Int,
)

/**
 * Imports bank statements (PDF, or CSV exported from net banking). Everything happens on the
 * phone: the file is read, rows are checked against the running balance, matched against
 * transactions already captured from SMS, and only the missing ones are added.
 */
object StatementImporter {
    @Volatile private var pdfReady = false

    fun displayName(context: Context, uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment ?: "statement"

    fun import(context: Context, store: TxnStore, uri: Uri, password: String?): ImportOutcome {
        val name = displayName(context, uri)
        val result = StatementParser.parse(readText(context, uri, name, password))
        val rules = store.rules()
        var added = 0
        var known = 0
        val matched = HashSet<Long>()
        store.transaction {
            for (row in result.rows) {
                val match = store.findMatch(row.date, row.amount, row.direction, row.ref, matched)
                if (match != null) {
                    matched += match
                    known++
                    continue
                }
                val (category, source) = Categorizer.categorizeWithSource(row.merchant, row.direction, rules)
                if (store.insertStatementRow(row, category, source)) added++ else known++
            }
        }
        return ImportOutcome(name, result, added, known)
    }

    private fun readText(context: Context, uri: Uri, name: String, password: String?): String {
        val mime = context.contentResolver.getType(uri) ?: ""
        val isPdf = mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true)
        val input = context.contentResolver.openInputStream(uri) ?: error("Couldn't open $name")
        if (!isPdf) return input.bufferedReader().use { csvToLines(it.readText()) }
        if (!pdfReady) {
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfReady = true
        }
        return input.use { stream ->
            try {
                PDDocument.load(stream, password ?: "").use { doc ->
                    PDFTextStripper().apply { sortByPosition = true }.getText(doc)
                }
            } catch (e: InvalidPasswordException) {
                throw PasswordRequired(wrongPassword = password != null)
            }
        }
    }

    /** CSV rows → one line per row with fields separated by spaces (quotes and commas inside them handled). */
    internal fun csvToLines(text: String): String = text.lines().joinToString("\n") { line ->
        val fields = mutableListOf<String>()
        val cur = StringBuilder()
        var quoted = false
        for (ch in line) {
            when {
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> { fields += cur.toString().trim(); cur.clear() }
                else -> cur.append(ch)
            }
        }
        fields += cur.toString().trim()
        fields.filter { it.isNotEmpty() }.joinToString(" ")
    }
}
