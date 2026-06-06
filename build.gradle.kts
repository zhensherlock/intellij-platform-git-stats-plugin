import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.gradle.api.tasks.bundling.Zip

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.opentest4j:opentest4j:1.3.0")

    intellijPlatform {
        intellijIdeaCommunity(properties("platformVersion"))
        bundledPlugin("Git4Idea")
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "GitStats"

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, properties("platformVersion"))
            create(IntelliJPlatformType.IntellijIdea, "2026.1.3")
        }
    }
}

tasks.named<Zip>("buildPlugin") {
    archiveBaseName = "GitStats"
}
