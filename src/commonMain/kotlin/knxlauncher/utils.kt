package knxlauncher

import kotlinx.cinterop.get
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path
import platform.posix.environ

expect val DS: String
expect fun binaryPath(): Path

fun readAll(path: Path): String {
    return FileSystem.SYSTEM.read(path) {
        readUtf8()
    }
}

fun readAllLines(path: Path): List<String> {
    return readAll(path).lines()
}

fun env(): Map<String, String> {
    val env = mutableListOf<String>()
    for (i in 0..Int.MAX_VALUE) {
        val value = environ?.get(i)?.toKString() ?: break
        env.add(value)
    }
    return env.map { it.split("=", limit = 2) }.associate { it[0] to it[1] }
}

fun readMap(name: String, path: Path?, replacer: Replacer, init: Map<String, String> = mapOf()): Map<String, String> {
    info("${name} file: ${path}")
    if (path != null && FileSystem.SYSTEM.exists(path)) {
        return init.plus(
            readAllLines(path)
                .filter { it.isNotBlank() && !it.trim().startsWith("#") }
                .map { it.split('=', limit = 2) }
                .associate { it.first() to replacer.replaceVars(it.last()) }
        ).onEach { debug("[${name}] ${it.key} => ${it.value}") }
    } else {
        debug("${name} file not found. Resuming anyway.")
    }
    return mapOf()
}
