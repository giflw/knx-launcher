package knxlauncher

object Properties {

    const val LAUNCHER_EXE = "launcher.exe"
    const val LAUNCHER_NAME = "launcher.name"
    const val LAUNCHER_DIR = "launcher.dir"
    const val USER_HOME = "user.home"
    const val USER_HOME_ALIAS = "~"
    const val USER_NAME = "user.name"

    private val properties:Map<String, String> =run {
        val vars = mutableMapOf<String, String>()
        val path = binaryPath
        vars[LAUNCHER_EXE] = path.name
        vars[LAUNCHER_NAME] = path.name.substring(
            0,
            if (path.name.contains(".")) path.name.lastIndexOf(".") else path.name.length - 1
        )
        vars[LAUNCHER_DIR] = path.parent?.normalized().toString()
        vars[USER_HOME] = getFirstEnvValue("HOME", "USERPROFILE")
        vars[USER_HOME_ALIAS] = vars[USER_HOME]!!
        vars[USER_NAME] = getFirstEnvValue("USER", "USERNAME")
        return@run vars
    }

    private fun getFirstEnvValue(vararg varnames: String): String {
        for (varname in varnames) {
            val value = Env.get(varname)
            if (value.isNotEmpty()) {
                return value
            }
        }
        return ""
    }

    fun get(key: String):String {
        return this.properties[key]!!
    }

    fun get(): Map<String, String> {
        return this.properties
    }

}
