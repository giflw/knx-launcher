package knxlauncher

import kotlinx.cinterop.*
import platform.posix.environ

actual fun nativeenv(): CPointer<CPointerVar<ByteVar>>? = environ
