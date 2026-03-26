package com.example.plugin.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
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

        val funNameArg = args[0]
        val argNameArg = args[1]

        if (funNameArg !is FirLiteralExpression) {
            reporter.reportOn(funNameArg.source, DefaultArgOfErrors.NON_CONST_ARG, "funName")
            return
        }
        if (argNameArg !is FirLiteralExpression) {
            reporter.reportOn(argNameArg.source, DefaultArgOfErrors.NON_CONST_ARG, "argName")
            return
        }

        val funName = funNameArg.value as? String ?: return
        val argName = argNameArg.value as? String ?: return

        val funFqName = FqName(funName)
        val packageFqName = funFqName.parent()
        val shortName = funFqName.shortName()

        val funcSymbols = context.session.symbolProvider
            .getTopLevelFunctionSymbols(packageFqName, shortName)

        if (funcSymbols.isEmpty()) {
            reporter.reportOn(expression.source, DefaultArgOfErrors.FUNCTION_NOT_FOUND, funName)
            return
        }

        val funcSymbol = funcSymbols.first()
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
}
