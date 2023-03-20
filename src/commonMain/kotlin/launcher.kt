import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

val VARS: MutableMap<String, String?> = HashMap()

fun populateVars(path: Path) {
    VARS["launcher.exe"] = path.name
    VARS["app.name"] = path.name
    VARS["launcher.dir"] = path.parent.toString()
}
fun replaceVars(str: String): String {
    var newStr = str
    val regex = Regex("[\\$#]\\{(?<varname>[a-zA-Z0-9.]+)\\}")
    val iter = regex.findAll(str).iterator()
    println("iter $iter")
    while(iter.hasNext()) {
        val varname = iter.next().groups.get("varname")?.value
        val varvalue: String? = VARS[varname]
        println("$varname -> $varvalue")
        if (varname != null && varvalue != null) {
            newStr = str.replace("\${${varname}}", varvalue).replace("#{${varname}}", varvalue)
        }
    }
    return newStr
}

fun main(args: Array<String>) {
    val path: Path = utils.binaryPath().toPath()
    populateVars(path)
    val appName: String = path.name

    val parser = ArgParser(appName)
    val debug by parser.option(ArgType.Boolean, shortName = "d", description = "Turn on debug mode").default(false)
    val descriptor by parser.option(ArgType.String, shortName = "D", description = "Application description file")
        .default("${appName}.descriptor")
    parser.parse(args)

    val level: String = if (debug) "DEBUG" else "INFO"

    val descriptorPath: Path? = path.parent?.resolve(descriptor)
    if (debug) {
        println("Descriptor path: ${descriptorPath}")
    }
    if(descriptorPath == null) {
        throw Exception("${descriptor} not found!")
    }
    val lines = utils.readAllLines(descriptorPath)
    if (debug) {
        println("---")
        lines.forEach {
            println(replaceVars(it))
        }
        println("---")
    }
    println("${appName} [${level}] ${descriptor}")
}
