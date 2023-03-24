package knxlauncher

import platform.windows.SetConsoleOutputCP

actual fun initPlatform(): Unit {
    SetConsoleOutputCP(65001)
}
