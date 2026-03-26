package com.example.gradleplugin

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Gradle plugin that integrates the defaultArgOf compiler plugin into a Kotlin project.
 *
 * ### Usage
 * Apply this plugin in your `build.gradle.kts`:
 * ```kotlin
 * plugins {
 *     id("com.example.defaultarg")
 * }
 * ```
 *
 * This automatically:
 * 1. Adds the `runtime` module as an `implementation` dependency,
 *    providing the `defaultArgOf(...)` function declarations.
 * 2. Configures the Kotlin compiler to load the defaultArgOf compiler plugin JAR,
 *    which performs the compile-time replacement.
 *
 * ### After applying
 * You can use `defaultArgOf` in your Kotlin source code:
 * ```kotlin
 * fun greet(name: String = "World") {}
 *
 * val nameDefault = defaultArgOf<String>(::greet, "name") // "World" at compile time
 * ```
 *
 * ### For library consumers
 * When publishing, the [getPluginArtifact] Maven coordinates must match the published
 * compiler-plugin artifact so that Gradle can resolve it from the repository.
 */
class DefaultArgOfGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.dependencies.add("implementation", target.project(":runtime"))
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "com.example.defaultarg"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.example",
        artifactId = "defaultarg-plugin",
        version = "1.0.0",
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }
}
