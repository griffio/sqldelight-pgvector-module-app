package griffio

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.PrimitiveType.BOOLEAN
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType
import app.cash.sqldelight.dialects.postgresql.PostgreSqlTypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTypes
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import griffio.grammar.PgvectorParserUtil
import griffio.grammar.psi.PgVectorTypeName

class PgVectorModule : SqlDelightModule {
    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = PgVectorTypeResolver(parentResolver)

    override fun setup() {
        PgvectorParserUtil.reset()
        PgvectorParserUtil.overridePostgreSqlParser()
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
        val type = when {
            vectorDataType != null -> IntermediateType(PgVectorSqlType.VECTOR)
            else -> super.definitionType(typeName)
        }
        return type
    }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? =
        when (functionExpr.functionName.text.lowercase()) {
            "binary_quantize" -> IntermediateType(PostgreSqlType.BIT)
            "cosine_distance" -> IntermediateType(PrimitiveType.REAL)
            "inner_product" -> IntermediateType(PrimitiveType.REAL)
            "l1_distance" -> IntermediateType(PrimitiveType.REAL)
            "l2_distance" -> IntermediateType(PrimitiveType.REAL)
            "l2_normalize" -> IntermediateType(PgVectorSqlType.VECTOR)
            "subvector" -> IntermediateType(PgVectorSqlType.VECTOR)
            "vector_dims" -> IntermediateType(PostgreSqlType.INTEGER)
            "vector_norm" -> IntermediateType(PrimitiveType.REAL)
            else -> parentResolver.functionType(functionExpr)
        }
}
