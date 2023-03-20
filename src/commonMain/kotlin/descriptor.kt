import kotlinx.cinterop.toKString
import okio.Path
import platform.posix.getenv

class Descriptor(val path: Path, val exePath: Path) {

    val VARS: MutableMap<String, String?> = HashMap()
    val regex = Regex("[\\$#]\\{(?<varname>[a-zA-Z0-9:.]+)\\}")

    init {
        VARS["launcher.exe"] = path.name
        VARS["launcher.name"] = path.name.substring(0, path.name.lastIndexOf("."))
        VARS["launcher.dir"] = path.parent?.normalized().toString()
    }

    fun getVarValue(varname: String?) : String? {
        if (varname == null) {
            return null
        }
        return if (varname.startsWith("env:")) {
            getenv(varname.substring(4))?.toKString()
        } else {
            VARS[varname]
        }
    }

    fun replaceVars(str: String): String {
        var newStr = str
        val iter = regex.findAll(str).iterator()
        logger.log(newStr)

        while (iter.hasNext()) {
            val varname = iter.next().groups.get("varname")?.value
            var varvalue = getVarValue(varname)
            if (varname != null) {
                varvalue = if (varvalue != null) varvalue else ""
                logger.log("${varname} -> ${varvalue}")
                newStr = str.replace("\${${varname}}", varvalue).replace("#{${varname}}", varvalue)
            }
        }
        return newStr
    }

    fun get() : List<String> {
        val descriptorPath: Path? = exePath.parent?.resolve(path)
        logger.info("Descriptor path: ${descriptorPath}")
        if (descriptorPath == null) {
            throw Exception("${path} not found!")
        }
        val lines = utils.readAllLines(descriptorPath)
            .filter { it.isNotBlank() }
            .map { replaceVars(it) }

        if (logger.debug) {
            lines.forEach {
                logger.debug("[APP] ${it}")
            }
        }
        return lines.filter { it.isNotBlank() }
    }
}

