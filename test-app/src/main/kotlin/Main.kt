import com.example.plugin.runtime.defaultArgOf

fun myFunction(option1: String = 123.toString()) { /* ... */ }

fun main() {
    val op1Default = defaultArgOf<String>(funName = "myFunction", argName = "option1")
    check(op1Default == "123") { "got: $op1Default" }
    println("OK: $op1Default")
}
