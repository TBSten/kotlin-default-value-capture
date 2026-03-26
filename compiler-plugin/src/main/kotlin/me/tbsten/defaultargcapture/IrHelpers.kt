package me.tbsten.defaultargcapture

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.atomic.AtomicInteger

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
 * Searches the module for a function matching [funName] by fully qualified name.
 *
 * The [funName] must be a fully qualified name (e.g. `"com.example.greet"`).
 * Short names without a package are rejected with a compile error.
 *
 * ### Examples
 * ```kotlin
 * // FQN — exact match
 * module.findFunctionOrThrow("com.example.greet") // OK
 *
 * // Short name — compile error
 * module.findFunctionOrThrow("greet")
 * // → "'funName' must be a fully qualified name (e.g. 'com.example.greet'), but got 'greet'."
 * ```
 *
 * @param funName fully qualified function name
 * @return the matching [IrSimpleFunction]
 * @throws DefaultArgOfPluginException if the name is not FQN, or no function is found
 */
internal fun IrModuleFragment.findFunctionOrThrow(funName: String): IrSimpleFunction {
    if (!funName.contains('.')) {
        throw DefaultArgOfPluginException(
            "'funName' must be a fully qualified name (e.g. 'com.example.$funName'), " +
                "but got '$funName'."
        )
    }
    var result: IrSimpleFunction? = null
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)
        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            if (declaration.kotlinFqName.asString() == funName) {
                result = declaration
            }
            super.visitSimpleFunction(declaration)
        }
    })
    return result
        ?: throw DefaultArgOfPluginException(
            "Function '$funName' not found in module '${name}'. " +
                "Note: only functions in the same module can be referenced."
        )
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

private val lambdaRenameCounter = AtomicInteger(0)

/**
 * Renames lambda/anonymous function declarations in a deep-copied expression to avoid
 * JVM signature clashes with the originals.
 *
 * After `deepCopyWithSymbols`, lambda backing functions like `target$lambda$0` get new IR
 * symbols but keep the same JVM name, causing "Platform declaration clash". This function
 * walks the copied expression tree and renames:
 * - `IrFunctionExpression.function` (lambda/anonymous function declarations)
 * - Any `IrSimpleFunction` with `$lambda` or `$anonymous` in the name
 *
 * ### Example
 * `target$lambda$0` → `defaultArgOf$0$lambda`
 */
internal fun IrExpression.renameCopiedLambdas() {
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

        override fun visitFunctionExpression(expression: IrFunctionExpression) {
            val id = lambdaRenameCounter.getAndIncrement()
            expression.function.name = Name.identifier("defaultArgOf\$$id\$lambda")
            super.visitFunctionExpression(expression)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            val name = declaration.name.asString()
            if ("\$lambda" in name || "\$anonymous" in name) {
                val id = lambdaRenameCounter.getAndIncrement()
                declaration.name = Name.identifier("defaultArgOf\$$id\$lambda")
            }
            super.visitSimpleFunction(declaration)
        }
    })
}
