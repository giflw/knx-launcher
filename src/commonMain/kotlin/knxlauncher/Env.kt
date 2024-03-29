package knxlauncher

import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.environ

object Env {

    private val env: Map<String, String> = memScoped {
        val env = mutableListOf<String>()
        for (i in 0..Int.MAX_VALUE) {
            val value = environ?.get(i)?.toKString() ?: break
            env.add(value)
        }
        return@memScoped env.map { it.split("=", limit = 2) }.associate { it[0] to it[1] }
    }

    fun get(key: String): String {
        return env[key] ?: ""
    }

    fun get(): Map<String, String> {
        return this.env
    }

}
