plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

dependencies {
    compileOnly(libs.kotlinCompilerEmbeddable)
    compileOnly(libs.autoServiceAnnotations)
    ksp(libs.autoServiceKsp)

    testImplementation(project(":runtime"))
    testImplementation(libs.kotlinCompilerEmbeddable)
    testImplementation(libs.kctforkCore)
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.kotestAssertionsCore)
}
