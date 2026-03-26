package me.tbsten.defaultargcapture

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.assertions.assertSoftly
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

    fun JvmCompilationResult.shouldCompileOk(): JvmCompilationResult {
        if (exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n$messages")
        }
        return this
    }

    fun JvmCompilationResult.loadTopLevelField(name: String, pkg: String? = null): Any? {
        val className = if (pkg != null) "$pkg.SourceKt" else "SourceKt"
        return classLoader.loadClass(className)
            .getDeclaredField(name)
            .also { it.isAccessible = true }
            .get(null)
    }

    // --- 文字列ベース API（FQN 必須） ---

    test("文字列リテラルのデフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            package com.example.test
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = "hello") {}
            val v = defaultArgOf<String>(funName = "com.example.test.target", argName = "x")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v", pkg = "com.example.test") shouldBe "hello"
    }

    test("式形式のデフォルト値（123.toString()）が展開される") {
        val result = compile(
            // language=kotlin
            """
            package com.example.test
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = 123.toString()) {}
            val v = defaultArgOf<String>(funName = "com.example.test.target", argName = "x")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v", pkg = "com.example.test") shouldBe "123"
    }

    test("存在しない関数名はコンパイルエラーになる") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            val v = defaultArgOf<String>(funName = "com.example.notExist", argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Function 'com.example.notExist' not found"
    }

    test("存在しないパラメータ名はコンパイルエラーになる") {
        val result = compile(
            // language=kotlin
            """
            package com.example.test
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = "hi") {}
            val v = defaultArgOf<String>(funName = "com.example.test.target", argName = "notExist")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Parameter 'notExist' not found"
    }

    test("定数でない funName はコンパイルエラーになる") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = "hi") {}
            val name = "com.example.target"
            val v = defaultArgOf<String>(funName = name, argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "must be a compile-time string constant"
    }

    test("short name はコンパイルエラーになる（FQN 必須）") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = "hi") {}
            val v = defaultArgOf<String>(funName = "target", argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "must be a fully qualified name"
    }

    // --- 関数参照ベース API ---

    test("関数参照で文字列リテラルのデフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = "hello") {}
            val v = defaultArgOf<String>(::target, "x")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v") shouldBe "hello"
    }

    test("関数参照で式形式のデフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = 123.toString()) {}
            val v = defaultArgOf<String>(::target, "x")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v") shouldBe "123"
    }

    test("関数参照で存在しないパラメータ名はコンパイルエラーになる") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = "hi") {}
            val v = defaultArgOf<String>(::target, "notExist")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Parameter 'notExist' not found"
    }

    test("関数参照でデフォルト値のないパラメータはコンパイルエラーになる") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
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
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            class MyClass {
                fun myMethod(x: String = "member-default") {}
            }
            val v = defaultArgOf<String>(MyClass::myMethod, "x")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v") shouldBe "member-default"
    }

    test("extension function の関数参照でデフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun String.myExt(x: Int = 42) {}
            val v = defaultArgOf<Int>(String::myExt, "x")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v") shouldBe 42
    }

    test("メンバ関数の関数参照で存在しないパラメータはコンパイルエラーになる") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            class MyClass {
                fun myMethod(x: String = "hi") {}
            }
            val v = defaultArgOf<String>(MyClass::myMethod, "notExist")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Parameter 'notExist' not found"
    }

    // --- 文字列ベース API: FQN ---

    test("FQN 指定でデフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            package com.example.test
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = "fqn-default") {}
            val v = defaultArgOf<String>(funName = "com.example.test.target", argName = "x")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v", pkg = "com.example.test") shouldBe "fqn-default"
    }

    // --- エッジケース ---

    test("Int リテラルのデフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: Int = 42) {}
            val v = defaultArgOf<Int>(::target, "x")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v") shouldBe 42
    }

    test("Boolean リテラルのデフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(enabled: Boolean = true) {}
            val v = defaultArgOf<Boolean>(::target, "enabled")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v") shouldBe true
    }

    test("ラムダのデフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(action: () -> String = { "lambda-result" }) {}
            val v = defaultArgOf<() -> String>(::target, "action")
            """.trimIndent()
        )

        result.shouldCompileOk()
        @Suppress("UNCHECKED_CAST")
        val fn = result.loadTopLevelField("v") as () -> String
        fn() shouldBe "lambda-result"
    }

    test("リスト生成式のデフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(items: List<String> = listOf("a", "b")) {}
            val v = defaultArgOf<List<String>>(::target, "items")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v") shouldBe listOf("a", "b")
    }

    test("null デフォルト値が展開される") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String? = null) {}
            val v = defaultArgOf<String?>(::target, "x")
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v") shouldBe null
    }

    test("defaultArgOf がラムダ内で使われる場合に展開される") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String = "inside-lambda") {}
            val v = run { defaultArgOf<String>(::target, "x") }
            """.trimIndent()
        )

        result.shouldCompileOk()
        result.loadTopLevelField("v") shouldBe "inside-lambda"
    }

    test("複数パラメータの関数から特定のデフォルト値を取得できる") {
        val result = compile(
            // language=kotlin
            """
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(a: String = "first", b: Int = 99, c: Boolean = false) {}
            val va = defaultArgOf<String>(::target, "a")
            val vb = defaultArgOf<Int>(::target, "b")
            val vc = defaultArgOf<Boolean>(::target, "c")
            """.trimIndent()
        )

        result.shouldCompileOk()
        assertSoftly {
            result.loadTopLevelField("va") shouldBe "first"
            result.loadTopLevelField("vb") shouldBe 99
            result.loadTopLevelField("vc") shouldBe false
        }
    }

    // --- 文字列ベース API エラーケース ---

    test("デフォルト値のないパラメータはコンパイルエラーになる（文字列ベース）") {
        val result = compile(
            // language=kotlin
            """
            package com.example.test
            import me.tbsten.defaultargcapture.runtime.defaultArgOf
            fun target(x: String) {}
            val v = defaultArgOf<String>(funName = "com.example.test.target", argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "has no default value"
    }
})
