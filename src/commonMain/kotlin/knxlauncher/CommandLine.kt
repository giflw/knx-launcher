package knxlauncher

import okio.Path

class CommandLine(val path: Path, val exePath: Path, val replacer: Replacer) {

    fun get() : List<String> {
        val descriptorPath: Path? = exePath.parent?.resolve(path)
        info("Descriptor path: ${descriptorPath}")
        if (descriptorPath == null) {
            throw Exception("${path} not found!")
        }
        val lines = readAllLines(descriptorPath)
            .filter { it.isNotBlank() }
            .map { replacer.replaceVars(it) }

        if (debug) {
            lines.forEach {
                debug("[APP] ${it}")
            }
        }
        return lines.filter { it.isNotBlank() }
    }
}

