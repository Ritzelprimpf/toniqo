package de.ritzelprimpf.toniqo.ui.info

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Loads the shipped `assets/THIRD_PARTY_LICENSES.txt` and asserts it is present and names the
 * Apache License, Version 2.0 — the license every currently-shipped third-party dependency
 * (AndroidX, Kotlin, Kotlin Coroutines, Dagger/Hilt) uses.
 *
 * The file is available as a classpath resource in JVM unit tests because `build.gradle.kts`
 * adds `src/main/assets` to the `test` source set's `resources`.
 */
class ThirdPartyLicenseAssetTest {

    private val licenseText by lazy {
        val stream = javaClass.classLoader!!.getResourceAsStream("THIRD_PARTY_LICENSES.txt")
            ?: error(
                "Asset not found: THIRD_PARTY_LICENSES.txt — ensure src/main/assets is in the " +
                    "test resource path (build.gradle.kts sourceSets config)",
            )
        stream.bufferedReader().readText()
    }

    @Test
    fun `bundled third-party license asset is present and non-empty`() {
        assertTrue(licenseText.isNotBlank())
    }

    @Test
    fun `bundled third-party license asset names the Apache License, Version 2_0`() {
        assertTrue(licenseText.contains("Apache License"))
        assertTrue(licenseText.contains("Version 2.0"))
    }
}
