package org.jetbrains.plugins.scala
package lang
package psi
package impl
package expr

import psi.ScalaPsiElementImpl
import com.intellij.lang.ASTNode
import api.expr._
import types._
import nonvalue.{Parameter, ScMethodType, ScTypePolymorphicType}
import result.{TypeResult, Success, TypingContext}
import types.Compatibility.Expression
import collection.Seq

/**
 * @author Alexander Podkhalyuzin
 * Date: 06.03.2008
 */

class ScMethodCallImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScMethodCall {
  override def toString: String = "MethodCall"

  protected override def innerType(ctx: TypingContext): TypeResult[ScType] = {
    val nonValueType = getInvokedExpr.getNonValueType(TypingContext.empty)

    def tuplizyCase(fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem]),
                    exprs: Seq[Expression]): ScType = {
      val c = fun(exprs)
      def tail: ScType = {
        applicabilityProblemsVar = c._2
        c._1
      }
      if (!c._2.isEmpty) {
        ScalaPsiUtil.tuplizy(exprs, getProject, getResolveScope) match {
          case Some(e) => {
            val cd = fun(e)
            if (!cd._2.isEmpty) tail
            else {
              applicabilityProblemsVar = cd._2
              cd._1
            }
          }
          case _ => tail
        }

      } else tail
    }

    val res: ScType = nonValueType match {
      case Success(ScFunctionType(retType: ScType, params: Seq[ScType]), _) => {
        val exprs: Seq[Expression] = argumentExpressions.map(Expression(_))
        val fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem]) = t =>
          (retType, Compatibility.checkConformanceExt(true, params.zipWithIndex.map {case (tp, i) => {
            new Parameter("v" + (i + 1), tp, false, false)
          }}, t, true, false).problems)
        tuplizyCase(fun, exprs)
      }
      case Success(ScMethodType(retType, params, _), _) => {
        val exprs: Seq[Expression] = argumentExpressions.map(Expression(_))
        val fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem]) = t =>
          (retType, Compatibility.checkConformanceExt(true, params, t, true, false).problems)
        tuplizyCase(fun, exprs)
      }
      case Success(ScTypePolymorphicType(ScMethodType(retType, params, _), typeParams), _) => {
        val exprs: Seq[Expression] = argumentExpressions.map(expr => new Expression(expr))
        val fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem]) =
          ScalaPsiUtil.localTypeInferenceWithApplicability(retType, params, _, typeParams)
        tuplizyCase(fun, exprs)
      }
      case Success(ScTypePolymorphicType(ScFunctionType(retType, params), typeParams), _) => {
        val exprs: Seq[Expression] = argumentExpressions.map(expr => new Expression(expr))
        val fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem]) =
          ScalaPsiUtil.localTypeInferenceWithApplicability(retType, params.zipWithIndex.map {case (tp, i) => {
          new Parameter("v" + (i + 1), tp, false, false)
        }}, _, typeParams)
        tuplizyCase(fun, exprs)
      }
      case Success(tp: ScType, _) => ScalaPsiUtil.processTypeForUpdateOrApply(tp, this, false).getOrElse(Nothing) match {
        case ScFunctionType(retType: ScType, params: Seq[ScType]) => {
          val exprs: Seq[Expression] = argumentExpressionsIncludeUpdateCall.map(Expression(_))
          val fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem]) = t =>
            (retType, Compatibility.checkConformanceExt(true, params.zipWithIndex.map {case (tp, i) => {
              new Parameter("v" + (i + 1), tp, false, false)
            }}, t, true, false).problems)
          tuplizyCase(fun, exprs)
        }
        case ScMethodType(retType, params, _) => {
          val exprs: Seq[Expression] = argumentExpressionsIncludeUpdateCall.map(Expression(_))
          val fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem]) = t =>
            (retType, Compatibility.checkConformanceExt(true, params,
              t, true, false).problems)
          tuplizyCase(fun, exprs)
        }
        case ScTypePolymorphicType(ScMethodType(retType, params, _), typeParams) => {
          val exprs: Seq[Expression] = argumentExpressionsIncludeUpdateCall.map(expr => new Expression(expr))
          val fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem]) =
            ScalaPsiUtil.localTypeInferenceWithApplicability(retType, params, _, typeParams)
          tuplizyCase(fun, exprs)
        }
        case ScTypePolymorphicType(ScFunctionType(retType, params), typeParams) => {
          val exprs: Seq[Expression] = argumentExpressionsIncludeUpdateCall.map(expr => new Expression(expr))
          val fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem]) =
            ScalaPsiUtil.localTypeInferenceWithApplicability(retType, params.zipWithIndex.map {case (tp, i) => {
              new Parameter("v" + (i + 1), tp, false, false)
            }}, _, typeParams)
          tuplizyCase(fun, exprs)
        }
        case tp => {
          applicabilityProblemsVar = Seq(new DoesNotTakeParameters)
          tp
        }
      }
      case x => return x
    }

    Success(res, Some(this))
  }

  private var applicabilityProblemsVar: Seq[ApplicabilityProblem] = Seq.empty

  def applicationProblems: scala.Seq[ApplicabilityProblem] = {
    getType(TypingContext.empty) //update applicabilityProblemsVar if needs
    applicabilityProblemsVar
  }
}