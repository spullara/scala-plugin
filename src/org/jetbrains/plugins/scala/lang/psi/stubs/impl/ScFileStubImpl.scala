package org.jetbrains.plugins.scala.lang.psi.stubs.impl
import api.ScalaFile
import com.intellij.psi.PsiClass
import com.intellij.psi.tree.{TokenSet, IStubFileElementType}
import parser.ScalaElementTypes
import elements.wrappers.PsiFileStubWrapperImpl
import com.intellij.util.io.StringRef
import com.intellij.psi.stubs.PsiFileStubImpl

/**
 * @author ilyas
 */

class ScFileStubImpl(file: ScalaFile) extends PsiFileStubWrapperImpl[ScalaFile](file) with ScFileStub {

  override def getType = ScalaElementTypes.FILE.asInstanceOf[IStubFileElementType[Nothing]]

  implicit  def refToStr(ref: StringRef) = StringRef.toString(ref)

  var packName: StringRef = _
  var sourceFileName: StringRef = _
  var compiled: Boolean = false

  def this(file: ScalaFile, pName : StringRef, name: StringRef, compiled: Boolean) = {
    this(file)
    this.sourceFileName = name
    packName = pName
    this.compiled = compiled
  }

  def getClasses = {
    import ScalaElementTypes._
    getChildrenByType(TokenSet.create(CLASS_DEF, OBJECT_DEF, TRAIT_DEF), PsiClass.ARRAY_FACTORY)
  }

  def getFileName = sourceFileName

  def packageName = packName

  def isCompiled: Boolean = compiled
}