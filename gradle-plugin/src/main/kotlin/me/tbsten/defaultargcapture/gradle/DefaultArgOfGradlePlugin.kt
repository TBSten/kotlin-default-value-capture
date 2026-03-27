package me.tbsten.defaultargcapture.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
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
 *     id("me.tbsten.defaultargcapture") version "0.1.0-alpha02"
 * }
 * ```
 *
 * This automatically:
 * 1. Adds the `runtime` module as a dependency (`commonMainImplementation` for KMP,
 *    `implementation` for single-target projects), providing the `defaultArgOf(...)` function declarations.
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
        super.apply(target)
        val runtimeDep = "$GROUP_ID:runtime:$VERSION"
        when (target.kotlinExtension) {
            is KotlinMultiplatformExtension -> {
                target.dependencies.add("commonMainImplementation", runtimeDep)
            }
            is KotlinSingleTargetExtension<*> -> {
                target.dependencies.add("implementation", runtimeDep)
            }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "me.tbsten.defaultargcapture"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = GROUP_ID,
        artifactId = "compiler-plugin",
        version = VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }

    companion object {
        private const val GROUP_ID = "me.tbsten.defaultargcapture"
        private const val VERSION = "0.1.0-alpha02"
    }
}
