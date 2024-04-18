package io.ia.sdk.gradle.modl.util

fun unsignedModuleName(humanModuleName: String): String {
    return "${humanModuleName.replace(" ", "-")}.unsigned.modl"
}

fun signedModuleName(humanModuleName: String): String {
    return unsignedModuleName(humanModuleName).replace(".unsigned", "")
}

fun nameToDirName(moduleName: String): String {
    return moduleName.split(" ").joinToString("-") { it.lowercase() }
}

// For when you don't need full-blown XML parsing just to test. Smoosh all
// tags together in one long line by knocking out indentation and newlines.
fun collapseXmlToOneLine(xml: String): String =
    xml.replace(Regex("""^\s+"""), "").replace(Regex("""\R"""), "")
