package griffio

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType
import app.cash.sqldelight.dialects.postgresql.PostgreSqlTypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import griffio.grammar.PgvectorParser
import griffio.grammar.PgvectorParserUtil
import griffio.grammar.PgvectorParserUtil.extension_expr
import griffio.grammar.PgvectorParserUtil.index_method
import griffio.grammar.PgvectorParserUtil.storage_parameters
import griffio.grammar.PgvectorParserUtil.type_name
import griffio.grammar.psi.PgVectorTypeName
import griffio.grammar.psi.impl.PgVectorExtensionExprImpl

class PgVectorModule : SqlDelightModule {
    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = PgVectorTypeResolver(parentResolver)

    override fun setup() {
        PgvectorParserUtil.reset()
        PgvectorParserUtil.overridePostgreSqlParser()
        // As the grammar doesn't support inheritance - override type_name manually to try inherited type_name
        PostgreSqlParserUtil.type_name = Parser { psiBuilder, i ->
            type_name?.parse(psiBuilder, i) ?: PgvectorParser.type_name_real(psiBuilder, i)
                    || PostgreSqlParser.type_name_real(psiBuilder, i)
        }
        // doesn't support inheritance - override extension_expr manually to try inherited extension_expr
        PostgreSqlParserUtil.extension_expr = Parser { psiBuilder, i ->
            extension_expr?.parse(psiBuilder, i) ?: PgvectorParser.extension_expr_real(psiBuilder, i)
                    || PostgreSqlParser.extension_expr_real(psiBuilder, i)
        }
        // etc
        PostgreSqlParserUtil.index_method = Parser { psiBuilder, i ->
            index_method?.parse(psiBuilder, i) ?: PgvectorParser.index_method_real(psiBuilder, i)
                    || PostgreSqlParser.index_method_real(psiBuilder, i)
        }
        // etc
        PostgreSqlParserUtil.storage_parameters = Parser { psiBuilder, i ->
            storage_parameters?.parse(psiBuilder, i) ?: PgvectorParser.storage_parameters_real(psiBuilder, i)
                    || PostgreSqlParser.storage_parameters_real(psiBuilder, i)
        }

    }
}

enum class PgVectorSqlType(override val javaType: TypeName) : DialectType {
    BIT(STRING),
    VECTOR(STRING);

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
        return when (this) {
            BIT -> CodeBlock.of("bindString(%L, %L)\n", columnIndex, value)
            VECTOR -> CodeBlock.of("bindString(%L, %L)\n", columnIndex, value)
        }
    }

    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
        return CodeBlock.of(
            when (this) {
                BIT, VECTOR -> "$cursorName.getString($columnIndex)"
            },
            javaType,
        )
    }
}

// Change to inheritance so that definitionType can be called by polymorphism - not possible with delegation
private class PgVectorTypeResolver(private val parentResolver: TypeResolver) : PostgreSqlTypeResolver(parentResolver) {

    override fun booleanBinaryExprTypes(): Array<DialectType> {
        return arrayOf(PgVectorSqlType.VECTOR, PgVectorSqlType.BIT, *super.booleanBinaryExprTypes())
    }

    override fun definitionType(typeName: SqlTypeName): IntermediateType {
        return when (typeName) {
            is PgVectorTypeName -> if (typeName.bitDataType != null) IntermediateType(PgVectorSqlType.BIT) else IntermediateType(PgVectorSqlType.VECTOR)
            else -> super.definitionType(typeName)
        }
    }

    override fun resolvedType(expr: SqlExpr): IntermediateType {
        return when (expr) {
            is PgVectorExtensionExprImpl -> expr.vectorExtension()
            else -> super.resolvedType(expr)
        }
    }

    /**
     * Supported distance functions are:
     * <-> - L2 distance
     * <#> - (negative) inner product
     * <=> - cosine distance
     * <+> - L1 distance
     * <~> - Hamming distance (binary vectors) // BIT type
     * <%> - Jaccard distance (binary vectors) // BIT type
     */
    fun PgVectorExtensionExprImpl.vectorExtension(): IntermediateType {
        if (distanceOperatorExpression != null) return IntermediateType(PrimitiveType.REAL) else error("must be distanceOperatorExpression")
    }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? =
        when (functionExpr.functionName.text.lowercase()) {
            "avg" -> IntermediateType(PgVectorSqlType.VECTOR)
            "binary_quantize" -> IntermediateType(PgVectorSqlType.BIT)
            "cosine_distance" -> IntermediateType(PrimitiveType.REAL)
            "inner_product" -> IntermediateType(PrimitiveType.REAL)
            "l1_distance" -> IntermediateType(PrimitiveType.REAL)
            "l2_distance" -> IntermediateType(PrimitiveType.REAL)
            "l2_normalize" -> IntermediateType(PgVectorSqlType.VECTOR)
            "subvector" -> IntermediateType(PgVectorSqlType.VECTOR)
            "sum" -> IntermediateType(PgVectorSqlType.VECTOR)
            "vector_dims" -> IntermediateType(PostgreSqlType.INTEGER)
            "vector_norm" -> IntermediateType(PrimitiveType.REAL)
            else -> super.functionType(functionExpr)
        }
}
