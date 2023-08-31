package net.sergeych.raysearch

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.isSymbolicLink

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
open class SearchRule {
    open fun shouldSkipDir(parent: String, dir: String): Boolean =
        dir.startsWith('.')

    /**
     * Check that the file should be scanned and provide text extraction
     * for it.
     * @return text extractor or null if the file should be skipped
     */
    open fun docDef(path: Path): DocDef? {
        if( path.isSymbolicLink() || path.isHidden() )
            return null
        if( path.isDirectory())
            throw IllegalArgumentException("can't docFef() on directory: $path")

        val x= path.extension.lowercase()
        return when {
            x in noScanFileExtensions -> null
            x in programSources -> PlainTextExtractor.detectCharset(path)?.let { DocDef.ProgramSource(it) }
            x in plainTexts -> PlainTextExtractor.detectCharset(path)?.let { DocDef.TextDocument(it) }
            else -> PlainTextExtractor.detectCharset(path)?.let { DocDef.TextDocument(it) }
        }
    }
    companion object {
        val noScanFileExtensions = setOf(
            "jpg", "jpeg,", "mp3", "png", "gif", "tif", "tiff", "bmp",
            "mov", "wav", "mpg", "mpeg", "webp", "ico", "icns",

            "apk", "wasm", "bin", "exe", "kexe", "lib", "o", "so", "dll", "class", "pyc",

            "dat", "sym", "dump", "hprof",
            "eot", "ttf", "woff",
            "zsync",
            "unikey", "unicontract",
            "zip", "bz2", "gz", "7z", "jar",
            // to implement soon:
            "odf", "odt", "ods", "pdf",
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
    override fun shouldSkipDir(parent: String, dir: String): Boolean = dir in dirs
    override fun docDef(path: Path): DocDef? =
        if (path.fileName.toString() in files) null else super.docDef(path)
}

object DefaultSearchRule : SearchRule()

object GradleProjectRule : SearchNamesRule(
    setOf("build", "gradle"),
    setOf("gradle.properties", "settings.gradle.kts", "gradlew", "gradlew.bat")
)

object NpmProjectRule : SearchRule() {
    override fun shouldSkipDir(parent: String, dir: String): Boolean {
        return dir == "node_modules"
    }
}