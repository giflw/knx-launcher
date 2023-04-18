package knxlauncher

class Config(forceDebug: Boolean, private val config: Map<String, String>) {

    data class ConfigEntry<T>(val name: String, val getter: (String) -> T) {
        val value = getter(name)
    }

    private fun booleanConfig(name: String, default: Boolean): Boolean {
        val cfg: Any = config.getOrElse(name) { default }
        return if (cfg == "true") true else (if (cfg == "false") false else default)
    }

    private fun stringConfig(name: String, default: String): String {
        return config.getOrElse(name) { default }
    }

    val argsIfNoArgs = ConfigEntry("argsIfNoArgs") { stringConfig(it, "").split(Regex("(?<=[^\\\\]) ")) }
    val cwd = ConfigEntry("cwd") { stringConfig(it, Properties.get(Properties.LAUNCHER_DIR)) }
    val debug = ConfigEntry("debug") { forceDebug || booleanConfig(it, false) }
    val knxExtraArgsName = ConfigEntry("knxExtraArgsName") { stringConfig(it, "_KNX_EXTRA_ARGS") }
    val preserveEnv = ConfigEntry("preserveEnv") { booleanConfig(it, true) }
    val wait = ConfigEntry("wait") { booleanConfig(it, true) }

    val ALL = listOf(
        argsIfNoArgs, cwd, debug, knxExtraArgsName, preserveEnv, wait
    )

    override fun toString(): String = config.toString()

}
