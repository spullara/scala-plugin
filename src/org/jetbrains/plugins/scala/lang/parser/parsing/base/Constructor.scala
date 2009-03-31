package org.jetbrains.plugins.scala.lang.parser.parsing.base

import com.intellij.lang.PsiBuilder
import expressions.ArgumentExprs
import lexer.ScalaTokenTypes
import types.AnnotType

/**
 * @author AlexanderPodkhalyuzin
 */

/*
 * Constr ::= AnnotType {ArgumentExprs}
 */

object Constructor {
  def parse(builder: PsiBuilder): Boolean = parse(builder, false)
  def parse(builder: PsiBuilder, isAnnotation: Boolean): Boolean = {
    val constrMarker = builder.mark
    if (!AnnotType.parse(builder)) {
      builder error ScalaBundle.message("identifier.expected")
      constrMarker.rollbackTo
      return false
    }
    if (builder.getTokenType == ScalaTokenTypes.tLPARENTHESIS) {
      ArgumentExprs parse builder
      while (builder.getTokenType == ScalaTokenTypes.tLPARENTHESIS && !isAnnotation) {
        ArgumentExprs parse builder
      }
    }
    else builder.mark.done(ScalaElementTypes.ARG_EXPRS)
    constrMarker.done(ScalaElementTypes.CONSTRUCTOR)
    return true
  }
}