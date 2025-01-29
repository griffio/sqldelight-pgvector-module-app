package griffio.grammar.rules

import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import griffio.grammar.PgvectorParser
import griffio.grammar.PgvectorParserUtil
import griffio.grammar.psi.PgvectorTypes
import griffio.grammar.psi.PgvectorTypes.TYPE_NAME

object ParserOverrides : GeneratedParserUtilBase() {
    @JvmStatic
    fun typeNameDataTypeExt(
        builder: PsiBuilder,
        level: Int,
        geometry_data_type: Parser,
    ): Boolean = geometry_data_type.parse(builder, level)

    @JvmStatic
    fun type_name_real(builder_: PsiBuilder, level_: Int): Boolean {
        var result_: Boolean?
        val marker_ = enter_section_(
            builder_,
            level_,
            _COLLAPSE_,
            TYPE_NAME,
            "<type name real>",
        )

        result_ = typeNameDataTypeExt(
            builder_, level_ + 1,
        ) { builder: PsiBuilder, level: Int ->
            PostgreSqlParser.type_name_real(
                builder,
                level,
            )
        }

        if (!result_) result_ = PgvectorParserUtil.vectorDataTypeExt(
            builder_, level_ + 1,
        ) { builder: PsiBuilder, level: Int ->
            PgvectorParser.vector_data_type_real(
                builder,
                level,
            )
        }

        exit_section_(builder_, level_, marker_, result_, false, null)
        return result_
    }

    @JvmStatic
    fun extensionExprExt(
        builder: PsiBuilder,
        level: Int,
        vector_data_type: Parser,
    ): Boolean = vector_data_type.parse(builder, level)

    fun extension_expr_real(builder_: PsiBuilder, level_: Int): Boolean {
        if (!recursion_guard_(builder_, level_, "extension_expr_real")) return false
        var result_: Boolean?
        val marker_ =
            enter_section_(builder_, level_, _COLLAPSE_, PgvectorTypes.EXTENSION_EXPR, "<extension expr real>")

        result_ = extensionExprExt(
            builder_, level_ + 1,
        ) { builder: PsiBuilder, level: Int ->
            PostgreSqlParser.extension_expr_real(
                builder,
                level,
            )
        }

        if (!result_) result_ = PgvectorParserUtil.distanceOperatorExpressionExt(
            builder_, level_ + 1
        ) { builder: PsiBuilder?, level: Int ->
            PgvectorParser.distance_operator_expression_real(
                builder,
                level
            )
        }

        exit_section_(builder_, level_, marker_, result_, false, null)
        return result_
    }
}
