import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
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

    // Kotlin bağımlılıklarını 2.1.0 sürümüne sabitle (Transitive metadata çakışmalarını önler)
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("2.1.0")
                because("Force consistent Kotlin 2.1.0 across all dependencies")
            }
        }
    }

    android {
        namespace = "com.kekik.${project.name.lowercase().replace("-", "")}"
        compileSdkVersion(34)

        defaultConfig {
            minSdk = 21
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xskip-metadata-version-check",
                "-Xallow-unstable-dependencies"
            )
        }
    }

    dependencies {
        val cloudstreamVersion = "-SNAPSHOT"
        val kotlinVersion = "2.1.0"
        
        // Kotlin BOM ile sürüm sabitleme
        "implementation"(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

        "implementation"("com.github.recloudstream:cloudstream:$cloudstreamVersion")
        "implementation"("com.github.Blatzar:NiceHttp:0.4.11")
        "implementation"("org.jsoup:jsoup:1.17.2")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    }
}

task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
