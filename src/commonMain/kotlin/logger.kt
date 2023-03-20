package logger

var debug: Boolean = Platform.isDebugBinary

fun log(msg: String) {
    if (debug) {
        debug(msg)
    }
}

fun info(msg: String) {
    println(msg)
}

fun debug(msg:String) {
    println(msg)
}
