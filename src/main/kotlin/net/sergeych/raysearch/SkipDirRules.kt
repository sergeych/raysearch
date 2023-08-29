package net.sergeych.raysearch

interface SkipDirRule {
    fun shouldSkip(parent: String,dir: String): Boolean
}

object NoSkipRule : SkipDirRule {
    override fun shouldSkip(parent: String, dir: String) = false
}

object GradleProjectRule: SkipDirRule {
    override fun shouldSkip(parent: String, dir: String): Boolean {
        return dir == "build" || dir == "gradle"
    }
}

object NpmProjectRule: SkipDirRule {
    override fun shouldSkip(parent: String, dir: String): Boolean {
        return dir == "node_modules"
    }
}