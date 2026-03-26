plugins {
    id("buildsrc.convention.kotlin-jvm")
    `java-gradle-plugin`
}

dependencies {
    compileOnly(libs.kotlinGradlePluginApi)
    implementation(project(":plugin"))
    implementation(project(":runtime"))
}

gradlePlugin {
    plugins {
        create("defaultArgOf") {
            id = "com.example.defaultarg"
            implementationClass = "com.example.gradleplugin.DefaultArgOfGradlePlugin"
        }
    }
}
