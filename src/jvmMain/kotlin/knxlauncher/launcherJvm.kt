package knxlauncher

import kotlin.system.exitProcess

actual fun exit(exitCode: Int) : Unit {
    exitProcess(exitCode)
}
