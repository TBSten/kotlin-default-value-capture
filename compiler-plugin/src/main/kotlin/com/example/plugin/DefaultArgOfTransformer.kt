package com.example.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

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
