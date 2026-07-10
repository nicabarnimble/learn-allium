import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "AlliumTutorKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        val desktopMain by getting {
            dependencies {
                val os = System.getProperty("os.name")
                val arch = System.getProperty("os.arch")
                val target = when {
                    os == "Mac OS X" && arch == "aarch64" -> "macos-arm64"
                    os == "Mac OS X" -> "macos-x64"
                    os.startsWith("Windows") && arch == "aarch64" -> "windows-arm64"
                    os.startsWith("Windows") -> "windows-x64"
                    arch == "aarch64" -> "linux-arm64"
                    else -> "linux-x64"
                }
                implementation(
                    "org.jetbrains.compose.desktop:desktop-jvm-$target:${libs.versions.composeMultiplatform.get()}",
                )
            }
        }
    }
}

compose.resources {
    packageOfResClass = "dev.allium.tutor.generated.resources"
}

val packagingJdk = extensions.getByType<JavaToolchainService>().launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
    vendor.set(JvmVendorSpec.ADOPTIUM)
}

compose.desktop {
    application {
        mainClass = "dev.allium.tutor.DesktopMainKt"
        javaHome = packagingJdk.get().metadata.installationPath.asFile.absolutePath
        jvmArgs("--enable-native-access=ALL-UNNAMED")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Allium Tutor"
            packageVersion = "1.0.0"
            description = "Story-driven training for behavioural specification"
            vendor = "Allium"
        }
    }
}
