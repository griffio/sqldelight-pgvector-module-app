package griffio

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.pgvector.PGvector
import griffio.migrations.Items
import griffio.queries.Sample
import org.postgresql.ds.PGSimpleDataSource

private fun getSqlDriver() = PGSimpleDataSource().apply {
    setURL("jdbc:postgresql://localhost:5432/vector")
    applicationName = "App Main"
    user = "postgres"
    password = ""
}.asJdbcDriver()

val vectorAdapter = object: ColumnAdapter<PGvector, String> {
    override fun decode(databaseValue: String): PGvector = PGvector(databaseValue)
    override fun encode(value: PGvector): String = value.toString()
}

val adapters = Items.Adapter(vectorAdapter)

fun main() {
    val driver = getSqlDriver()
    val sample = Sample(driver, adapters)
    sample.vectorQueries.insert()
    sample.vectorQueries.select().executeAsList().forEach(::println)
    println("embeddings")
    sample.vectorQueries.selectEmbeddings().executeAsList().forEach(::println)
    println("embeddings")
    sample.vectorQueries.selectWithVector("[3,1,2]").executeAsList().forEach(::println)
    println("subVector")
    sample.vectorQueries.selectSubVector("[1,2,3,4,5]").executeAsList().forEach(::println)
    println("cosineDistance")
    sample.vectorQueries.selectCosineDistance().executeAsList().forEach(::println)
    println("binaryQuantize")
    sample.vectorQueries.selectBinaryQuantize().executeAsList().forEach(::println)
    println("l2Distance")
    sample.vectorQueries.selectL2Distance().executeAsList().forEach(::println)
    println("innerProduct")
    sample.vectorQueries.selectInnerProduct().executeAsList().forEach(::println)
    println("euclideanDistance")
    sample.vectorQueries.selectL1Distance().executeAsList().forEach(::println)
    println("selectL2Normalize")
    sample.vectorQueries.selectL2Normalize().executeAsList().forEach(::println)
    println("selectGT")
    sample.vectorQueries.selectGT().executeAsList().forEach(::println)
    println("selectAvg")
    sample.vectorQueries.selectAvg().executeAsList().forEach(::println)
    println("selectMultiply")
    sample.vectorQueries.selectMultiply().executeAsList().forEach(::println)
    println("selectNegativeDistanceOperator")
    sample.vectorQueries.selectNegativeDistanceOperator().executeAsList().forEach(::println)
}
