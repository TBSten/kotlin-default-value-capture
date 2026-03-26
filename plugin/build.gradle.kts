plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
}

dependencies {
    compileOnly(libs.kotlinCompilerEmbeddable)
    compileOnly(libs.autoServiceAnnotations)
    ksp(libs.autoServiceKsp)

    testImplementation(libs.kotlinCompilerEmbeddable)
    testImplementation(libs.kctforkCore)
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.kotestAssertionsCore)
}
