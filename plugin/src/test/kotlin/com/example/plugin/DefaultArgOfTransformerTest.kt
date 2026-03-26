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
            // TODO: compilerPluginRegistrars に PluginRegistrar を追加（task-005 で対応）
            inheritClassPath = true
            messageOutputStream = System.out
            if (dumpIr) kotlincArguments = listOf("-Xphases-to-dump-after=IrVerification")
        }.compile()

    fun JvmCompilationResult.loadTopLevelField(name: String): Any? =
        classLoader.loadClass("SourceKt")
            .getDeclaredField(name)
            .also { it.isAccessible = true }
            .get(null)

    xtest("文字列リテラルのデフォルト値が展開される") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            fun target(x: String = "hello") {}
            val v = defaultArgOf<String>(funName = "target", argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.loadTopLevelField("v") shouldBe "hello"
    }

    xtest("式形式のデフォルト値（123.toString()）が展開される") {
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

    xtest("存在しない関数名はコンパイルエラーになる") {
        val result = compile(
            """
            import com.example.plugin.runtime.defaultArgOf
            val v = defaultArgOf<String>(funName = "notExist", argName = "x")
            """.trimIndent()
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Function 'notExist' not found"
    }

    xtest("存在しないパラメータ名はコンパイルエラーになる") {
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

    xtest("定数でない funName はコンパイルエラーになる") {
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

    xtest("デフォルト値のないパラメータはコンパイルエラーになる") {
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
