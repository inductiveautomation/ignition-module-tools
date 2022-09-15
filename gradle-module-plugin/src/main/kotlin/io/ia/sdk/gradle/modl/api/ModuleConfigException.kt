package io.ia.sdk.gradle.modl.api

open class ModuleConfigException(message: String) :
    Exception("There was an error in applying the ignitionModule settings in the build.gradle, $message")
