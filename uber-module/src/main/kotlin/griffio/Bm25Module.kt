package griffio

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.STRING
import griffio.grammar.Bm25Parser
import griffio.grammar.Bm25ParserUtil
import griffio.grammar.Bm25ParserUtil.extension_expr
import griffio.grammar.Bm25ParserUtil.index_method
import griffio.grammar.Bm25ParserUtil.storage_parameters
import griffio.grammar.Bm25ParserUtil.type_name
import griffio.grammar.psi.Bm25ExtensionExpr
import griffio.grammar.psi.Bm25TypeName
import kotlin.text.lowercase

class Bm25Module : SqlDelightModule {
    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = Bm25TypeResolver(parentResolver)

    override fun setup() {
        Bm25ParserUtil.reset()
        Bm25ParserUtil.overridePostgreSqlParser()
        // As the grammar doesn't support inheritance - override type_name manually to try inherited type_name
        PostgreSqlParserUtil.type_name = Parser { psiBuilder, i ->
            type_name?.parse(psiBuilder, i) ?: Bm25Parser.type_name_real(psiBuilder, i)
                    || PostgreSqlParser.type_name_real(psiBuilder, i)
        }

        PostgreSqlParserUtil.extension_expr = Parser { psiBuilder, i ->
            extension_expr?.parse(psiBuilder, i) ?: Bm25Parser.extension_expr_real(psiBuilder, i)
                    || PostgreSqlParser.extension_expr_real(psiBuilder, i)
        }

        // etc
        PostgreSqlParserUtil.index_method = Parser { psiBuilder, i ->
            index_method?.parse(psiBuilder, i) ?: Bm25Parser.index_method_real(psiBuilder, i)
                    || PostgreSqlParser.index_method_real(psiBuilder, i)
        }
        // etc
        PostgreSqlParserUtil.storage_parameters = Parser { psiBuilder, i ->
            storage_parameters?.parse(psiBuilder, i) ?: Bm25Parser.storage_parameters_real(psiBuilder, i)
                    || PostgreSqlParser.storage_parameters_real(psiBuilder, i)
        }

    }
}

enum class Bm25VectorSqlType(override val javaType: TypeName) : DialectType {
    BM25VECTOR(STRING);

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
        return when (this) {
            BM25VECTOR -> CodeBlock.of("bindString(%L, %L)\n", columnIndex, value)
        }
    }

    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
        return CodeBlock.of(
            when (this) {
                BM25VECTOR -> "$cursorName.getString($columnIndex)"
            },
            javaType,
        )
    }
}

// Change to inheritance so that definitionType can be called by polymorphism - not possible with delegation
class Bm25TypeResolver(private val parentResolver: TypeResolver) : PostGisTypeResolver(parentResolver) {

    override fun definitionType(typeName: SqlTypeName): IntermediateType {
        return when (typeName) {
            is Bm25TypeName -> IntermediateType(Bm25VectorSqlType.BM25VECTOR)
            else -> super.definitionType(typeName)
        }
    }

    override fun resolvedType(expr: SqlExpr) : IntermediateType {
        return if (expr is Bm25ExtensionExpr && expr.scoreOperatorExpression != null)
            IntermediateType(PrimitiveType.REAL) else super.resolvedType(expr)
    }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? =
        when (functionExpr.functionName.text.lowercase()) {
            "tokenize" -> IntermediateType(Bm25VectorSqlType.BM25VECTOR)
            "to_bm25query" -> IntermediateType(Bm25VectorSqlType.BM25VECTOR)
            else -> super.functionType(functionExpr)
        }
}
