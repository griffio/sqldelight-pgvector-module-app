package griffio

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import griffio.queries.Sample
import org.postgresql.ds.PGSimpleDataSource

private fun getSqlDriver() = PGSimpleDataSource().apply {
    setURL("jdbc:postgresql://localhost:5432/vector")
    applicationName = "App Main"
    user = "postgres"
    password = ""
}.asJdbcDriver()

fun main() {
    val driver = getSqlDriver()
    val sample = Sample(driver)
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
}
