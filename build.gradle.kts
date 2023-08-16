import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform") version "1.9.0"
}

group = "com.itquasar"
version = "0.3.0"

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
            val okioVersion = "3.5.0"
            dependencies {
                implementation("com.squareup.okio:okio:$okioVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
                implementation("com.kgit2:kommand:1.0.2")
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
        val linuxX64Main by creating {
            if (System.getProperty("os.name").equals("Linux", ignoreCase = true)) {
                nativeMain.dependsOn(this)
            }
        }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        //hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native") {
            binaries {
                val resourcesBaseName = "knx-launcher"
                val KNX_EXE_NAME = "KNX_EXE_NAME"
                var knxExeName = System.getenv(KNX_EXE_NAME)
                knxExeName = knxExeName ?: System.getProperty(KNX_EXE_NAME)
                val exeName = if (knxExeName != null && knxExeName.isNotEmpty()) knxExeName else "knx-launcher"
                executable("knxLauncher", listOf(DEBUG)){
                    baseName = "${exeName}_debug_"
                    entryPoint = "knxlauncher.main"
                    println("Executable path: ${outputFile.absolutePath}")
                }
                executable("knxLauncher", listOf(RELEASE)){
                    baseName = exeName
                    entryPoint = "knxlauncher.main"
                    println("Executable path: ${outputFile.absolutePath}")
                }
            }
        }
        isMingwX64 -> mingwX64("native") {
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
        else -> throw GradleException("Host OS is not supported.")
    }
}

tasks.named<KotlinCompilationTask<*>>("compileKotlinNative").configure {
    println ("!!!!!!!!!!!!!!!!!!!!!!!!")
    println("Opting in to experimental APIs")
    println ("!!!!!!!!!!!!!!!!!!!!!!!!")
    compilerOptions.optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    compilerOptions.optIn.add("kotlin.experimental.ExperimentalNativeApi")
}

tasks.withType<Wrapper> {
    gradleVersion = "8.3-rc-4"
    distributionType = Wrapper.DistributionType.BIN
}

