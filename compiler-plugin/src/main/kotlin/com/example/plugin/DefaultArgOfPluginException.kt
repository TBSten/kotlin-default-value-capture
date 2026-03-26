package com.example.plugin

/**
 * Exception thrown during `defaultArgOf` IR processing to signal a compile-time error.
 *
 * Caught by [DefaultArgOfTransformer.visitCall] and reported to the user via
 * [MessageCollector][org.jetbrains.kotlin.cli.common.messages.MessageCollector]
 * as a compiler error.
 *
 * ### Example error scenarios
 * - `DefaultArgOfPluginException("Function 'foo' not found in module ...")`
 * - `DefaultArgOfPluginException("Parameter 'bar' not found in 'foo'. Available: x, y")`
 * - `DefaultArgOfPluginException("'funName' must be a compile-time string constant, ...")`
 */
class DefaultArgOfPluginException(message: String) : Exception(message)
