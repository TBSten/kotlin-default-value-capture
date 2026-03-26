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

internal fun IrCall.findFunctionReferenceArg(): IrFunctionReference? {
    val funcParam = symbol.owner.parameters
        .firstOrNull { it.name.asString() == "func" }
        ?: return null
    return arguments[funcParam] as? IrFunctionReference
}

internal val IrSimpleFunction.valueOnlyParameters: List<IrValueParameter>
    get() = parameters.filter { !it.isHidden }
