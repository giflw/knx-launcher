package knxlauncher

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCStringArray
import kotlinx.cli.*
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.spawnvpe
import kotlin.system.exitProcess


fun main(args: Array<String>) {
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
    val arguments: List<String> by parser.argument(ArgType.String).optional().multiple(Int.MAX_VALUE)

    parser.parse(args)

    val cfg = Config(readMap("Config", path.parent?.resolve(cfgDescriptor)))
    knxlauncher.debug = debug || cfg.debug

    info("${parser.programName} [debug=${debug}] ${cmdDescriptor} ${cfgDescriptor}")

    var replacer = Replacer(path, env())
    val env = readMap("Environment", path.parent?.resolve(envDescriptor), if (cfg.preserveEnv) env() else mapOf())
        .onEach { it.key to replacer.replaceVars(it.value) }

    replacer = Replacer(path, env)
    val commandLine: List<String> = CommandLine(cmdDescriptor.toPath(), path, replacer).get()
    val command: String = commandLine.first()

    var commandArgs: List<String> = commandLine.subList(1, commandLine.size)
    commandArgs = if (commandArgs.contains(cfg.knxExtraArgsName)) {
        val first = commandLine.subList(0, commandLine.indexOf(cfg.knxExtraArgsName) - 1)
        val last = if (commandLine.indexOf(cfg.knxExtraArgsName) < commandLine.size) {
            commandLine.subList(commandLine.indexOf(cfg.knxExtraArgsName), commandLine.size - 1)
        } else {
            listOf()
        }
        first + arguments + last
    } else {
        commandArgs
    }

    if (command.isNotEmpty()) {
        debug(commandLine.joinToString(separator = " "))
        var exitCode = 1
        memScoped {
            exitCode = spawnvpe(
                cfg.mode,
                command,
                commandArgs.toCStringArray(this),
                env.map { "${it.key}=${it.value}" }.toCStringArray(this)
            ).toInt()
            info("Exit code from child: ${exitCode}")
        }
        exitProcess(exitCode)
    } else {
        error("Command not supplied!")
        exitProcess(1)
    }
}
