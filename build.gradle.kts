plugins {
    kotlin("multiplatform") version "1.8.10"
}

group = "com.itquasar"
version = "0.3.0-SNAPSHOT"

fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val taskName = linkTaskName.replaceFirst("link", "windres")
    val inFile = compilation.allKotlinSourceSets.stream().filter {
            it.name == "mingwX64Main"
    } .findFirst().get().resources.sourceDirectories.singleFile.resolve(rcFileName)
    val outFile = buildDir.resolve("processedResources/$taskName.res")

    val windresTask = tasks.create<Exec>(taskName) {
        inputs.file(inFile)
        outputs.file(outFile)
        commandLine("windres", "--use-temp-file", inFile, /*"-D_${buildType.name}",*/ "-O", "coff", "-o", outFile)
        dependsOn(compilation.compileTaskProvider)
    }

    linkTask.dependsOn(windresTask)
    linkerOpts(outFile.toString())
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        commonMain {
            val okioVersion = "3.3.0"
            dependencies {
                implementation("com.squareup.okio:okio:$okioVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
                implementation("com.kgit2:kommand:1.0.1")
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain.get())
        }
        val mingwX64Main by creating {
            if (System.getProperty("os.name").startsWith("Win", ignoreCase = true)) {
                nativeMain.dependsOn(this)
            }
        }
    }
    mingwX64("native") {
        binaries {
            val resourcesBaseName = "knx-launcher"
            val KNX_EXE_NAME = "KNX_EXE_NAME"
            var knxExeName = System.getenv(KNX_EXE_NAME)
            knxExeName = knxExeName ?: System.getProperty(KNX_EXE_NAME)
            val exeName = if (knxExeName != null && knxExeName.isNotEmpty()) knxExeName else "knx-launcher"
            executable("knxLauncher", listOf(DEBUG)){
                baseName = "${exeName}_debug_"
                entryPoint = "knxlauncher.main"
                windowsResources("${resourcesBaseName}.rc")
                println("Executable path: ${outputFile.absolutePath}")
            }
            executable("knxLauncher", listOf(RELEASE)){
                baseName = exeName
                entryPoint = "knxlauncher.main"
                windowsResources("${resourcesBaseName}.rc")
                println("Executable path: ${outputFile.absolutePath}")
            }
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.3"
    distributionType = Wrapper.DistributionType.BIN
}

