package knxlauncher

import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.toKString
import platform.windows.GetModuleFileNameW
import platform.windows.GetModuleHandleW
import platform.windows.MAX_PATH
import okio.Path
import okio.Path.Companion.toPath

actual val DS: String = "\\"

actual fun binaryPath(): Path {
    // Get the path to the EXE.
    val hmodule = GetModuleHandleW(null)
    val wstr: WSTR = nativeHeap.allocArray<UShortVar>(MAX_PATH)
    GetModuleFileNameW(hmodule, wstr, MAX_PATH)
    // Strip the filename leaving just the directory.
    //PathRemoveFileSpecW(wstr)
    return wstr.toKString().toPath()
}
