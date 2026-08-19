import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) =
    extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) =
    extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        setRepo("https://github.com/akdrhmz/dev-workspace-tools")
    }

    android {
        namespace = "com.kekik.${project.name.lowercase().replace("-", "")}"
        compileSdkVersion(34)

        defaultConfig {
            minSdk = 21
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    dependencies {
        val cloudstreamVersion = "-SNAPSHOT"
        "implementation"("com.github.recloudstream:cloudstream:$cloudstreamVersion")
        "implementation"("org.jsoup:jsoup:1.17.2")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    }
}

task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
