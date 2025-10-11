package griffio.movies

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.text.iterator

/**
 * A lightweight HTTP client to call the embeddings endpoint, equivalent to:
 *
 * curl http://localhost:11434/v1/embeddings \
 *   -H "Content-Type: application/json" \
 *   -d '{"model":"llama3.1","input":["dog"]}'
 *
 * Reuses a single HttpClient instance for connection pooling,
 * which is optimal for repeated/batch calls to the same host.
 */
class EmbeddingsClient(
    val baseUrl: URI = URI.create("http://localhost:11434"),
    val model: String = "qwen3-embedding:0.6b",
    httpClient: HttpClient? = null
) {

    private val client: HttpClient = httpClient ?: defaultClient

    fun embeddings(input: String): String = embeddings(listOf(input))

    fun embeddings(inputs: List<String>): String {
        val body = buildRequestBody(model, inputs)
        val request = HttpRequest.newBuilder()
            .uri(baseUrl.resolve("/v1/embeddings"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {
            return response.body()
        } else {
            throw IOException("Embeddings request failed: ${response.statusCode()} ${response.body()}")
        }
    }
    private fun buildRequestBody(model: String, inputs: List<String>): String {
        val inputsJson = inputs.joinToString(prefix = "[", postfix = "]") { "\"${escapeJson(it)}\"" }
        return """{"model":"$model","input":$inputsJson}"""
    }

    companion object {

        val defaultClient: HttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        private fun escapeJson(s: String): String {
            val sb = StringBuilder(s.length + 16)
            for (ch in s) {
                when (ch) {
                    '\"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    else -> {
                        if (ch < ' ') {
                            sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                        } else {
                            sb.append(ch)
                        }
                    }
                }
            }
            return sb.toString()
        }
    }
}
