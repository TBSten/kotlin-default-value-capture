package me.tbsten.defaultargcapture

import com.google.auto.service.AutoService
import me.tbsten.defaultargcapture.fir.DefaultArgOfFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/**
 * Entry point for the defaultArgOf compiler plugin (K2 compiler).
 *
 * Registers two compiler extensions that work together to process `defaultArgOf` calls:
 *
 * ### 1. FIR checker (frontend phase)
 * [DefaultArgOfFirExtensionRegistrar] adds validation during type analysis.
 * This catches errors early and reports them with accurate line numbers in the IDE.
 *
 * Example: if the user writes `defaultArgOf<String>("nonExistent", "x")`, the FIR checker
 * reports "Function 'nonExistent' not found." at the call site before IR lowering even begins.
 *
 * ### 2. IR transformer (backend phase)
 * [DefaultArgOfIrExtension] performs the actual replacement of `defaultArgOf(...)` calls
 * with the default value expressions from the referenced function parameters.
 *
 * Example: `defaultArgOf<String>(::greet, "name")` is replaced with `"World"` if
 * `fun greet(name: String = "World")`.
 */
@AutoService(CompilerPluginRegistrar::class)
class DefaultArgOfPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "me.tbsten.defaultargcapture"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(DefaultArgOfFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(DefaultArgOfIrExtension(configuration))
    }
}
