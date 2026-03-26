package me.tbsten.defaultargcapture.testapp

import me.tbsten.defaultargcapture.runtime.defaultArgOf

fun myFunction(option1: String = 123.toString()) { /* ... */ }

class MyClass {
    fun myMethod(greeting: String = "hello") {}
}

fun main() {
    // 文字列ベース API（FQN 必須）
    val op1Default = defaultArgOf<String>(
        funName = "me.tbsten.defaultargcapture.testapp.myFunction",
        argName = "option1",
    )
    check(op1Default == "123") { "string-based: got $op1Default" }
    println("OK (string-based): $op1Default")

    // 関数参照ベース API
    val op1Ref = defaultArgOf<String>(::myFunction, "option1")
    check(op1Ref == "123") { "func-ref: got $op1Ref" }
    println("OK (func-ref): $op1Ref")

    // メンバ関数
    val memberDefault = defaultArgOf<String>(MyClass::myMethod, "greeting")
    check(memberDefault == "hello") { "member: got $memberDefault" }
    println("OK (member): $memberDefault")
}
