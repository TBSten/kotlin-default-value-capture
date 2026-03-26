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

    // --- 文字列ベース API: FQN / 曖昧マッチ ---

    test("FQN 指定でデフォルト値が展開される") {
        val result = compile(
            """
            package com.example.test
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = "fqn-default") {}
            val v = defaultArgOf<String>(funName = "com.example.test.target", argName = "x")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.classLoader.loadClass("com.example.test.SourceKt")
            .getDeclaredField("v")
            .also { it.isAccessible = true }
            .get(null) shouldBe "fqn-default"
    }

    test("short name で同名関数が複数ある場合はコンパイルエラーになる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = "a") {}
            object Foo { fun target(x: String = "b") {} }
            val v = defaultArgOf<String>(funName = "target", argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Ambiguous function name 'target'"
    }

    // --- エッジケース ---

    test("Int リテラルのデフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: Int = 42) {}
            val v = defaultArgOf<Int>(::target, "x")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("v") shouldBe 42
    }

    test("Boolean リテラルのデフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(enabled: Boolean = true) {}
            val v = defaultArgOf<Boolean>(::target, "enabled")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("v") shouldBe true
    }

    // TODO ラムダのデフォルト値は deepCopyWithSymbols でシンボルコピーの問題が発生する既知の制限
    //  将来的に IrFactory でラムダを再構築する対応を検討
    xtest("ラムダのデフォルト値はコンパイルエラーになる（既知の制限）") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(action: () -> String = { "lambda-result" }) {}
            val v = defaultArgOf<() -> String>(::target, "action")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        @Suppress("UNCHECKED_CAST")
        val fn = result.loadTopLevelField("v") as () -> String
        fn() shouldBe "lambda-result"
    }

    test("リスト生成式のデフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(items: List<String> = listOf("a", "b")) {}
            val v = defaultArgOf<List<String>>(::target, "items")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("v") shouldBe listOf("a", "b")
    }

    test("null デフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String? = null) {}
            val v = defaultArgOf<String?>(::target, "x")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("v") shouldBe null
    }

    test("defaultArgOf がラムダ内で使われる場合に展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = "inside-lambda") {}
            val v = run { defaultArgOf<String>(::target, "x") }
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("v") shouldBe "inside-lambda"
    }

    test("複数パラメータの関数から特定のデフォルト値を取得できる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(a: String = "first", b: Int = 99, c: Boolean = false) {}
            val va = defaultArgOf<String>(::target, "a")
            val vb = defaultArgOf<Int>(::target, "b")
            val vc = defaultArgOf<Boolean>(::target, "c")
            """.trimIndent()
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            throw AssertionError("Compilation failed:\n${result.messages}")
        }
        result.loadTopLevelField("va") shouldBe "first"
        result.loadTopLevelField("vb") shouldBe 99
        result.loadTopLevelField("vc") shouldBe false
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
