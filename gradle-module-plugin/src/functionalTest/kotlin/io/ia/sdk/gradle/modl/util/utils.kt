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
