package com.example.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * IR generation extension that kicks off the `defaultArgOf` call replacement.
 *
 * Creates a [DefaultArgOfTransformer] and runs it over the entire [IrModuleFragment],
 * replacing every `defaultArgOf(...)` call site with the default value expression
 * from the target function parameter.
 *
 * ### Transformation example
 * Given:
 * ```kotlin
 * fun greet(name: String = "World") {}
 * val x = defaultArgOf<String>(::greet, "name")
 * ```
 * After transformation, the IR for `x` becomes equivalent to:
 * ```kotlin
 * val x = "World"
 * ```
 *
 * ### Error reporting
 * Errors (e.g. missing function, missing parameter) are reported via [MessageCollector],
 * which is obtained from the [CompilerConfiguration].
 */
class DefaultArgOfIrExtension(
    private val configuration: CompilerConfiguration,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val messageCollector = configuration.get(
            CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE,
        )
        moduleFragment.transform(
            DefaultArgOfTransformer(pluginContext, moduleFragment, messageCollector),
            null,
        )
    }
}
