package com.example.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

@AutoService(CommandLineProcessor::class)
class DefaultArgOfCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.example.defaultarg"
    override val pluginOptions: Collection<AbstractCliOption> = emptyList()
}
