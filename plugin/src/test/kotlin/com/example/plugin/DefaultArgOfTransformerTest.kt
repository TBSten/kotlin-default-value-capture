package com.example.plugin

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class DefaultArgOfTransformerTest : FunSpec({

    fun compile(source: String, dumpIr: Boolean = false): JvmCompilationResult =
        KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Source.kt", source))
            compilerPluginRegistrars = listOf(DefaultArgOfPluginRegistrar())
            inheritClassPath = true
            jvmTarget = "21"
            messageOutputStream = System.out
            if (dumpIr) kotlincArguments = listOf("-Xphases-to-dump-after=IrVerification")
        }.compile()

    fun JvmCompilationResult.loadTopLevelField(name: String): Any? =
        classLoader.loadClass("SourceKt")
            .getDeclaredField(name)
            .also { it.isAccessible = true }
            .get(null)

    test("文字列リテラルのデフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = "hello") {}
            val v = defaultArgOf<String>(funName = "target", argName = "x")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("v") shouldBe "hello"
    }

    test("式形式のデフォルト値（123.toString()）が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = 123.toString()) {}
            val v = defaultArgOf<String>(funName = "target", argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.loadTopLevelField("v") shouldBe "123"
    }

    test("存在しない関数名はコンパイルエラーになる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            val v = defaultArgOf<String>(funName = "notExist", argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Function 'notExist' not found"
    }

    test("存在しないパラメータ名はコンパイルエラーになる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = "hi") {}
            val v = defaultArgOf<String>(funName = "target", argName = "notExist")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Parameter 'notExist' not found"
    }

    test("定数でない funName はコンパイルエラーになる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = "hi") {}
            val name = "target"
            val v = defaultArgOf<String>(funName = name, argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "must be a compile-time string constant"
    }

    // --- 関数参照ベース API ---

    test("関数参照で文字列リテラルのデフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = "hello") {}
            val v = defaultArgOf<String>(::target, "x")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("v") shouldBe "hello"
    }

    test("関数参照で式形式のデフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = 123.toString()) {}
            val v = defaultArgOf<String>(::target, "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.loadTopLevelField("v") shouldBe "123"
    }

    test("関数参照で存在しないパラメータ名はコンパイルエラーになる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = "hi") {}
            val v = defaultArgOf<String>(::target, "notExist")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Parameter 'notExist' not found"
    }

    test("関数参照でデフォルト値のないパラメータはコンパイルエラーになる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String) {}
            val v = defaultArgOf<String>(::target, "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "has no default value"
    }

    // --- メンバ関数 ---

    test("メンバ関数の関数参照でデフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            class MyClass {
                fun myMethod(x: String = "member-default") {}
            }
            val v = defaultArgOf<String>(MyClass::myMethod, "x")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("v") shouldBe "member-default"
    }

    test("extension function の関数参照でデフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun String.myExt(x: Int = 42) {}
            val v = defaultArgOf<Int>(String::myExt, "x")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("v") shouldBe 42
    }

    test("メンバ関数の関数参照で存在しないパラメータはコンパイルエラーになる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            class MyClass {
                fun myMethod(x: String = "hi") {}
            }
            val v = defaultArgOf<String>(MyClass::myMethod, "notExist")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Parameter 'notExist' not found"
    }

    // --- 文字列ベース API エラーケース ---

    test("デフォルト値のないパラメータはコンパイルエラーになる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String) {}
            val v = defaultArgOf<String>(funName = "target", argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "has no default value"
    }
})
