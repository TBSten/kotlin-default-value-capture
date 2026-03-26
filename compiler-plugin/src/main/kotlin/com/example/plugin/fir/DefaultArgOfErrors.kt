package com.example.plugin.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

/**
 * Diagnostic factory definitions for defaultArgOf FIR checker errors.
 *
 * Each factory produces a compile-time error diagnostic with a human-readable message.
 * These diagnostics are reported by [DefaultArgOfFirChecker] during the FIR analysis phase.
 *
 * ### Diagnostic types
 *
 * | Factory | When reported | Example message |
 * |---|---|---|
 * | [NON_CONST_ARG] | Argument is not a string literal | `'funName' must be a compile-time string constant.` |
 * | [FUNCTION_NOT_FOUND] | Function does not exist | `Function 'foo' not found.` |
 * | [PARAMETER_NOT_FOUND] | Parameter does not exist | `Parameter 'bar' not found.` |
 * | [NOT_FQN] | funName is not a fully qualified name | `'funName' must be a fully qualified name, but got 'foo'.` |
 * | [NO_DEFAULT_VALUE] | Parameter has no default | `Parameter 'bar' has no default value.` |
 */
object DefaultArgOfErrors {
    val Renderer: BaseDiagnosticRendererFactory = RendererFactory

    /** Reported when `funName` or `argName` is not a compile-time string constant. */
    val NON_CONST_ARG = factory1("DEFAULT_ARG_OF_NON_CONST_ARG")

    /** Reported when `funName` is not a fully qualified name (missing package). */
    val NOT_FQN = factory1("DEFAULT_ARG_OF_NOT_FQN")

    /** Reported when the specified function cannot be found via FIR symbol provider. */
    val FUNCTION_NOT_FOUND = factory1("DEFAULT_ARG_OF_FUNCTION_NOT_FOUND")

    /** Reported when the specified parameter does not exist on the target function. */
    val PARAMETER_NOT_FOUND = factory1("DEFAULT_ARG_OF_PARAMETER_NOT_FOUND")

    /** Reported when the specified parameter has no default value. */
    val NO_DEFAULT_VALUE = factory1("DEFAULT_ARG_OF_NO_DEFAULT_VALUE")

    private fun factory1(name: String) = KtDiagnosticFactory1<String>(
        name,
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        DefaultArgOfErrors::class,
        RendererFactory,
    )

    private object RendererFactory : BaseDiagnosticRendererFactory() {
        override val MAP by KtDiagnosticFactoryToRendererMap("DefaultArgOf") { map ->
            map.put(NON_CONST_ARG, "''{0}'' must be a compile-time string constant.", CommonRenderers.STRING)
            map.put(NOT_FQN, "'funName' must be a fully qualified name (e.g. 'com.example.{0}'), but got ''{0}''.", CommonRenderers.STRING)
            map.put(FUNCTION_NOT_FOUND, "Function ''{0}'' not found.", CommonRenderers.STRING)
            map.put(PARAMETER_NOT_FOUND, "Parameter ''{0}'' not found.", CommonRenderers.STRING)
            map.put(NO_DEFAULT_VALUE, "Parameter ''{0}'' has no default value.", CommonRenderers.STRING)
        }
    }
}
