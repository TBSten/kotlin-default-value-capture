package com.example.plugin.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

object DefaultArgOfErrors {
    val Renderer: BaseDiagnosticRendererFactory = RendererFactory

    val NON_CONST_ARG = factory1("DEFAULT_ARG_OF_NON_CONST_ARG")
    val FUNCTION_NOT_FOUND = factory1("DEFAULT_ARG_OF_FUNCTION_NOT_FOUND")
    val PARAMETER_NOT_FOUND = factory1("DEFAULT_ARG_OF_PARAMETER_NOT_FOUND")
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
            map.put(FUNCTION_NOT_FOUND, "Function ''{0}'' not found.", CommonRenderers.STRING)
            map.put(PARAMETER_NOT_FOUND, "Parameter ''{0}'' not found.", CommonRenderers.STRING)
            map.put(NO_DEFAULT_VALUE, "Parameter ''{0}'' has no default value.", CommonRenderers.STRING)
        }
    }
}
