package knxlauncher

import okio.FileSystem
import okio.Path
import okio.SYSTEM

expect val DS: String
expect val binaryPath: Path

fun readAll(path: Path): String {
    debug("readAll ${path}")
    return FileSystem.SYSTEM.read(path) {
        readUtf8()
    }
}

fun readAllLines(path: Path): List<String> {
    debug("readAllLines ${path}")
    return readAll(path).lines()
}

fun readMap(name: String, path: Path?, replacer: Replacer, init: Map<String, String> = mapOf()): Map<String, String> {
    info("${name} file: ${path}")
    if (path != null && FileSystem.SYSTEM.exists(path)) {
        return init.plus(
            readAllLines(path)
                .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
                .map { it.split('=', limit = 2) }
                .associate { it.first() to replacer.replaceVars(it.last()) }
        ).onEach { debug("[${name}] ${it.key} => ${it.value}") }
    } else {
        debug("${name} file not found. Resuming anyway.")
    }
    return mapOf()
}
