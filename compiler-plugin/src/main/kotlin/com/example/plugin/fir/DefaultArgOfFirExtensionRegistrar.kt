package com.example.plugin.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * Registers FIR-phase checkers for the defaultArgOf plugin.
 *
 * This registrar is called during the K2 compiler's frontend phase and adds
 * [DefaultArgOfAdditionalCheckers], which validates `defaultArgOf` calls before
 * IR lowering begins.
 *
 * ### Why a FIR checker?
 * IR-phase errors often lack accurate line numbers and are harder to distinguish
 * from type errors. By validating in the FIR phase, users see clear, actionable
 * errors in the IDE with precise source locations.
 *
 * ### Registration flow
 * ```
 * DefaultArgOfPluginRegistrar
 *   └─ FirExtensionRegistrarAdapter.registerExtension(DefaultArgOfFirExtensionRegistrar())
 *        └─ configurePlugin() → +::DefaultArgOfAdditionalCheckers
 *             └─ DefaultArgOfFirChecker (expression checker)
 * ```
 */
class DefaultArgOfFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::DefaultArgOfAdditionalCheckers
    }
}

/**
 * Provides the [DefaultArgOfFirChecker] as an additional expression checker
 * for `FirFunctionCall` nodes.
 */
class DefaultArgOfAdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirExpressionChecker<FirFunctionCall>> =
            setOf(DefaultArgOfFirChecker(session))
    }
}
