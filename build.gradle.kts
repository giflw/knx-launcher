plugins {
    kotlin("multiplatform") version "1.8.10"
}


fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val taskName = linkTaskName.replaceFirst("link", "windres")
    val inFile = compilation.defaultSourceSet.resources.sourceDirectories.singleFile.resolve(rcFileName)
    val outFile = buildDir.resolve("processedResources/$taskName.res")

    val windresTask = tasks.create<Exec>(taskName) {
        inputs.file(inFile)
        outputs.file(outFile)
        commandLine("windres", "--use-temp-file", inFile, /*"-D_${buildType.name}",*/ "-O", "coff", "-o", outFile)
        dependsOn(compilation.compileKotlinTask)
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
    }
    mingwX64("native") {
        binaries {
            executable("launcher", listOf(RELEASE, DEBUG)){
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
