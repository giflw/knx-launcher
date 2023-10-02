package knxlauncher

var binaryDebug: Boolean = Platform.isDebugBinary

private fun log(level: String, msg: String, ex: Throwable? = null) {
    println ("[${level}] ${msg}")
    if (ex != null) {
        println(ex)
    }
}

fun error(msg: String) {
    log("ERROR", msg)
}

fun info(msg:String) {
    log("INFO", msg)
}

fun warn(msg:String) {
   log("WARN", msg)
}

fun debug(msg:String) {
    if (binaryDebug) {
        log("DEBUG", msg)
    }
}
