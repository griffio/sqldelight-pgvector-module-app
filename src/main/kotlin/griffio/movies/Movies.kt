package griffio.movies

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.pgvector.PGbit
import com.pgvector.PGvector
import griffio.migrations.Items
import griffio.queries.Sample
import org.postgresql.ds.PGSimpleDataSource
import java.net.URI

private fun getSqlDriver() = PGSimpleDataSource().apply {
    setURL("jdbc:postgresql://localhost:5432/vector")
    applicationName = "App Main"
    user = "postgres"
    password = ""
}.asJdbcDriver()

val bitVectorAdapter = object: ColumnAdapter<PGbit, String> {
    override fun decode(databaseValue: String): PGbit = PGbit(databaseValue)
    override fun encode(value: PGbit): String = value.toString()
}
val vectorAdapter = object: ColumnAdapter<PGvector, String> {
    override fun decode(databaseValue: String): PGvector = PGvector(databaseValue)
    override fun encode(value: PGvector): String = value.toString()
}

val adapters = Items.Adapter(vectorAdapter, bitVectorAdapter)

fun main() {
    // use Ollama locally with an embedding model
    val http = EmbeddingsClient(URI.create("http://localhost:11434"), "qwen3-embedding:0.6b")
    val driver = getSqlDriver()
    val sample = Sample(driver, adapters)

    val movies = sample.movieQueries.select().executeAsList()

    for (movie in movies) {
        val response = http.embeddings(movie.plot_summary)
        sample.movieQueries.update(movieId = movie.id, embeddingResponse = response)
    }

    fun search(prompt: String) {
        val searchResponse = http.embeddings(prompt)
        val header = "-".repeat(prompt.length)
        println(header)
        println(prompt)
        println(header)
        sample.movieQueries.similaritySearch(searchResponse).executeAsList().forEach { movie ->
            println("""${movie.title} ( ${movie.year} ${movie.genre} ) Score: ${movie.similarityScore}""")
        }
    }

    search("Tell me some movies that have a dystopian setting in them.")
    search("Movies about questioning what's real.")
    search("Films where time works differently.")
    search("Something uplifting about never giving up.")
    search("Stories about rich and poor people.")

}
