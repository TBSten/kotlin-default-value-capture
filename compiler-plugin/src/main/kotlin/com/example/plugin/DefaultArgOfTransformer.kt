package com.example.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * IR transformer that replaces `defaultArgOf(...)` calls with the actual default value expressions.
 *
 * ### Resolution strategies
 *
 * **Function reference overload** (`defaultArgOf(::greet, "name")`):
 * The first argument is an `IrFunctionReference`, so the target function is resolved
 * directly from the reference symbol. This works for top-level functions, member functions,
 * and extension functions.
 *
 * **String-based overload** (`defaultArgOf("greet", "name")`):
 * The target function is looked up by name within the [IrModuleFragment].
 * Both short names and fully qualified names are supported. If a short name matches
 * multiple functions, a compile error is reported with the list of FQN candidates.
 *
 * ### Replacement process
 * 1. Resolve the target function (via function reference or name lookup)
 * 2. Find the named parameter and extract its `defaultValue` IR node
 * 3. Deep-copy the expression with [deepCopyWithSymbols] to avoid symbol conflicts
 * 4. Recursively transform the copy (handles nested `defaultArgOf` calls)
 *
 * ### Error handling
 * Errors are wrapped in [DefaultArgOfPluginException], caught here, and reported
 * via [MessageCollector] as compiler errors. The original call expression is preserved
 * to allow the compiler to continue reporting other errors.
 */
class DefaultArgOfTransformer(
    private val context: IrPluginContext,
    private val module: IrModuleFragment,
    private val messageCollector: MessageCollector,
) : IrElementTransformerVoid() {

    private val defaultArgOfSymbols by lazy {
        context.referenceFunctions(
            CallableId(FqName("com.example.plugin.runtime"), Name.identifier("defaultArgOf"))
        ).toSet()
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (defaultArgOfSymbols.isEmpty()) return super.visitCall(expression)
        if (expression.symbol !in defaultArgOfSymbols) return super.visitCall(expression)

        return try {
            replaceWithDefaultValue(expression)
        } catch (e: DefaultArgOfPluginException) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                e.message ?: "defaultArgOf plugin error",
            )
            super.visitCall(expression)
        }
    }

    private fun replaceWithDefaultValue(expression: IrCall): IrExpression {
        val targetFunction = resolveTargetFunction(expression)
        val argName = expression.getStringArgOrThrow("argName")

        val param = targetFunction.valueOnlyParameters
            .firstOrNull { it.name.asString() == argName }
            ?: throw DefaultArgOfPluginException(
                "Parameter '$argName' not found in '${targetFunction.name}'. " +
                    "Available: ${targetFunction.valueOnlyParameters.joinToString { it.name.asString() }}"
            )

        val defaultValue = param.defaultValue
            ?: throw DefaultArgOfPluginException(
                "Parameter '$argName' in '${targetFunction.name}' has no default value."
            )

        val copied = defaultValue.expression.deepCopyWithSymbols(targetFunction)
        return copied.transform(this, null)
    }

    private fun resolveTargetFunction(expression: IrCall): IrSimpleFunction {
        val funcRef = expression.findFunctionReferenceArg()
        if (funcRef != null) {
            val owner = funcRef.symbol.owner
            return owner as? IrSimpleFunction
                ?: throw DefaultArgOfPluginException(
                    "Referenced function '${owner.name}' is not a simple function."
                )
        }
        val funName = expression.getStringArgOrThrow("funName")
        return module.findFunctionOrThrow(funName)
    }
}
