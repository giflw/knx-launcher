package utils

import okio.FileSystem
import okio.Path

expect val DS : String
expect fun binaryPath(): Path

fun readAll(path: Path) : String {
    return FileSystem.SYSTEM.read(path) {
        readUtf8()
    }
}

fun readAllLines(path: Path): List<String> {
    return readAll(path).lines()
}
