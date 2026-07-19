package de.ritzelprimpf.toniqo.ui.info

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Loads the shipped `assets/LICENSE.txt` and asserts it is present and matches the project's
 * MIT license, catching drift if the root `LICENSE` file is updated without re-copying it here.
 *
 * The file is available as a classpath resource in JVM unit tests because `build.gradle.kts`
 * adds `src/main/assets` to the `test` source set's `resources`.
 */
class LicenseAssetTest {

    private val licenseText by lazy {
        val stream = javaClass.classLoader!!.getResourceAsStream("LICENSE.txt")
            ?: error("Asset not found: LICENSE.txt — ensure src/main/assets is in the test resource path (build.gradle.kts sourceSets config)")
        stream.bufferedReader().readText()
    }

    @Test
    fun `bundled license asset is present and non-empty`() {
        assertTrue(licenseText.isNotBlank())
    }

    @Test
    fun `bundled license asset is the MIT license`() {
        assertTrue(licenseText.startsWith("MIT License"))
        assertTrue(licenseText.contains("Ritzelprimpf"))
    }
}
