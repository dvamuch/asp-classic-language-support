package dvamuch.aspclassiclanguagesupport2.lang

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Prevents a green formatter audit from silently using a different IDE SDK. */
class AspPlatformCompatibilityTest : BasePlatformTestCase() {
    fun testRunsOnSupportedPhpStormBuild() {
        val info = ApplicationInfo.getInstance()
        assertEquals("Tests must use the same PhpStorm build as the development target", "262.10315.130", info.build.asStringWithoutProductCode())
        assertEquals("PS", info.build.productCode)
        println("ASP test platform: ${info.fullVersion}, ${info.build.asString()}")
    }
}
