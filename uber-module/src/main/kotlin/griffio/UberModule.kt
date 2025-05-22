package griffio

import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParserUtil

import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser
import griffio.grammar.Bm25Parser
import griffio.grammar.Bm25ParserUtil.extension_expr
import griffio.grammar.Bm25ParserUtil.index_method
import griffio.grammar.Bm25ParserUtil.storage_parameters
import griffio.grammar.PgvectorParser
import griffio.grammar.PostgisParser
import griffio.grammar.PostgisParserUtil.type_name

class UberModule : SqlDelightModule {
    // all typeResolvers must be combined to resolve types for all modules - parentResolver is passed when this module is loaded and provides ansi types
    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = Bm25TypeResolver(PostGisTypeResolver(PgVectorTypeResolver(parentResolver)))

    override fun setup() {
        // for each module the rules must be overridden in the PostgreSql dialect parser
        PgVectorModule().setup()
        PostGisModule().setup()
        Bm25Module().setup()
        // For example - Postgis, PgVector and Bm25 all add new data types - a new combined rule must be added to the dialect parser
        // All the rules that are shared must call all overridden parser rules
        PostgreSqlParserUtil.type_name = GeneratedParserUtilBase.Parser { psiBuilder, i ->
            type_name?.parse(psiBuilder, i) ?: PostgisParser.type_name_real(psiBuilder, i)
                    || PgvectorParser.type_name_real(psiBuilder, i)
                    || Bm25Parser.type_name_real(psiBuilder, i)
                    || PostgreSqlParser.type_name_real(psiBuilder, i)
        }
        PostgreSqlParserUtil.extension_expr = Parser { psiBuilder, i ->
            extension_expr?.parse(psiBuilder, i) ?: Bm25Parser.extension_expr_real(psiBuilder, i)
                    || PgvectorParser.extension_expr_real(psiBuilder, i)
                    || PostgreSqlParser.extension_expr_real(psiBuilder, i)
        }
        PostgreSqlParserUtil.index_method = Parser { psiBuilder, i ->
            index_method?.parse(psiBuilder, i) ?: Bm25Parser.index_method_real(psiBuilder, i)
                    || PgvectorParser.index_method_real(psiBuilder, i)
                    || PostgreSqlParser.index_method_real(psiBuilder, i)
        }
        PostgreSqlParserUtil.storage_parameters = Parser { psiBuilder, i ->
            storage_parameters?.parse(psiBuilder, i) ?: Bm25Parser.storage_parameters_real(psiBuilder, i)
                    || PgvectorParser.storage_parameters_real(psiBuilder, i)
                    || PostgreSqlParser.storage_parameters_real(psiBuilder, i)
        }

    }
}
