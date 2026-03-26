# kotlin-default-value-capture

A Kotlin compiler plugin that lets you retrieve function default argument values at compile time.

## Motivation

Kotlin allows defining default arguments for function parameters, but there is no standard way to access those values programmatically.
For example, if you want to display default values in a CLI help message or a configuration framework, you would need to either duplicate the values or rely on reflection.

This plugin replaces `defaultArgOf` function calls with **the actual default value expressions** at compile time, providing a type-safe solution with zero runtime cost.

## Setup

### Gradle

```kotlin
// build.gradle.kts
plugins {
    id("me.tbsten.defaultargcapture") version "0.1.0-alpha01"
}
```

Simply applying the plugin automatically adds the runtime library dependency and configures the compiler plugin.

## Usage

```kotlin
import com.example.plugin.runtime.defaultArgOf

fun greet(name: String = "World") {
    println("Hello, $name!")
}

fun main() {
    // Replaced with "World" at compile time
    val defaultName = defaultArgOf<String>(funName = "greet", argName = "name")
    println("Default value: $defaultName") // => Default value: World
}
```

### API

```kotlin
inline fun <reified T> defaultArgOf(funName: String, argName: String): T
```

| Parameter | Description |
|---|---|
| `T` | The type of the default value |
| `funName` | Fully qualified name of the target function |
| `argName` | Name of the target parameter |

- All arguments must be **compile-time constants**
- Specifying a non-existent function/parameter name or a parameter without a default value results in a **compile error**

### Example: CLI Help

```kotlin
fun serve(port: Int = 8080, host: String = "localhost") { /* ... */ }

fun printHelp() {
    val defaultPort = defaultArgOf<Int>(funName = "serve", argName = "port")
    val defaultHost = defaultArgOf<String>(funName = "serve", argName = "host")
    println("  --port  Port number (default: $defaultPort)")
    println("  --host  Hostname    (default: $defaultHost)")
}
```

## Known Limitations

| Limitation | Note |
|---|---|
| Only functions in the same module can be referenced | Default values from other modules are not available in the IR |
| `funName` requires a fully qualified name | Short names may cause ambiguity |

## How It Works

1. You call `defaultArgOf<T>(funName = "f", argName = "x")` in your code
2. The FIR checker validates the arguments during type analysis and reports compile errors if invalid
3. The IR Transformer finds the call and replaces it with the default value IR expression from the target function

## Requirements

- Kotlin 2.x (K2 compiler)

## License

MIT
