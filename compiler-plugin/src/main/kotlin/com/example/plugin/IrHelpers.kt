package com.example.plugin

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Extracts a compile-time string constant from a named argument of a `defaultArgOf` call.
 *
 * ### Example
 * For `defaultArgOf<String>(funName = "greet", argName = "name")`:
 * ```kotlin
 * val funName = expression.getStringArgOrThrow("funName") // "greet"
 * val argName = expression.getStringArgOrThrow("argName") // "name"
 * ```
 *
 * @param paramName the parameter name to look up (e.g. `"funName"`, `"argName"`)
 * @return the string constant value
 * @throws DefaultArgOfPluginException if the parameter is missing, not provided, or not a string constant
 */
internal fun IrCall.getStringArgOrThrow(paramName: String): String {
    val param = symbol.owner.parameters
        .firstOrNull { it.name.asString() == paramName }
        ?: throw DefaultArgOfPluginException(
            "Parameter '$paramName' not found in ${symbol.owner.name}. " +
                "Expected: funName, argName"
        )
    val expr = arguments[param]
        ?: throw DefaultArgOfPluginException(
            "'$paramName' argument is missing in defaultArgOf call"
        )
    if (expr !is IrConst || expr.kind != IrConstKind.String) {
        throw DefaultArgOfPluginException(
            "'$paramName' must be a compile-time string constant, " +
                "but got ${expr::class.simpleName}: ${expr.dump()}"
        )
    }
    return expr.value as String
}

/**
 * Searches the module for a function matching [funName].
 *
 * ### Resolution rules
 * - If [funName] contains `.`, it is treated as a **fully qualified name** (exact match).
 *   Example: `"com.example.greet"` matches only `com.example.greet`.
 * - Otherwise, it matches by **short name**. If multiple candidates are found, an error
 *   is thrown with the list of FQN candidates to help the user disambiguate.
 *
 * ### Examples
 * ```kotlin
 * // FQN — exact match
 * module.findFunctionOrThrow("com.example.greet")
 *
 * // Short name — matches if unique in module
 * module.findFunctionOrThrow("greet")
 *
 * // Short name — throws if ambiguous
 * // "Ambiguous function name 'greet'. Candidates: com.example.greet, com.other.greet"
 * ```
 *
 * @param funName function name (short name or fully qualified name)
 * @return the matching [IrSimpleFunction]
 * @throws DefaultArgOfPluginException if no function is found or the name is ambiguous
 */
internal fun IrModuleFragment.findFunctionOrThrow(funName: String): IrSimpleFunction {
    val isFqn = funName.contains('.')
    val candidates = mutableListOf<IrSimpleFunction>()
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)
        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            if (declaration.kotlinFqName.asString() == funName) {
                candidates.add(declaration)
            } else if (!isFqn && declaration.name.asString() == funName) {
                candidates.add(declaration)
            }
            super.visitSimpleFunction(declaration)
        }
    })
    if (candidates.isEmpty()) {
        throw DefaultArgOfPluginException(
            "Function '$funName' not found in module '${name}'. " +
                "Note: only functions in the same module can be referenced."
        )
    }
    if (candidates.size > 1) {
        val fqns = candidates.joinToString { it.kotlinFqName.asString() }
        throw DefaultArgOfPluginException(
            "Ambiguous function name '$funName'. " +
                "Use fully qualified name to disambiguate. Candidates: $fqns"
        )
    }
    return candidates.single()
}

/**
 * Extracts the `func` argument as an [IrFunctionReference] if present.
 *
 * This is used to distinguish between the two `defaultArgOf` overloads:
 * - **Function reference overload**: `defaultArgOf(::greet, "name")` — returns the [IrFunctionReference]
 * - **String-based overload**: `defaultArgOf("greet", "name")` — returns `null`
 *
 * @return the function reference, or `null` if the call uses the string-based overload
 */
internal fun IrCall.findFunctionReferenceArg(): IrFunctionReference? {
    val funcParam = symbol.owner.parameters
        .firstOrNull { it.name.asString() == "func" }
        ?: return null
    return arguments[funcParam] as? IrFunctionReference
}

/**
 * Returns the value parameters excluding hidden parameters (dispatch/extension receivers).
 *
 * In Kotlin 2.3.x IR, `parameters` includes all parameters (including hidden ones
 * for dispatch receiver, extension receiver, etc.). This property filters them out,
 * returning only the user-visible value parameters.
 *
 * ### Example
 * For `class Foo { fun bar(x: String, y: Int) {} }`:
 * - `parameters` includes `[$this: Foo, x: String, y: Int]`
 * - `valueOnlyParameters` returns `[x: String, y: Int]`
 */
internal val IrSimpleFunction.valueOnlyParameters: List<IrValueParameter>
    get() = parameters.filter { !it.isHidden }
