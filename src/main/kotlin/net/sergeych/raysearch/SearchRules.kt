package net.sergeych.raysearch

import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.debug
import net.sergeych.mp_logger.warning
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Indexing context.
 *
 * Allows us to analyze directory structure and determine the rules on skipping subfolders,
 * files and determining right text extraction algorithm. Primary is used for
 * root project folders to skip intermediate files aor provide custom extraction tools
 * for file formats.
 *
 * The base class ignores directories and files staring with '.' and executable files,
 * and provide default format detection for files. When extending, call the super methods
 * if no custom action is required.
 */
open class SearchRule : LogTag("SRUL"){
    open fun shouldSkipDir(path: Path): Boolean =
        path.name.let { it.startsWith('.') || it == "tmp" || it.endsWith('~') }
                || !path.isReadable() || path.isHidden()

    /**
     * Check that the file should be scanned and provide text extraction
     * for it.
     * @return text extractor or null if the file should be skipped
     */
    open fun docDef(file: Path): DocDef? {
        if( file.isSymbolicLink() || file.isHidden() || file.fileName.toString()[0] == '.')
            return null
        if( file.isDirectory())
            throw IllegalArgumentException("can't docDef() on directory: $file")

        if( file.name.endsWith('~'))
            return null

        val x= file.extension.lowercase()
        return try {
            when {
                x in noScanFileExtensions -> null
                x in programSources -> PlainTextExtractor.detectCharset(file)?.let { DocDef.ProgramSource(it) }
                x in plainTexts -> PlainTextExtractor.detectCharset(file)?.let { DocDef.TextDocument(it) }
                x == "odt" -> DocDef.OdfDef
                x == "ods" -> DocDef.OdsDef
                x == "pdf" -> DocDef.PdfDef
                else -> {
                    if( reVersionedSo in file.name) {
                        debug { "versioned so: $file" }
                        return null
                    }
                    else if( file.isExecutable() ) {
                        try {
                            file.inputStream().use {
                                val shebang = it.readNBytes(2)
                                if (shebang.size != 2 || shebang[0].toInt() != '#'.code
                                    || shebang[1].toInt() != '!'.code
                                )
                                // non-script executable, won't index
                                    return null
                            }
                        } catch (x: Exception) {
                            warning { "failed to read file header: $file" }
                            return null
                        }
                    }
                    PlainTextExtractor.detectCharset(file)?.let { DocDef.TextDocument(it) }
                }
            }
        }
        catch(x: Throwable) {
            warning { "failed to detect doc $file: $x" }
            null
        }
    }
    companion object {

        val reVersionedSo = Regex("""\.so\..*$""")

        val noScanFileExtensions = setOf(
            "jpg", "jpeg", "mp3", "mp4", "png", "gif", "tif", "tiff", "bmp",
            "mov", "wav", "mpg", "mpeg", "webp", "ico", "icns", "img",

            "apk", "wasm", "bin", "exe", "kexe", "lib", "o", "so", "dll", "class", "pyc","pak",
            "lib", "a", "obj",
            "dat", "sym", "dump", "hprof", "iso", "deb", "rom", "bc", "rsa", "pem",

            "eot", "ttf", "woff",
            "zsync", "ztext",
            "unikey", "unicontract",

            "zip", "bz2", "gz", "7z", "jar",
            "db",

            "tmp",
            // to implement one day:
            "docx", "xlsx", "doc", "xls"
        )

        val programSources = setOf(
            "c", "cpp", "c++", "cxx", "h", "h++", "hpp", "objc", "rs",
            "java", "scala", "kt", "gradle",
            "sql",
            "rb", "py", "pl", "js", "ts", "sh",
        )
        val plainTexts = setOf(
            "txt", "md", "me"
        )
    }
}

/**
 * Simple rule
 */
open class SearchNamesRule(
    val dirs: Set<String> = setOf(),
    val files: Set<String> = setOf()
) : SearchRule() {
    override fun shouldSkipDir(path: Path): Boolean = (path.name in dirs) || super.shouldSkipDir(path)
    override fun docDef(file: Path): DocDef? =
        if (file.fileName.toString() in files) null else super.docDef(file)
}

object DefaultSearchRule : SearchRule()

object GradleProjectRule : SearchNamesRule(
    setOf("build", "gradle", "node_modules"),
    setOf("gradle.properties", "settings.gradle.kts", "gradlew", "gradlew.bat")
)

object NpmProjectRule : SearchRule() {
    override fun shouldSkipDir(path: Path): Boolean {
        return path.name == "node_modules"
    }
}