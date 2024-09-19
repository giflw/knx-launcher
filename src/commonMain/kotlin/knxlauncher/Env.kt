package knxlauncher

expect val env: Map<String, String>

object Env {

    fun get(key: String): String {
        return env[key] ?: ""
    }

    fun get(): Map<String, String> {
        return env
    }

}
