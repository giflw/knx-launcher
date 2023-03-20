import okio.Path
import okio.Path.Companion.toPath

fun main(args: Array<String>) {
    val path: Path = utils.binaryPath().toPath()
    println("hi ${path.name}: ${args}")
}
