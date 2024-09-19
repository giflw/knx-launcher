package knxlauncher

import kotlinx.cinterop.*
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.*

actual val DS: String = "/"

// https://github.com/graviton-browser/graviton-browser/blob/master/bootstrap/src/main/kotlin/linux.kt
actual val binaryPath: Path = memScoped {
    val length = PATH_MAX.toULong()
    val pathBuf = allocArray<ByteVar>(length.toInt())
    val myPid = getpid()
    val res = readlink("/proc/$myPid/exe", pathBuf, length)
    if (res < 1) {
        throw RuntimeException("/proc/$myPid/exe failed: $res")
    }
    return@memScoped pathBuf.toKString().toPath()
}
