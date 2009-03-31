package org.jetbrains.plugins.scala.lang.psi.impl.expr

import psi.ScalaPsiElementImpl
import com.intellij.lang.ASTNode
import api.expr._
import types.Nothing

/** 
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

class ScThrowStmtImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScThrowStmt {
  override def toString: String = "ThrowStatement"

  override def getType = Nothing
}