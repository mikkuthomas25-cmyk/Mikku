package com.fintrack.core

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Exercises the real SDK request/response path against a local fake API (no network, no key). */
class ClaudeAiTest {
    private val requests = mutableListOf<Pair<Map<String, String>, String>>()
    private var reply = ""
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/v1/messages") { ex ->
            requests += ex.requestHeaders.mapValues { it.value.joinToString() }.mapKeys { it.key.lowercase() } to
                ex.requestBody.readBytes().decodeToString()
            val body = """{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
                "content":[{"type":"text","text":${quote(reply)}}],"stop_reason":"end_turn","stop_sequence":null,
                "usage":{"input_tokens":1,"output_tokens":1}}""".toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }
        start()
    }
    private val ai = ClaudeAi("test-key", "http://127.0.0.1:${server.address.port}")

    @AfterTest fun stop() = server.stop(0)

    private fun quote(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    @Test fun categorizesWithStructuredOutputAndFallbacks() {
        reply = """{"results":[{"merchant":"Thirdwave","category":"Food & Dining"},{"merchant":"Xyz","category":"Not A Category"}]}"""
        assertEquals(mapOf("Thirdwave" to "Food & Dining"), ai.categorizeMerchants(listOf("Thirdwave", "Xyz")))
        val (headers, body) = requests.single()
        assertTrue("server-side-fallback-2026-07-01" in headers.getValue("anthropic-beta"))
        assertTrue("\"fallbacks\":\"default\"" in body, body)
        assertTrue("\"format\"" in body && "json_schema" in body, body)
        assertTrue("claude-opus-5-5" in body)
    }

    @Test fun insightsSendOnlyAggregates() {
        reply = "You spent most on rent."
        val txn = Txn(1, LocalDate.of(2026, 10, 3), 15000.0, Direction.DEBIT, "Landlord", "Rent",
            raw = "SECRET RAW SMS TEXT")
        val s = Summaries.build(YearMonth.of(2026, 10), listOf(txn), emptyList(), LocalDate.of(2026, 10, 26))
        assertEquals("You spent most on rent.", ai.monthlyInsights(s))
        val body = requests.single().second
        assertTrue("SECRET RAW SMS TEXT" !in body)
        assertTrue("\"effort\":\"medium\"" in body, body)
    }
}
