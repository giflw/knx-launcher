package knxlauncher

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class Replacer(path: Path, private val env: Map<String, String>) {

    private val vars: MutableMap<String, String?> = HashMap()
    private val regex = Regex("[\\$#]\\{(?<varname>[a-zA-Z0-9:.]+)\\}")

    init {
        vars["launcher.exe"] = path.name
        vars["launcher.name"] = path.name.substring(
            0,
            if (path.name.contains(".")) path.name.lastIndexOf(".") else path.name.length - 1
        )
        vars["launcher.dir"] = path.parent?.normalized().toString()
        vars["user.home"] = getFirstVarValue("USERPROFILE", "HOME")
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
            vars[varname]
        }
    }

    fun replaceVars(str: String): String {
        var newStr = str
        val iter = regex.findAll(str).iterator()
        debug(newStr)

        while (iter.hasNext()) {
            val varname = iter.next().groups.get("varname")?.value
            var varvalue = getVarValue(varname)
            if (varname != null) {
                varvalue = if (varvalue != null) varvalue else ""
                debug("${varname} -> ${varvalue}")
                newStr = str.replace("\${${varname}}", varvalue).replace("#{${varname}}", varvalue)
            }
        }
        return newStr
    }

}
