plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
    id("buildsrc.convention.publish-convention")
}

mavenPublishing {
    coordinates(
        groupId = "me.tbsten.defaultargcapture",
        artifactId = "compiler-plugin",
        version = libs.versions.defaultarg.get(),
    )
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
        freeCompilerArgs.add("-Xcontext-parameters")
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
