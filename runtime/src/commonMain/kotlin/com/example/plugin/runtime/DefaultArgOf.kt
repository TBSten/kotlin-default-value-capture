package com.example.plugin.runtime

/**
 * コンパイラプラグインによって差し替えられる。実行時に呼ばれることはない。
 * reified T にすることで呼び出し側の型注釈が不要になる。
 */
@Suppress("UNUSED_PARAMETER", "unused")
inline fun <reified T> defaultArgOf(funName: String, argName: String): T =
    error("This call must be replaced by the defaultArgOf compiler plugin")
