package com.example.plugin

import com.google.auto.service.AutoService
import com.example.plugin.fir.DefaultArgOfFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@AutoService(CompilerPluginRegistrar::class)
class DefaultArgOfPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "com.example.defaultarg"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(DefaultArgOfFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(DefaultArgOfIrExtension(configuration))
    }
}
