package knxlauncher

import com.kgit2.process.Child
import com.kgit2.process.Command
import kotlinx.cli.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    initPlatform()

    val path: Path = binaryPath()

    val programName = path.name.replace(".exe", "", ignoreCase = true).replace("_debug_", "")

    val parser = ArgParser(programName)
    val debug by parser.option(ArgType.Boolean, description = "Turn on debug mode").default(debug)
    val cmdDescriptor by parser.option(ArgType.String, shortName = "d", description = "Application description file")
        .default("${programName}.cmd.knx")
    val cfgDescriptor by parser.option(ArgType.String, shortName = "c", description = "Configuration description file")
        .default("${programName}.cfg.knx")
    val envDescriptor by parser.option(ArgType.String, shortName = "e", description = "Environment description file")
        .default("${programName}.env.knx")
    val incDescriptor by parser.option(ArgType.String, shortName = "e", description = "Includes description file (list of other files to include as key=value replacements)")
        .default("${programName}.inc.knx")
    val arguments: List<String> by parser.argument(ArgType.String).optional().multiple(Int.MAX_VALUE)

    parser.parse(args)

    val originalEnv = env()

    var otherVars: Map<String, String> = if (!FileSystem.SYSTEM.exists(path.parent!!.resolve(incDescriptor))) {
        mapOf()
    } else {
        val replacer = Replacer(mapOf(), path, originalEnv)
        readAllLines(incDescriptor.toPath())
            .filter { it != null && !it.isEmpty() }
            .map { readAllLines(replacer.replaceVars(it).toPath()) }
            .flatten()
            .map { it.trimStart() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.split("=", limit = 1) }
            .filter { it.size == 2 }
            .associate {
                Pair(replacer.replaceVars(it.first()), replacer.replaceVars(it.last()))
            }
    }

    var replacer = Replacer(otherVars, path, originalEnv)

    val cfg = Config(readMap("Config", path.parent?.resolve(cfgDescriptor), replacer))
    knxlauncher.debug = debug || cfg.debug
    debug("[CFG] ${cfg}")

    info("${parser.programName} [debug=${debug}] ${cmdDescriptor} ${cfgDescriptor}")

    val env =
        readMap("Environment", path.parent?.resolve(envDescriptor), replacer, if (cfg.preserveEnv) originalEnv else mapOf())

    replacer = Replacer(otherVars, path, env)
    val commandLine: List<String> = CommandLine(cmdDescriptor.toPath(), path, replacer).get()
    val command: String = commandLine.first()

    var commandArgs: List<String> = commandLine.subList(1, commandLine.size)
    commandArgs = if (commandArgs.contains(cfg.knxExtraArgsName)) {
        debug("[CMD] contains ${cfg.knxExtraArgsName}")
        val first = commandArgs.subList(0, commandArgs.indexOf(cfg.knxExtraArgsName))
        debug("[CMD:first] ${first}")
        val last = if (commandArgs.indexOf(cfg.knxExtraArgsName) + 1 < commandArgs.size) {
            commandArgs.subList(commandArgs.indexOf(cfg.knxExtraArgsName) + 1, commandArgs.size - 1)
        } else {
            listOf()
        }
        debug("[CMD:last] ${last}")
        val cargs = first + arguments + last
        debug("[CMD:cargs] ${cargs}")
        cargs
    } else {
        debug("[CMD:oargs] ${commandArgs}")
        commandArgs
    }

    debug("[CMD] ${command}")
    if (command.isNotEmpty()) {
        debug(command + " " + commandArgs.joinToString(separator = " "))
        val process = Command(command).cwd(path.parent.toString())
        commandArgs.forEach { process.arg(it) }
        env.forEach { process.env(it.key, it.value) }

        if (cfg.wait) {
            val childExitStatus = process.status()
            info("Child Exit Status: ${childExitStatus}")
            exitProcess(childExitStatus.exitStatus())
        } else {
            val child: Child = process.spawn()
            info("Child: ${child}")
            exitProcess(if (child.id != null && child.id!! > 0) 0 else 1)
        }
    } else {
        error("Command not supplied!")
        exitProcess(1)
    }
}
