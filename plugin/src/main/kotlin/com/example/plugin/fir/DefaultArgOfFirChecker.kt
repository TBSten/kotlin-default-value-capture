package com.example.plugin.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.FqName

class DefaultArgOfFirChecker(private val session: FirSession) :
    FirExpressionChecker<FirFunctionCall>(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        try {
            doCheck(expression)
        } catch (_: Exception) {
            // FIR チェッカーは best-effort。IR フェーズで同等のエラーが報告される。
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun doCheck(expression: FirFunctionCall) {
        val calleeRef = expression.calleeReference
        if (calleeRef.name.asString() != "defaultArgOf") return

        val args = expression.argumentList.arguments
        if (args.size < 2) return

        val firstArg = args[0]
        val argNameArg = args[1]

        // argName は常にリテラル文字列でなければならない
        if (argNameArg !is FirLiteralExpression) {
            reporter.reportOn(argNameArg.source, DefaultArgOfErrors.NON_CONST_ARG, "argName")
            return
        }
        val argName = argNameArg.value as? String ?: return

        // 第1引数が callable reference か文字列かで分岐
        val funcSymbol = when (firstArg) {
            is FirCallableReferenceAccess -> {
                val ref = firstArg.calleeReference as? FirResolvedNamedReference ?: return
                ref.resolvedSymbol as? FirFunctionSymbol<*> ?: return
            }
            is FirLiteralExpression -> {
                val funName = firstArg.value as? String ?: return
                resolveByStringName(expression, funName) ?: return
            }
            else -> {
                reporter.reportOn(firstArg.source, DefaultArgOfErrors.NON_CONST_ARG, "funName")
                return
            }
        }

        val param = funcSymbol.valueParameterSymbols
            .firstOrNull { it.name.asString() == argName }

        if (param == null) {
            reporter.reportOn(expression.source, DefaultArgOfErrors.PARAMETER_NOT_FOUND, argName)
            return
        }

        if (!param.hasDefaultValue) {
            reporter.reportOn(expression.source, DefaultArgOfErrors.NO_DEFAULT_VALUE, argName)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun resolveByStringName(
        expression: FirFunctionCall,
        funName: String,
    ): FirFunctionSymbol<*>? {
        val funFqName = FqName(funName)
        val packageFqName = funFqName.parent()
        val shortName = funFqName.shortName()

        val funcSymbols = context.session.symbolProvider
            .getTopLevelFunctionSymbols(packageFqName, shortName)

        if (funcSymbols.isEmpty()) {
            reporter.reportOn(expression.source, DefaultArgOfErrors.FUNCTION_NOT_FOUND, funName)
            return null
        }

        return funcSymbols.first()
    }
}
