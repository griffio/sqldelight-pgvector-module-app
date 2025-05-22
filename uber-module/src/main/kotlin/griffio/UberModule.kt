package griffio

import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParserUtil

import com.intellij.lang.parser.GeneratedParserUtilBase
import griffio.grammar.PgvectorParser
import griffio.grammar.PostgisParser
import griffio.grammar.PostgisParserUtil.type_name

class UberModule : SqlDelightModule {
    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = PostGisTypeResolver(PgVectorTypeResolver(parentResolver))

    override fun setup() {
        // for each module the rules must be overridden in the PostgreSql dialect parser
        PostGisModule().setup()
        PgVectorModule().setup()
        // For example - Postgis and PgVector both add new data types - a new combined rule must be added to the dialect parser
        PostgreSqlParserUtil.type_name = GeneratedParserUtilBase.Parser { psiBuilder, i ->
            type_name?.parse(psiBuilder, i) ?: PostgisParser.type_name_real(psiBuilder, i)
                    || PgvectorParser.type_name_real(psiBuilder, i) || PostgreSqlParser.type_name_real(psiBuilder, i)
        }
    }
}
