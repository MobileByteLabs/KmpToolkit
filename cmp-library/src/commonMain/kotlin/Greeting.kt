package io.github.template

/**
 * A simple greeting class demonstrating Kotlin Multiplatform.
 *
 * This is a sample class that you should replace with your actual library implementation.
 * It demonstrates how to use expect/actual declarations for platform-specific code.
 */
class Greeting {

    /**
     * Returns a greeting message with platform information.
     *
     * @return A greeting string containing the platform name
     */
    fun greet(): String = "Hello from ${getPlatformName()}!"

    /**
     * Returns a personalized greeting.
     *
     * @param name The name to greet
     * @return A personalized greeting string
     */
    fun greet(name: String): String = "Hello, $name! Welcome from ${getPlatformName()}."
}

/**
 * Returns the name of the current platform.
 *
 * This is an expect declaration - each platform provides its own implementation.
 */
expect fun getPlatformName(): String
