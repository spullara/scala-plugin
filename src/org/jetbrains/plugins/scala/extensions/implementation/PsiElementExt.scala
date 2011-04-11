package org.jetbrains.plugins.scala.extensions.implementation

import iterator._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiFile, PsiReference, PsiElement}
/**
 * Pavel Fatin
 */

trait PsiElementExt {
  protected def repr: PsiElement

  def firstChild: Option[PsiElement] = {
    val child = repr.getFirstChild
    if (child == null) None else Some(child)
  }

  def lastChild: Option[PsiElement] = {
    val child = repr.getLastChild
    if (child == null) None else Some(child)
  }

  def elementAt(offset: Int): Option[PsiElement] = {
    val e = repr.findElementAt(offset)
    if (e == null) None else Some(e)
  }

  def referenceAt(offset: Int): Option[PsiReference] = {
    val e = repr.findReferenceAt(offset)
    if (e == null) None else Some(e)
  }

  def parent: Option[PsiElement] = {
    val p = repr.getParent
    if (p == null) None else Some(p)
  }

  def parents: Iterator[PsiElement] = new ParentsIterator(repr)

  def containingFile: Option[PsiFile] = {
    val f = repr.getContainingFile
    if (f == null) None else Some(f)
  }

  def parentsInFile: Iterator[PsiElement] =
    new ParentsIterator(repr).takeWhile(!_.isInstanceOf[PsiFile])

  def contexts: Iterator[PsiElement] = new ContextsIterator(repr)

  def prevSibling: Option[PsiElement] = {
    val sibling = repr.getPrevSibling
    if (sibling == null) None else Some(sibling)
  }

  def nextSibling: Option[PsiElement] = {
    val sibling = repr.getNextSibling
    if (sibling == null) None else Some(sibling)
  }

  def prevSiblings: Iterator[PsiElement] = new PrevSiblignsIterator(repr)

  def nextSiblings: Iterator[PsiElement] = new NextSiblignsIterator(repr)

  def children: Iterator[PsiElement] = new ChildrenIterator(repr)

  def isAncestorOf(e: PsiElement) = PsiTreeUtil.isAncestor(repr, e, true)

  def depthFirst: Iterator[PsiElement] = depthFirst(DefaultPredicate)

  def depthFirst(predicate: PsiElement => Boolean): Iterator[PsiElement] =
    new DepthFirstIterator(repr, predicate)

  def breadthFirst: Iterator[PsiElement] = depthFirst(DefaultPredicate)

  def breadthFirst(predicate: PsiElement => Boolean): Iterator[PsiElement] =
    new BreadthFirstIterator(repr, predicate)

  def isScope: Boolean = ScalaPsiUtil.isScope(repr)

  def scopes: Iterator[PsiElement] = contexts.filter(ScalaPsiUtil.isScope(_))

  def containingScalaFile: Option[ScalaFile] = repr.getContainingFile match {
    case sf: ScalaFile => Some(sf)
    case _ => None
  }
}
