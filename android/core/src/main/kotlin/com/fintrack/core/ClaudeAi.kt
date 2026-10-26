package com.fintrack.core

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.time.Duration

/**
 * Optional Claude features. Callers run these off the main thread.
 *
 * Privacy: categorization sends only merchant names; insights send only
 * monthly aggregates ([MonthSummary.toPromptText]). Raw SMS text is never sent.
 */
class ClaudeAi(apiKey: String, baseUrl: String? = null) {
    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .apply { if (baseUrl != null) baseUrl(baseUrl) }
        .timeout(Duration.ofSeconds(120))
        .build()

    class MerchantCategory {
        @JsonPropertyDescription("The merchant name exactly as given")
        var merchant: String = ""

        @JsonPropertyDescription("One of the allowed category names")
        var category: String = ""
    }

    class CategoryBatch {
        var results: List<MerchantCategory> = emptyList()
    }

    /** Maps each merchant to a category from [Categorizer.CATEGORIES]; unknown answers are dropped. */
    fun categorizeMerchants(merchants: List<String>): Map<String, String> {
        if (merchants.isEmpty()) return emptyMap()
        val prompt = buildString {
            appendLine("These are merchant / payee names taken from Indian bank and UPI transaction SMS.")
            appendLine("Assign each the single best spending category. Names may be truncated, a UPI handle,")
            appendLine("or a person's name (use Transfers for people).")
            appendLine()
            appendLine("Allowed categories: ${Categorizer.CATEGORIES.joinToString(", ")}")
            appendLine()
            appendLine("Merchants:")
            merchants.forEach { appendLine("- $it") }
        }
        val params = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(4000L)
            .outputConfig(CategoryBatch::class.java)
            .addUserMessage(prompt)
            .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .build()
        val response = client.messages().create(params)
        if (response.stopReason().orElse(null) == StopReason.REFUSAL) return emptyMap()
        val allowed = Categorizer.CATEGORIES.toSet()
        return response.content()
            .mapNotNull { it.text().orElse(null)?.text() }
            .flatMap { it.results }
            .filter { it.category in allowed }
            .associate { it.merchant to it.category }
    }

    /** Plain-language analysis of one month: where the money went and how to save. */
    fun monthlyInsights(summary: MonthSummary): String {
        val prompt = """
            You are a personal finance coach. Below is one month of my spending, aggregated
            from bank SMS (amounts in INR). Give me:
            1. Where my money went, in two or three sentences.
            2. Three specific, realistic ways to save next month, with estimated rupee amounts.
            3. Anything unusual (spikes, a category growing fast, many small payments adding up).
            Plain text, short paragraphs, no markdown. Don't invent data that isn't here.

        """.trimIndent() + "\n" + summary.toPromptText()
        val params = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(4000L)
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.MEDIUM).build())
            .addUserMessage(prompt)
            .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .build()
        val response = client.messages().create(params)
        if (response.stopReason().orElse(null) == StopReason.REFUSAL) {
            return "Claude declined to answer this request."
        }
        return response.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("").trim()
    }

    companion object {
        const val MODEL = "claude-opus-5-5"
        private const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
    }
}
