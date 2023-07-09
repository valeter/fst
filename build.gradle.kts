import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.8.20"
    id("org.jetbrains.compose") version "1.4.0"
    java

    id("io.qameta.allure") version "2.11.2"
}

group = "ru.anisimov.fst"
version = (extra["fst.version"] as String)

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
            kotlinOptions.allWarningsAsErrors = true
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "fst"
            packageVersion = version as String

            windows {
                menu = true
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "ab95c89c-705c-4007-9cda-bed91a6823db"
                iconFile.set(project.file("src/jvmMain/resources/icon/icon.png"))
            }

            macOS {
                // Use -Pcompose.desktop.mac.sign=true to sign and notarize.
                bundleID = "ru.anisimov.fst"
                packageName = "fst"
                dockName = "fst"
                iconFile.set(project.file("src/jvmMain/resources/icon/icon.icns"))
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon/icon.png"))
            }
        }
    }
}

tasks.register<Copy>("copyScriptsToJars") {
    from(layout.projectDirectory.dir("src/script"))
    include("*.*")
    into(layout.buildDirectory.dir("compose/jars"))
}
