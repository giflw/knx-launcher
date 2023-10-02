package knxlauncher

import okio.Path

class Replacer {

    private val vars: MutableMap<String, String?> = mutableMapOf()
    private val regex = Regex("[\\$#]\\{(?<varname>[a-zA-Z0-9:._~-]+)\\}")

    init {
        vars.putAll(Properties.get())
    }

    fun vars(otherVars: Map<String, String>): Replacer {
        vars.putAll(otherVars)
        return this
    }

    fun config(config: Config): Replacer {
        config.all().forEach {
            vars["cfg:${it.name}"] = it.value.toString()
        }
        return this
    }

    fun env(env: Map<String, String>): Replacer {
        env.forEach {
            vars["env:${it.key}"] = it.value
        }
        return this
    }

    private fun getVarValue(varname: String?): String? {
        if (varname == null) {
            return null
        }
        return if (vars.containsKey(varname)) vars[varname] else ""
    }

    fun replaceVars(str: String): String {
        var newStr = str
        debug("[replacer:original] ${newStr}")
        val iterator = regex.findAll(str).iterator()

        while (iterator.hasNext()) {
            val varname = iterator.next().groups.get("varname")?.value
            var varvalue = getVarValue(varname)
            if (varname != null) {
                varvalue = varvalue ?: ""
                debug("[replacer:match] ${varname} -> ${varvalue}")
                newStr = newStr // on next line
                    .replace("\${${varname}}", varvalue)
                    .replace("#{${varname}}", varvalue)
                debug("[replacer:replaced] ${newStr}")
            }
        }
        return newStr
    }

}
