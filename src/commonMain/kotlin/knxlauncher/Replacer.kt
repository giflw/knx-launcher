package knxlauncher

import okio.Path

class Replacer(path: Path, private val env: Map<String, String>) {

    private val vars: MutableMap<String, String?> = HashMap()
    private val regex = Regex("[\\$#]\\{(?<varname>[a-zA-Z0-9:._~-]+)\\}")

    init {
        vars["launcher.exe"] = path.name
        vars["launcher.name"] = path.name.substring(
            0,
            if (path.name.contains(".")) path.name.lastIndexOf(".") else path.name.length - 1
        )
        vars["launcher.dir"] = path.parent?.normalized().toString()
        vars["user.home"] = getFirstVarValue("USERPROFILE", "HOME")
        vars["~"] = vars["user.home"]
    }

    private fun getFirstVarValue(vararg varnames: String?): String? {
        for(varname in varnames) {
            val value = getVarValue(varname)
            if (value != null) {
                return value
            }
        }
        return null
    }

    private fun getVarValue(varname: String?): String? {
        if (varname == null) {
            return null
        }
        return if (varname.startsWith("env:")) {
            val key = varname.substring(4)
            if (env.containsKey(key)) env[key] else null
        } else {
            return if (vars.containsKey(varname)) vars[varname] else env[varname]
        }
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
