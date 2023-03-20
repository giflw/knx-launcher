import kotlinx.cinterop.MemScope
import kotlinx.cinterop.toCStringArray
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.*
import utils.readAllLines


fun mode(mode: String?): Int {
    return when (mode) {
        "overlay" -> P_OVERLAY
        "wait" -> P_WAIT
        "nowait" -> P_NOWAIT
        "nowaito" -> P_NOWAITO
        "detach" -> P_DETACH
        else -> P_NOWAIT
    }
}

fun main(args: Array<String>) {
    val path: Path = utils.binaryPath()

    val parser = ArgParser(path.name.replace(".exe", "", ignoreCase = true))
    val debug by parser.option(ArgType.Boolean, description = "Turn on debug mode").default(logger.debug)
    val appDescriptor by parser.option(ArgType.String, shortName = "d", description = "Application description file")
        .default("${parser.programName}.app.knx")
    val cfgDescriptor by parser.option(ArgType.String, shortName = "c", description = "Configuration description file")
        .default("${parser.programName}.cfg.knx")
    parser.parse(args)

    logger.debug = debug

    logger.info("${parser.programName} [debug=${debug}] ${appDescriptor} ${cfgDescriptor}")

    val cfgPath = path.parent?.resolve(cfgDescriptor)
    logger.info("Config path: ${cfgPath}")
    val cfg = if (cfgPath != null) {
        readAllLines(cfgPath)
            .filter { it.isNotBlank() }
            .map { it.split('=', limit = 2) }
            .associate { it.first() to it.last() }
    } else {
        mapOf()
    }

    cfg.forEach {
        logger.debug("[CFG] ${it.key} -> ${it.value}")
    }

    val commandLine: List<String> = Descriptor(appDescriptor.toPath(), path).get()
    val command: String = commandLine.first()
    val commandArgs: List<String> = commandLine.subList(1, commandLine.size)

    logger.debug(commandLine.joinToString(separator = " "))
    val exitCode: Int = spawnvp(mode(cfg["mode"]), command, commandArgs.toCStringArray(MemScope())).toInt()
    exit(exitCode)
}
