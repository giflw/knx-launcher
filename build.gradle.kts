plugins {
    kotlin("multiplatform") version "1.8.10"
}


fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val taskName = linkTaskName.replaceFirst("link", "windres")
    val inFile = compilation.allKotlinSourceSets.stream().filter {
            println(">>>>> ${it}")
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
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    sourceSets {
        commonMain {
            val okioVersion = "3.3.0"
            dependencies {
                implementation("com.squareup.okio:okio:$okioVersion")
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
            executable("knxLauncher", listOf(RELEASE, DEBUG)){
                baseName = "knx-launcher"
                windowsResources("${baseName}.rc")
                println("Executable path: ${outputFile.absolutePath}")
            }
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.3"
    distributionType = Wrapper.DistributionType.BIN
}
