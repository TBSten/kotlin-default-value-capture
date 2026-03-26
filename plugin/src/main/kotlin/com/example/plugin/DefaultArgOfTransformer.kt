package com.example.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class DefaultArgOfTransformer(
    private val context: IrPluginContext,
    private val module: IrModuleFragment,
    private val messageCollector: MessageCollector,
) : IrElementTransformerVoid() {

    private val defaultArgOfSymbol by lazy {
        context.referenceFunctions(
            CallableId(FqName("com.example.plugin.runtime"), Name.identifier("defaultArgOf"))
        ).singleOrNull()
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val symbol = defaultArgOfSymbol ?: return super.visitCall(expression)
        if (expression.symbol != symbol) return super.visitCall(expression)

        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "[DefaultArgOf] HIT: ${expression.dump()}"
        )
        return super.visitCall(expression)
    }
}
