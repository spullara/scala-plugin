package org.jetbrains.plugins.scala.lang.psi.api.expr

import base.{ScStableCodeReferenceElement, ScPathElement}
import toplevel.typedef.ScTypeDefinition
import psi.ScalaPsiElement

/** 
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

trait ScThisReference extends ScExpression with ScPathElement {
  def reference = findChild(classOf[ScStableCodeReferenceElement])

  def refClass : Option[ScTypeDefinition]
}