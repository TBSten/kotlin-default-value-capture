package me.tbsten.defaultargcapture.fir

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

/**
 * FIR-phase checker that validates `defaultArgOf` calls during frontend analysis.
 *
 * Runs before the IR phase and provides early error reporting with accurate line numbers.
 *
 * ### Validated cases
 *
 * **Non-constant arguments:**
 * ```kotlin
 * val name = "greet"
 * defaultArgOf<String>(name, "x") // ERROR: 'funName' must be a compile-time string constant
 * ```
 *
 * **Non-existent function (string-based):**
 * ```kotlin
 * defaultArgOf<String>("nonExistent", "x") // ERROR: Function 'nonExistent' not found
 * ```
 *
 * **Non-existent parameter:**
 * ```kotlin
 * fun greet(name: String = "World") {}
 * defaultArgOf<String>(::greet, "typo") // ERROR: Parameter 'typo' not found
 * ```
 *
 * **No default value:**
 * ```kotlin
 * fun greet(name: String) {} // no default
 * defaultArgOf<String>(::greet, "name") // ERROR: Parameter 'name' has no default value
 * ```
 *
 * ### Best-effort design
 * This checker is wrapped in a try-catch. If an unexpected exception occurs (e.g. due to
 * incomplete resolution in certain environments), it is silently ignored and the IR phase
 * reports the equivalent error as a fallback.
 */
class DefaultArgOfFirChecker(private val session: FirSession) :
    FirExpressionChecker<FirFunctionCall>(MppCheckerKind.Platform) {

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
        if (!funName.contains('.')) {
            reporter.reportOn(expression.source, DefaultArgOfErrors.NOT_FQN, funName)
            return null
        }
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
