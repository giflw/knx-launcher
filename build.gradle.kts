plugins {
    kotlin("multiplatform") version "1.9.24"
}

group = "com.itquasar"
version = "0.4.0-SNAPSHOT"

fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val taskName = linkTaskName.replaceFirst("link", "windres")
    val inFile = compilation.allKotlinSourceSets.stream().filter {
        it.name == "mingwX64Main"
    }.findFirst().get().resources.sourceDirectories.singleFile.resolve(rcFileName)
    val outFile = layout.buildDirectory.asFile.get().resolve("processedResources/$taskName.res")

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

fun isMinGWx64(): Boolean {
    return System.getProperty("os.name").startsWith("Win", ignoreCase = true)
}

kotlin {
    jvm()
    mingwX64()
    linuxX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            listOf("kotlinx.cinterop", "kotlin.experimental").forEach { pkg ->
                listOf("ExperimentalForeignApi", "ExperimentalNativeApi").forEach { cls ->
                    languageSettings.optIn("${pkg}.${cls}")
                }
            }
        }
        commonMain {
            val okioVersion = "3.9.1"
            dependencies {
                implementation("com.squareup.okio:okio:$okioVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
                implementation("com.kgit2:kommand:2.2.1")
            }
        }

    }
    sourceSets.forEach(System.out::println)
    mingwX64("mingwX64") {
        binaries {
            val resourcesBaseName = "knx-launcher"
            val KNX_EXE_NAME = "KNX_EXE_NAME"
            var knxExeName = System.getenv(KNX_EXE_NAME)
            knxExeName = knxExeName ?: System.getProperty(KNX_EXE_NAME)
            val exeName = if (knxExeName != null && knxExeName.isNotEmpty()) knxExeName else "knx-launcher"
            executable("knxLauncher", listOf(DEBUG)) {
                baseName = "${exeName}_debug_"
                entryPoint = "knxlauncher.main"
                windowsResources("${resourcesBaseName}.rc")
                println("Executable path: ${outputFile.absolutePath}")
            }
            executable("knxLauncher", listOf(RELEASE)) {
                baseName = exeName
                entryPoint = "knxlauncher.main"
                windowsResources("${resourcesBaseName}.rc")
                println("Executable path: ${outputFile.absolutePath}")
            }
        }
    }
}
