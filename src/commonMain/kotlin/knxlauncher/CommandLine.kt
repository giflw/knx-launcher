package knxlauncher

import okio.Path

class CommandLine(val path: Path, val exePath: Path, val replacer: Replacer) {

    private val splitRegex = Regex("\\s(?=(([^\"]*\"){2})*[^\"]*$)\\s*")

    fun get(): List<String> {
        val descriptorPath: Path? = exePath.parent?.resolve(path)
        info("Descriptor path: ${descriptorPath}")
        if (descriptorPath == null) {
            throw Exception("${path} not found!")
        }
        return readAllLines(descriptorPath)
            .filter { it.isNotBlank() }
            .onEach { debug("[CMD:O] ${it}") }
            .map { replacer.replaceVars(it) }
            .flatMap { it.split(splitRegex) }
            .onEach { debug("[CMD:R] ${it}") }
    }
}

