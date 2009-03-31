package org.jetbrains.plugins.scala.lang

/**
 * @author ilyas
 */

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.lang.Language
import org.jetbrains.plugins.scala.lang.lexer.ScalaElementType
import org.jetbrains.plugins.scala.ScalaLanguage
import com.intellij.psi.tree.TokenSet
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes._
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes._

object TokenSets {

  val PROPERTY_NAMES = TokenSet.create(tIDENTIFIER)

  val TMPL_OR_PACKAGING_DEF_BIT_SET = TokenSet.create(PACKAGING, OBJECT_DEF, CLASS_DEF, TRAIT_DEF, FUNCTION_DEFINITION)

  val PACKAGING_BIT_SET = TokenSet.create(PACKAGING)

  val PACKAGE_STMT_BIT_SET = TokenSet.create(PACKAGE_STMT)

  val IMPORT_STMT_BIT_SET = TokenSet.create(IMPORT_STMT)

  val IMPORT_EXPR_BIT_SET = TokenSet.create(IMPORT_EXPR)

  val SELECTOR_BIT_SET = TokenSet.create(IMPORT_SELECTOR)

  val TMPL_DEF_BIT_SET = TokenSet.create(OBJECT_DEF, CLASS_DEF, TRAIT_DEF)

  val TMPL_OR_TYPE_BIT_SET = TokenSet.create(OBJECT_DEF,
      CLASS_DEF,
      TRAIT_DEF,
      TYPE_DEFINITION,
      TYPE_DECLARATION)

  val EXPR1_BIT_SET: TokenSet = TokenSet.create(IF_STMT,
      FOR_STMT,
      WHILE_STMT,
      DO_STMT,
      TRY_STMT,
      TRY_BLOCK,
      CATCH_BLOCK,
      FINALLY_BLOCK,
      RETURN_STMT,
      METHOD_CLOSURE,
      THROW_STMT,
      ASSIGN_STMT,
      MATCH_STMT,
      TYPED_EXPR_STMT,
      POSTFIX_EXPR,
      INFIX_EXPR,
      PLACEHOLDER_EXPR,
      PREFIX_EXPR)

  val STABLE_ID_BIT_SET = TokenSet.create(STABLE_ID,
      tIDENTIFIER)

  val TYPE_BIT_SET: TokenSet = TokenSet.orSet(STABLE_ID_BIT_SET,
      TokenSet.create(SIMPLE_TYPE,
          COMPOUND_TYPE,
          INFIX_TYPE,
          TYPE,
          TYPES,
          COMPOSITE_TYPE))

  val EXPRESSION_BIT_SET = TokenSet.orSet(EXPR1_BIT_SET,
      TokenSet.create(LITERAL,
          STRING_LITERAL,
          BOOLEAN_LITERAL,
          PREFIX_EXPR,
          PREFIX,
          POSTFIX_EXPR,
          INFIX_EXPR,
          PLACEHOLDER_EXPR,
          EXPR1,
          FUNCTION_EXPR,
          AN_FUN,
          GENERATOR,
          ENUMERATOR,
          ENUMERATORS,
          EXPRS,
          ARG_EXPRS,
          BLOCK_EXPR,
          ERROR_STMT,
          BLOCK,
          PARENT_EXPR))

  val SIMPLE_EXPR_BIT_SET = TokenSet.create(PLACEHOLDER_EXPR,
      LITERAL,
      BLOCK_EXPR)

  val REFERENCE_SET = TokenSet.create(REFERENCE)

  val ID_SET = TokenSet.create(tIDENTIFIER, tUNDER)

  val TYPE_PARAMS_SET = TokenSet.create(TYPE_PARAM, VARIANT_TYPE_PARAM)
}