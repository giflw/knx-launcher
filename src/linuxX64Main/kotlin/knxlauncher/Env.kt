package knxlauncher

import kotlinx.cinterop.*
import platform.posix.__environ

actual fun nativeenv(): CPointer<CPointerVar<ByteVar>>? = __environ
