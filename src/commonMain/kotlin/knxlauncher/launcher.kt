package knxlauncher

import com.kgit2.process.Child
import com.kgit2.process.ChildOptions
import com.kgit2.process.Command
import io.ktor.utils.io.errors.*
import kotlinx.cli.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    initPlatform()

    val path: Path = binaryPath

    val programName = path.name.replace(".exe", "", ignoreCase = true).replace("_debug_", "")

    val parser = ArgParser(programName)
    val debug by parser.option(ArgType.Boolean, description = "Turn on debug mode").default(binaryDebug)
    val cmdDescriptor by parser.option(ArgType.String, shortName = "d", fullName = "command", description = "Application command description file")
        .default("${programName}.cmd.knx")
    val cfgDescriptor by parser.option(ArgType.String, shortName = "c", fullName = "configuration", description = "Configuration description file")
        .default("${programName}.cfg.knx")
    val envDescriptor by parser.option(ArgType.String, shortName = "e", fullName = "environment", description = "Environment description file")
        .default("${programName}.env.knx")
    val incDescriptor by parser.option(ArgType.String, shortName = "i", fullName = "includes", description = "Includes description file (list of other files to include as key=value replacements)")
        .default("${programName}.inc.knx")
    val arguments: List<String> by parser.argument(ArgType.String).optional().multiple(Int.MAX_VALUE)

    parser.parse(args)

    val originalEnv = Env.get()

    val cfg = Config(debug, readMap("Config", path.parent?.resolve(cfgDescriptor), Replacer(path).env(originalEnv)))
    binaryDebug = cfg.debug.value
    debug("[CFG] ${cfg}")

    val otherVars: Map<String, String> = if (!FileSystem.SYSTEM.exists(path.parent!!.resolve(incDescriptor))) {
        mapOf()
    } else {
        val replacer = Replacer(path).env(originalEnv)
        readAllLines(incDescriptor.toPath())
            .filter { it.isNotEmpty() }
            .onEach { debug("Including ${it}") }
            .flatMap { readMap("Includes",  replacer.replaceVars(it).toPath(), replacer).entries }
            .onEach { debug("Included ${it.key}: ${it.value}") }
            .associate {
                Pair(replacer.replaceVars(it.key), replacer.replaceVars(it.value))
            }
    }
    info("Included ${otherVars.size} other vars from .inc.knx files list.")

    var replacer = Replacer(path).vars(otherVars).env(originalEnv)

    info("${parser.programName} [debug=${debug}] ${cmdDescriptor} ${cfgDescriptor}")

    val env =
        readMap("Environment", path.parent?.resolve(envDescriptor), replacer, if (cfg.preserveEnv.value) originalEnv else mapOf())

    replacer = Replacer(path).vars(otherVars).env(env).config(cfg)
    val commandLine: List<String> = CommandLine(cmdDescriptor.toPath(), path, replacer).get()
    val command: String = commandLine.first()

    var commandArgs: List<String> = commandLine.subList(1, commandLine.size)
    val knxExtraArgsName = cfg.knxExtraArgsName.value
    commandArgs = if (commandArgs.contains(knxExtraArgsName)) {
        debug("[CMD] contains $knxExtraArgsName")
        val first = commandArgs.subList(0, commandArgs.indexOf(knxExtraArgsName))
        debug("[CMD:first] ${first}")
        val last = if (commandArgs.indexOf(knxExtraArgsName) + 1 < commandArgs.size) {
            commandArgs.subList(commandArgs.indexOf(knxExtraArgsName) + 1, commandArgs.size - 1)
        } else {
            listOf()
        }
        debug("[CMD:last] ${last}")
        val cargs = first + (arguments.ifEmpty { cfg.argsIfNoArgs.value }) + last
        debug("[CMD:cargs] ${cargs}")
        cargs
    } else {
        debug("[CMD:oargs] ${commandArgs}")
        commandArgs
    }

    debug("[CMD] ${command}")
    if (command.isNotEmpty()) {
        debug(command + " " + commandArgs.joinToString(separator = " "))

        val process = Command(command)
        commandArgs.forEach { process.arg(it) }
        process.cwd(cfg.cwd.value)

        env.forEach { process.env(it.key, it.value) }

        if (cfg.wait.value) {
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
