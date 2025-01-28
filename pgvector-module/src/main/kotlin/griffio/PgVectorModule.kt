package griffio

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.PostgreSqlTypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import griffio.grammar.PgvectorParserUtil
import griffio.grammar.psi.PgVectorTypeName
import griffio.grammar.rules.ParserOverrides

class PgVectorModule : SqlDelightModule {
    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = PgVectorTypeResolver(parentResolver)

    override fun setup() {
        PgvectorParserUtil.reset()
        PgvectorParserUtil.overridePostgreSqlParser()
        PostgreSqlParserUtil.type_name = GeneratedParserUtilBase.Parser { psiBuilder, i ->
            PgvectorParserUtil.type_name?.parse(psiBuilder, i) ?: ParserOverrides.type_name_real(psiBuilder, i)
        }
        PostgreSqlParserUtil.extension_expr = GeneratedParserUtilBase.Parser { psiBuilder, i ->
            PgvectorParserUtil.extension_expr?.parse(psiBuilder, i) ?: ParserOverrides.extension_expr_real(psiBuilder, i)
        }
    }
}

enum class PgVectorSqlType(override val javaType: TypeName) : DialectType {
    VECTOR(STRING);

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
        return when (this) {
            VECTOR -> CodeBlock.of("bindString(%L, %L)\n", columnIndex, value)
        }
    }

    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
        return CodeBlock.of(
            when (this) {
                VECTOR -> "$cursorName.getString($columnIndex)"
            },
            javaType,
        )
    }
}

// Change to inheritance so that definitionType can be called by polymorphism - not possible with delegation
private class PgVectorTypeResolver(private val parentResolver: TypeResolver) : PostgreSqlTypeResolver(parentResolver) {

    override fun definitionType(typeName: SqlTypeName): IntermediateType = with(typeName) {
        check(this is PgVectorTypeName)
        return IntermediateType(PgVectorSqlType.VECTOR)
    }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? =
        when (functionExpr.functionName.text.lowercase()) {
            "subvector" -> IntermediateType(PgVectorSqlType.VECTOR)
            "cosine_distance" -> IntermediateType(PrimitiveType.REAL)
            else -> parentResolver.functionType(functionExpr)
        }
}
