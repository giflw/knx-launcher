package knxlauncher

class Config(forceDebug: Boolean, private val config: Map<String, String>) {

    companion object {
        const val RESTART_ALWAYS: String = "always";
        const val RESTART_ON_CODE: String = "onCode";
        const val RESTART_ON_ERROR: String = "onError";
        const val RESTART_NEVER: String = "never";
    }

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

    private fun intConfig(name: String, default: Int): Int {
        val cfg: Any = config.getOrElse(name) { default }
        return when (cfg) {
            is Int -> cfg
            is String -> cfg.toInt()
            else -> cfg.toString().toInt()
        }
    }

    val argsIfNoArgs = ConfigEntry("argsIfNoArgs") { stringConfig(it, "").split(Regex("(?<=[^\\\\]) ")) }
    val cwd = ConfigEntry("cwd") { stringConfig(it, Properties.get(Properties.LAUNCHER_DIR)) }
    val debug = ConfigEntry("debug") { forceDebug || booleanConfig(it, false) }
    val knxExtraArgsName = ConfigEntry("knxExtraArgsName") { stringConfig(it, "_KNX_EXTRA_ARGS") }
    val preserveEnv = ConfigEntry("preserveEnv") { booleanConfig(it, true) }
    /** only when wait is true */
    val restartMode = ConfigEntry("restartMode") { stringConfig(it, RESTART_ON_CODE) }
    val restartCode = ConfigEntry("restartCode") { intConfig(it, EXIT_RESTART) }

    // FIXME should implement, but must have a way to change args (by file?)
    //val relaunchCode = ConfigEntry("restartCode") { intConfig(it, EXIT_RELAUNCH) }
    val wait = ConfigEntry("wait") { booleanConfig(it, true) }

    fun shouldRestart(exitCode: Int): Boolean {
        return when (restartMode.value) {
            RESTART_ALWAYS -> true
            RESTART_NEVER -> false
            RESTART_ON_ERROR -> exitCode > 0
            RESTART_ON_CODE -> exitCode == restartCode.value
            else -> false
        }
    }

    fun all() = listOf(
        argsIfNoArgs, cwd, debug, knxExtraArgsName, preserveEnv, wait
    )

    override fun toString(): String = config.toString()

}
