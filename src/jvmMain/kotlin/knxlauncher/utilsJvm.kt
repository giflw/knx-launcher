package knxlauncher

import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import kotlin.io.path.toPath

actual val DS: String = File.separator
actual val binaryPath: Path = Config::class.java.protectionDomain.codeSource.location.toURI().toPath().toOkioPath()
