package griffio

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.PrimitiveType.BOOLEAN
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import griffio.PostGisSqlType.GEOMETRY
import griffio.grammar.PostgisParser
import griffio.grammar.PostgisParserUtil
import griffio.grammar.PostgisParserUtil.type_name
import griffio.grammar.psi.PostGisTypeName

class PostGisModule : SqlDelightModule {
    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = PostGisTypeResolver(parentResolver)

    override fun setup() {
        PostgisParserUtil.reset()
        PostgisParserUtil.overridePostgreSqlParser()
        PostgreSqlParserUtil.type_name = GeneratedParserUtilBase.Parser { psiBuilder, i ->
            type_name?.parse(psiBuilder, i) ?: PostgisParser.type_name_real(psiBuilder, i)
                    || PostgreSqlParser.type_name_real(psiBuilder, i)
        }
    }
}

enum class PostGisSqlType(override val javaType: TypeName) : DialectType {
    GEOMETRY(STRING), GEOGRAPHY(STRING);

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
        return when (this) {
            GEOMETRY, GEOGRAPHY -> CodeBlock.of("bindString(%L, %L)\n", columnIndex, value)
        }
    }

    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
        return CodeBlock.of(
            when (this) {
                GEOMETRY, GEOGRAPHY -> "$cursorName.getString($columnIndex)"
            },
            javaType,
        )
    }
}

// Change to inheritance so that definitionType can be called by polymorphism - not possible with delegation
open class PostGisTypeResolver(private val parentResolver: TypeResolver) : PgVectorTypeResolver(parentResolver) {
    override fun definitionType(typeName: SqlTypeName): IntermediateType {
        return when (typeName) {
            is PostGisTypeName ->
                when {
                    typeName.geometryDataType != null -> IntermediateType(PostGisSqlType.GEOMETRY)
                    typeName.geographyDataType != null -> IntermediateType(PostGisSqlType.GEOGRAPHY)
                    else -> error("postgis typename")
                }

            else -> super.definitionType(typeName)
        }
    }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? =
        when (functionExpr.functionName.text.lowercase()) {
            "st_geographyfromtext" -> IntermediateType(PostGisSqlType.GEOGRAPHY).nullableIf(resolvedType(functionExpr.exprList[0]).javaType.isNullable)
            "st_dwithin" -> IntermediateType(BOOLEAN).nullableIf(resolvedType(functionExpr.exprList[0]).javaType.isNullable)
            "st_force2d" -> IntermediateType(TEXT).nullableIf(resolvedType(functionExpr.exprList[0]).javaType.isNullable)
            "st_astext" -> IntermediateType(TEXT).nullableIf(resolvedType(functionExpr.exprList[0]).javaType.isNullable)
            "st_makepoint" -> IntermediateType(GEOMETRY)
            "st_setsrid" -> IntermediateType(GEOMETRY)
            "st_x" -> IntermediateType(PrimitiveType.REAL).nullableIf(resolvedType(functionExpr.exprList[0]).javaType.isNullable)
            "st_y" -> IntermediateType(PrimitiveType.REAL).nullableIf(resolvedType(functionExpr.exprList[0]).javaType.isNullable)
            "st_z" -> IntermediateType(PrimitiveType.REAL).nullableIf(resolvedType(functionExpr.exprList[0]).javaType.isNullable)
            else -> parentResolver.functionType(functionExpr)
        }
}
