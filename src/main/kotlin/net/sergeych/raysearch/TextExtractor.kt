package net.sergeych.raysearch

import java.nio.file.Path
import kotlin.io.path.readText

interface TextExtractor {
    val name: String
    fun extractTextFrom(file: Path): String
}

/**
 * USe the object to not allocate many instances of the text extractor
 */
object PlainTextExtractor: TextExtractor {
    override val name = "plain text"
    override fun extractTextFrom(file: Path): String = file.readText()
}