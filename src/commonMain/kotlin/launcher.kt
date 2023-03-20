import okio.Path
import okio.Path.Companion.toPath
import kotlinx.cli.*

fun main(args: Array<String>) {
    val path: Path = utils.binaryPath().toPath()
    val parser = ArgParser(path.name)
    val debug by parser.option(ArgType.Boolean, shortName = "d", description = "Turn on debug mode").default(false)
    parser.parse(args);
    println("hi ${path.name}: debug? ${debug}")
}
