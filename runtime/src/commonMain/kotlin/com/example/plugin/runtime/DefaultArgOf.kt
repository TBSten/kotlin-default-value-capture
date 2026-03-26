package com.example.plugin.runtime

/**
 * Captures the default value of a function parameter at compile time.
 *
 * The compiler plugin replaces this call with the actual default value expression
 * from the target function's parameter. This function is never executed at runtime.
 *
 * ### Basic usage — string literal default
 * ```kotlin
 * fun greet(name: String = "World") {}
 *
 * val nameDefault = defaultArgOf<String>(funName = "greet", argName = "name")
 * // nameDefault == "World"
 * ```
 *
 * ### Expression default values
 * Default values that are expressions (not just literals) are also captured:
 * ```kotlin
 * fun connect(timeout: String = (30 * 1000).toString()) {}
 *
 * val timeoutDefault = defaultArgOf<String>(funName = "connect", argName = "timeout")
 * // timeoutDefault == "30000"
 * ```
 *
 * ### Fully qualified name
 * When multiple functions share the same short name, use the fully qualified name
 * to disambiguate:
 * ```kotlin
 * package com.example
 * fun fetch(url: String = "https://example.com") {}
 *
 * val urlDefault = defaultArgOf<String>(
 *     funName = "com.example.fetch",
 *     argName = "url",
 * )
 * ```
 *
 * ### Compile-time errors
 * The following cases produce compile errors:
 * - `funName` or `argName` is not a compile-time string constant
 * - The specified function does not exist in the current module
 * - The specified parameter does not exist
 * - The parameter has no default value
 *
 * @param T the type of the default value
 * @param funName the name of the target function (short name or fully qualified name).
 *   If multiple functions share the same short name, use the fully qualified name.
 * @param argName the name of the parameter whose default value to capture
 * @return the default value expression, inlined at compile time
 * @see defaultArgOf(Function, String) for a type-safe alternative using function references
 */
@Suppress("UNUSED_PARAMETER", "unused")
inline fun <reified T> defaultArgOf(funName: String, argName: String): T =
    error("This call must be replaced by the defaultArgOf compiler plugin")

/**
 * Captures the default value of a function parameter at compile time using a function reference.
 *
 * This overload is **preferred** over the string-based variant because:
 * - It is refactoring-safe (renaming a function updates the reference automatically)
 * - It supports member functions and extension functions
 * - It does not require fully qualified names
 *
 * ### Top-level function
 * ```kotlin
 * fun greet(name: String = "World") {}
 *
 * val nameDefault = defaultArgOf<String>(::greet, "name")
 * // nameDefault == "World"
 * ```
 *
 * ### Member function (class method)
 * Use `ClassName::methodName` to reference a member function:
 * ```kotlin
 * class ApiClient {
 *     fun fetch(timeout: Int = 30_000) {}
 * }
 *
 * val timeoutDefault = defaultArgOf<Int>(ApiClient::fetch, "timeout")
 * // timeoutDefault == 30000
 * ```
 *
 * ### Extension function
 * ```kotlin
 * fun String.padOrTruncate(maxLength: Int = 80) {}
 *
 * val maxLenDefault = defaultArgOf<Int>(String::padOrTruncate, "maxLength")
 * // maxLenDefault == 80
 * ```
 *
 * ### Compile-time errors
 * The following cases produce compile errors:
 * - `argName` is not a compile-time string constant
 * - The specified parameter does not exist on the referenced function
 * - The parameter has no default value
 *
 * @param T the type of the default value
 * @param func a callable reference to the target function (e.g. `::greet`, `MyClass::method`)
 * @param argName the name of the parameter whose default value to capture
 * @return the default value expression, inlined at compile time
 */
@Suppress("UNUSED_PARAMETER", "unused")
inline fun <reified T> defaultArgOf(func: Function<*>, argName: String): T =
    error("This call must be replaced by the defaultArgOf compiler plugin")
