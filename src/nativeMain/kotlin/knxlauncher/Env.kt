package knxlauncher

import kotlinx.cinterop.get
import kotlinx.cinterop.toKString
import platform.posix.environ

actual val env: Map<String, String> = run {
    val env = mutableListOf<String>()
    for (i in 0..Int.MAX_VALUE) {
        val value = environ?.get(i)?.toKString() ?: break
        env.add(value)
    }
    return@run env.map { it.split("=", limit = 2) }.associate { it[0] to it[1] }
}

