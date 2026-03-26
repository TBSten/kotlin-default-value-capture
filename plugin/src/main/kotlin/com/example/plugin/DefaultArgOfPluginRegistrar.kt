package com.example.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(CompilerPluginRegistrar::class)
class DefaultArgOfPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "com.example.defaultarg"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // TODO: IrGenerationExtension.registerExtension (task-006 で対応)
    }
}
