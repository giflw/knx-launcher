package knxlauncher

import com.kgit2.process.Child
import com.kgit2.process.ChildExitStatus
import com.kgit2.process.Command
import kotlinx.cli.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.system.exitProcess

// source: https://kickjava.com/src/org/eclipse/equinox/app/IApplication.java.htm
/**
 * Exit object indicating normal termination
 */
const val EXIT_OK = 0

/**
 * Exit object requesting platform restart
 */
const val EXIT_RESTART = 23

/**
 * Exit object requesting that the command passed back be executed. Typically
 * this is used to relaunch Eclipse with different command line arguments. When the executable is
 * relaunched the command line will be retrieved from the <code>eclipse.exitdata</code> system property.
 */
const val EXIT_RELAUNCH = 24

class Options(args: Array<String>) {
    val path: Path = binaryPath

    val programName = path.name.replace(".exe", "", ignoreCase = true).replace("_debug_", "")

    val parser = ArgParser(programName)
    val debug by parser.option(ArgType.Boolean, description = "Turn on debug mode").default(binaryDebug)
    val cmdDescriptor by parser.option(
        ArgType.String,
        shortName = "d",
        fullName = "command",
        description = "Application command description file"
    )
        .default("${programName}.cmd.knx")
    val cfgDescriptor by parser.option(
        ArgType.String,
        shortName = "c",
        fullName = "configuration",
        description = "Configuration description file"
    )
        .default("${programName}.cfg.knx")
    val envDescriptor by parser.option(
        ArgType.String,
        shortName = "e",
        fullName = "environment",
        description = "Environment description file"
    )
        .default("${programName}.env.knx")
    val incDescriptor by parser.option(
        ArgType.String,
        shortName = "i",
        fullName = "includes",
        description = "Includes description file (list of other files to include as key=value replacements)"
    )
        .default("${programName}.inc.knx")
    val arguments: List<String> by parser.argument(ArgType.String).optional().multiple(Int.MAX_VALUE)

    init {
        parser.parse(args)
    }
}

fun main(args: Array<String>) {
    initPlatform()

    val opts = Options(args)
    info("${opts.parser.programName} [debug=${opts.debug}] ${opts.cfgDescriptor} ${opts.incDescriptor} ${opts.envDescriptor} ${opts.cmdDescriptor}")

    val originalEnv = Env.get()

    val cfg = config(opts, originalEnv)

    val otherVars: Map<String, String> = otherVars(opts, originalEnv, cfg)

    val env = env(otherVars, originalEnv, cfg, opts)

    val (command: String, commandArgs: List<String>) = command(otherVars, env, cfg, opts)

    if (command.isNotEmpty()) {
        debug(command + " " + commandArgs.joinToString(separator = " "))

        val process = Command(command)
        commandArgs.forEach { process.arg(it) }
        process.cwd(cfg.cwd.value)

        env.forEach { process.env(it.key, it.value) }

        if (cfg.wait.value) {
            run(process, cfg)
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

private fun run(process: Command, cfg: Config) {
//    fun handleSignal(signalNumber: Int) {
//        warn("Got signal ${signalNumber}")
//    }
//    signal(SIGINT, staticCFunction(::handleSignal))

    var childExitStatus: ChildExitStatus

    do {
        warn("Starting ${process.command}")
        val child = process.spawn()
        childExitStatus = child.wait()
        warn("Child Exit Status: ${childExitStatus}")
    } while (cfg.shouldRestart(childExitStatus.exitStatus()))

    exitProcess(childExitStatus.exitStatus())
}

private fun command(
    otherVars: Map<String, String>,
    env: Map<String, String>,
    cfg: Config,
    opts: Options
): Pair<String, List<String>> {
    val replacer = Replacer().vars(otherVars).env(env).config(cfg)
    val commandLine: List<String> = CommandLine(opts.cmdDescriptor.toPath(), opts.path, replacer).get()
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
        val cargs = first + (opts.arguments.ifEmpty { cfg.argsIfNoArgs.value }) + last
        debug("[CMD:cargs] ${cargs}")
        cargs
    } else {
        debug("[CMD:oargs] ${commandArgs}")
        commandArgs
    }
    debug("[CMD] ${command}")
    return Pair(command, commandArgs)
}

private fun env(
    otherVars: Map<String, String>,
    originalEnv: Map<String, String>,
    cfg: Config,
    opts: Options
): Map<String, String> {
    val replacer = Replacer().vars(otherVars).env(originalEnv).config(cfg)
    val env = readMap(
        "Environment",
        opts.path.parent?.resolve(opts.envDescriptor),
        replacer,
        if (cfg.preserveEnv.value) originalEnv else mapOf()
    )
    return env
}

private fun otherVars(
    opts: Options,
    originalEnv: Map<String, String>,
    cfg: Config
): Map<String, String> {
    val otherVars: Map<String, String> =
        if (!FileSystem.SYSTEM.exists(opts.path.parent!!.resolve(opts.incDescriptor))) {
            mapOf()
        } else {
            val replacer = Replacer().env(originalEnv).config(cfg)
            readAllLines(opts.incDescriptor.toPath())
                .filter { it.isNotEmpty() }
                .onEach { debug("Including ${it}") }
                .flatMap { readMap("Includes", replacer.replaceVars(it).toPath(), replacer).entries }
                .onEach { debug("Included ${it.key}: ${it.value}") }
                .associate {
                    Pair(replacer.replaceVars(it.key), replacer.replaceVars(it.value))
                }
        }
    info("Included ${otherVars.size} other vars from .inc.knx files list.")
    return otherVars
}

private fun config(
    opts: Options,
    originalEnv: Map<String, String>
): Config {
    val cfg = Config(
        opts.debug,
        readMap("Config", opts.path.parent?.resolve(opts.cfgDescriptor), Replacer().env(originalEnv))
    )
    binaryDebug = cfg.debug.value
    debug("[CFG] ${cfg}")
    return cfg
}
