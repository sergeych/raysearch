package net.sergeych.raysearch

import java.nio.file.Path
import kotlin.io.path.extension

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
    open fun textExtractor(path: Path): DocDef? =
        if (path.extension.lowercase() in noScanFileExtensions) null
        else extensionTypes[path.extension] ?: DocDef.TextDocument

    companion object {
        val noScanFileExtensions = setOf(
            "jpg", "png", "gif", "mov", "wav", "mpg", "mpeg",
            "wasm", "bin", "exe", "kexe", "lib", "o", "so", "dll", "class", "pyc",
            "zip", "bz2", "gz", "7z", "jar",
            // to implement soon:
            "odf", "odt", "ods", "pdf",
            "docx", "xlsx", "doc", "xls"
        )

        val extensionTypes = mutableMapOf<String, DocDef>()

        init {
            fun a(dd: DocDef, vararg exts: String) {
                for (x in exts) extensionTypes[x] = dd
            }
            a(
                DocDef.ProgramSource,
                "c", "cpp", "c++", "cxx", "h", "h++", "hpp", "objc", "rs",
                "java", "scala", "kt", "gradle",
                "sql",
                "rb", "py", "pl", "js", "ts", "sh",
            )
            a( DocDef.TextDocument, "txt", "md")
        }
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
    override fun textExtractor(path: Path): DocDef? =
        if (path.fileName.toString() in files) null else super.textExtractor(path)
}

object NoSearchRule : SearchRule()

object GradleProjectRule : SearchNamesRule(
    setOf("build", "gradle"),
    setOf("gradle.properties", "settings.gradle.kts", "gradlew", "gradlew.bat")
)

object NpmProjectRule : SearchRule() {
    override fun shouldSkipDir(parent: String, dir: String): Boolean {
        return dir == "node_modules"
    }
}