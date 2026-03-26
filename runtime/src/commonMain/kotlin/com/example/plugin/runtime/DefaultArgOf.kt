package com.example.plugin.runtime

/**
 * コンパイラプラグインによって差し替えられる。実行時に呼ばれることはない。
 * reified T にすることで呼び出し側の型注釈が不要になる。
 */
@Suppress("UNUSED_PARAMETER", "unused")
inline fun <reified T> defaultArgOf(funName: String, argName: String): T =
    error("This call must be replaced by the defaultArgOf compiler plugin")

/**
 * 関数参照ベースのオーバーロード。リファクタリング耐性が高い。
 * コンパイラプラグインが [func] から対象関数を解決し、デフォルト値で差し替える。
 */
@Suppress("UNUSED_PARAMETER", "unused")
inline fun <reified T> defaultArgOf(func: Function<*>, argName: String): T =
    error("This call must be replaced by the defaultArgOf compiler plugin")
