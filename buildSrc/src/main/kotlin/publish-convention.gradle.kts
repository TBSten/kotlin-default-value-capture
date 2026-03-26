package buildsrc.convention

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Sign only when GPG key is available (CI or local with signing configured).
    if (project.findProperty("signing.keyId") != null ||
        System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null
    ) {
        signAllPublications()
    }

    pom {
        name.set(project.name)
        description.set("Kotlin compiler plugin to capture function default argument values at compile time.")
        url.set("https://github.com/TBSten/kotlin-default-value-capture")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("tbsten")
                name.set("TBSten")
                url.set("https://github.com/TBSten")
            }
        }

        scm {
            url.set("https://github.com/TBSten/kotlin-default-value-capture")
            connection.set("scm:git:git://github.com/TBSten/kotlin-default-value-capture.git")
            developerConnection.set("scm:git:ssh://git@github.com/TBSten/kotlin-default-value-capture.git")
        }
    }
}
