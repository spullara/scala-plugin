package org.jetbrains.plugins.scala.lang.psi.api.expr

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement

/** 
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

trait ScIfStmt extends ScExpression {
  def condition: Option[ScExpression]
  def thenBranch : Option[ScExpression]
  def elseBranch : Option[ScExpression]
}