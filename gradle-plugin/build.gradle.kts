plugins {
    id("buildsrc.convention.kotlin-jvm")
    `java-gradle-plugin`
    id("buildsrc.convention.publish-convention")
}

val defaultargGroup = "me.tbsten.defaultargcapture"
val defaultargVersion = libs.versions.defaultarg.get()

mavenPublishing {
    coordinates(
        groupId = defaultargGroup,
        artifactId = "gradle-plugin",
        version = defaultargVersion,
    )
}

dependencies {
    compileOnly(libs.kotlinGradlePluginApi)
    implementation(project(":compiler-plugin"))
    implementation(project(":runtime"))
}

gradlePlugin {
    plugins {
        create("defaultArgOf") {
            id = "me.tbsten.defaultargcapture"
            implementationClass = "com.example.gradleplugin.DefaultArgOfGradlePlugin"
        }
    }
}
