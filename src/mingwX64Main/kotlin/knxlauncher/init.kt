package knxlauncher

import kotlinx.cinterop.staticCFunction
import platform.posix.sleep
import platform.windows.*

private fun handleConsoleCtrl(signal: UInt): Int {
    println("SetConsoleCtrlHandler ${signal}")
    sleep(10u)
    return FALSE
}

actual fun initPlatform(): Unit {
    SetConsoleOutputCP(65001u)
    SetConsoleCtrlHandler(staticCFunction(::handleConsoleCtrl), TRUE)
}
