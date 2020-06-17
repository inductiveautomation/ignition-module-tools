package io.ia.ignition.module.generator.error

import java.lang.IllegalArgumentException

/**
 * Thrown when an improper configuration object is provided to the generator
 */
class IllegalConfigurationException(message: String) : IllegalArgumentException(message)
