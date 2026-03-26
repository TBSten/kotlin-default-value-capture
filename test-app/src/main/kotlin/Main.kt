import com.example.plugin.runtime.defaultArgOf

fun myFunction(option1: String = 123.toString()) { /* ... */ }

fun main() {
    // 文字列ベース API
    val op1Default = defaultArgOf<String>(funName = "myFunction", argName = "option1")
    check(op1Default == "123") { "string-based: got $op1Default" }
    println("OK (string-based): $op1Default")

    // 関数参照ベース API
    val op1Ref = defaultArgOf<String>(::myFunction, "option1")
    check(op1Ref == "123") { "func-ref: got $op1Ref" }
    println("OK (func-ref): $op1Ref")
}
