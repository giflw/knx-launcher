import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform") version "1.9.10"
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
    sourceSets {
        commonMain {
            val okioVersion = "3.5.0"
            dependencies {
                implementation("com.squareup.okio:okio:$okioVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
                implementation("com.kgit2:kommand:1.0.2")
            }
        }

        val mingwX64Main by creating {
            if (isMinGWx64()) {
                dependsOn(commonMain.get())
            }
        }

        val mingwX64Test by creating {
            if (isMinGWx64()) {
                dependsOn(commonTest.get())
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


tasks.named<KotlinCompilationTask<*>>("compileKotlinMingwX64").configure {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
        freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalNativeApi")
    }
}

//tasks.withType<Wrapper> {
//    gradleVersion = "8.3"
//    distributionType = Wrapper.DistributionType.BIN
//}

