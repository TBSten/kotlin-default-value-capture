plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
}

dependencies {
    implementation(project(":runtime"))
    kotlinCompilerPluginClasspath(project(":plugin"))
}

application {
    mainClass = "MainKt"
}
