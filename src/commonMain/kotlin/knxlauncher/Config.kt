package knxlauncher

import platform.posix.*

class Config(private val config: Map<String, String>) {

    private fun booleanConfig(name: String, default: Boolean): Boolean {
        val cfg: Any = config.getOrElse(name) { default }
        return if (cfg == "true") true else (if (cfg == "false") false else default)
    }

    private fun stringConfig(name: String, default: String): String {
        return config.getOrElse(name) { default }
    }

    val mode: Int = when (config["mode"]) {
        "overlay" -> P_OVERLAY
        "wait" -> P_WAIT
        "nowait" -> P_NOWAIT
        "nowaito" -> P_NOWAITO
        "detach" -> P_DETACH
        else -> P_NOWAIT
    }

    val debug: Boolean = booleanConfig("debug", false)
    val preserveEnv: Boolean = booleanConfig("preserveEnv", true)
    val knxExtraArgsName: String = stringConfig("knxExtraArgsName", "_KNX_EXTRA_ARGS")

    override fun toString(): String = config.toString()
}
