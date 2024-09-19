package knxlauncher

import kotlinx.cinterop.*

expect fun nativeenv(): CPointer<CPointerVar<ByteVar>>?

actual val env: Map<String, String> = run {
    val env = mutableListOf<String>()
    for (i in 0..Int.MAX_VALUE) {
        val value = nativeenv()?.get(i)?.toKString() ?: break
        env.add(value)
    }
    return@run env.map { it.split("=", limit = 2) }.associate { it[0] to it[1] }
}

