plugins {
    kotlin("multiplatform") version "2.2.0"
    id("de.undercouch.download") version "5.6.0"
}

group = "com.itquasar"
version = "0.4.0-SNAPSHOT"

fun isMinGWx64(): Boolean {
    return System.getProperty("os.name").startsWith("Win", ignoreCase = true)
}

tasks.register("download-mingw") {
    if (isMinGWx64() && !layout.buildDirectory.file("mingw64").get().asFile.exists()) {
        val destFile = layout.buildDirectory.file("mingw64.zip")
        download.run {
            src("https://github.com/brechtsanders/winlibs_mingw/releases/download/15.1.0posix-13.0.0-ucrt-r2/winlibs-x86_64-posix-seh-gcc-15.1.0-mingw-w64ucrt-13.0.0-r2.zip")
            dest(destFile)
            onlyIfModified(true)
            println(name)
            println("from: $src")
            println("  to: $dest")
        }
        println("MinGW downloaded")
        copy {
            println("Unzipping MinGW")
            from(zipTree(destFile))
            into(layout.buildDirectory)
        }
        println("MinGW unzipped")
        destFile.get().asFile.delete()
    } else {
        println("Skipping mingw")
    }
}

fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val taskName = linkTaskName.replaceFirst("link", "windres")
    val inFile = compilation.allKotlinSourceSets.stream().filter {
        it.name == "mingwX64Main"
    }.findFirst().get().resources.sourceDirectories.singleFile.resolve(rcFileName)
    val outFile = layout.buildDirectory.asFile.get().resolve("processedResources/$taskName.res")

    val windresTask = tasks.create<Exec>(taskName) {
        inputs.file(inFile)
        outputs.file(outFile)
        commandLine(
            "build/mingw64/bin/windres",
            "--use-temp-file",
            inFile, /*"-D_${buildType.name}",*/
            "-O",
            "coff",
            "-o",
            outFile
        )
        dependsOn(compilation.compileTaskProvider, "download-mingw")
    }

    linkTaskProvider.get().dependsOn(windresTask)
    linkerOpts(outFile.toString())
}

repositories {
    mavenCentral()
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
